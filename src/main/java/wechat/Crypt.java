package wechat;


import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

/**
 * WeChat crypt utils.
 * Created by kurtg on 17/6/11.
 */
public class Crypt {
    private static final Charset CHARSET = Charset.forName("utf-8");
    private final byte[] aesKey;
    private final String token;
    private final String appId;

    class ByteCollector {
        ArrayList<Byte> bytes;

        public ByteCollector() {
            this.bytes = new ArrayList<>();
        }

        public int size() {
            return bytes.size();
        }

        public void addBytes(byte[] newBytes) {
            for (Byte b : newBytes) {
                bytes.add(b);
            }
        }

        public byte[] toBytes() {
            byte[] res = new byte[bytes.size()];
            int i = 0;
            for (Byte b : bytes) {
                res[i++] = b;
            }
            return res;
        }

        @Override
        public String toString() {
            return new String(toBytes(), CHARSET);
        }
    }

    static class PKCS7 {
        static int BLOCK_SIZE = 32;

        static byte[] encode(int len) {
            int padSize = BLOCK_SIZE - (len % BLOCK_SIZE);
            if (padSize == 0) padSize = BLOCK_SIZE;
            char pad = (char) (padSize & 0xFF);
            String str = "";
            for (int i = 0; i < padSize; i++) {
                str += pad;
            }
            return str.getBytes(CHARSET);
        }

        static byte[] decode(byte[] bytes) {
            int padSize = bytes[bytes.length - 1];
            if (padSize < 1 || padSize > 32) {
                padSize = 0;
            }
            return Arrays.copyOfRange(bytes, 0, bytes.length - padSize);
        }
    }

    public Crypt(String token, String aesKey, String appId) throws Exception{
        this.token = token;
        this.appId = appId;
        if (aesKey.length() != 43) {
            throw new InvalidKeyException("illegal Key Size");
        }
        this.aesKey = Base64.decodeBase64(aesKey + "=");
    }

    public String encrypt(String randomStr, String text) {
        byte[] randomStrBytes = randomStr.getBytes(CHARSET);
        byte[] textBytes = text.getBytes(CHARSET);
        byte[] bytesOrder = getBytesOrder(textBytes.length);
        byte[] appIdBytes = appId.getBytes(CHARSET);

        ByteCollector byteCollector = new ByteCollector();
        byteCollector.addBytes(randomStrBytes);
        byteCollector.addBytes(bytesOrder);
        byteCollector.addBytes(textBytes);
        byteCollector.addBytes(appIdBytes);
        byteCollector.addBytes(PKCS7.encode(byteCollector.size()));

        byte[] all = byteCollector.toBytes();

        String encrypted = null;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            IvParameterSpec ivParam = new IvParameterSpec(aesKey, 0, 16);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParam);

            byte[] encryptedBytes = cipher.doFinal(all);
            encrypted = Base64.encodeBase64String(encryptedBytes);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return encrypted;
    }

    public String decrypt(String text) {
        byte[] encrypted = Base64.decodeBase64(text);
        byte[] original = null;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            IvParameterSpec ivParam = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParam);
            original = cipher.doFinal(encrypted);
        }catch (Exception e) {
            e.printStackTrace();
        }

        byte[] bytes = PKCS7.decode(original);
        byte[] bytesOrder = Arrays.copyOfRange(bytes, 16, 20);
        int len = recoverBytesOrder(bytesOrder);

        String content = new String(Arrays.copyOfRange(bytes, 20, 20 + len), CHARSET);
        String fromAppId= new String(Arrays.copyOfRange(bytes, 20 + len, bytes.length), CHARSET);

        if (!fromAppId.equals(appId))
            return "";
        return content;
    }

    public String createEncryptXml(String replyXml, String timestamp, String nonce) {
        String encrypted = encrypt(getRandomStr(16), replyXml);

        if(timestamp.equals(""))
            timestamp = Long.toString(System.currentTimeMillis());

        String signature = SHA1(timestamp, nonce, encrypted);

        String format = "<xml><Encrypt><![CDATA[%1$s]]></Encrypt>\n"
                + "<MsgSignature><![CDATA[%2$s]]></MsgSignature>\n"
                + "<TimeStamp>%3$s</TimeStamp><Nonce><![CDATA[%4$s]]></Nonce></xml>";
        return String.format(format, encrypted, signature, timestamp, nonce);
    }

    public String extractEncryptXml(String xml) {
        String res = "";
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            Document doc = db.parse(is);

            Element root = doc.getDocumentElement();
            res = root.getElementsByTagName("Encrypt").item(0).getTextContent().trim();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    private byte[] getBytesOrder(int len) {
        byte[] res = new byte[4];
        res[3] = (byte) (len & 0xFF);
        res[2] = (byte) (len >> 8 & 0xFF);
        res[1] = (byte) (len >> 16 & 0xFF);
        res[0] = (byte) (len >> 24 & 0xFF);
        return res;
    }

    private int recoverBytesOrder(byte[] bytesOrder) {
        int len = 0;
        for (int i = 0; i < 4; i++) {
            len <<= 8;
            len |= bytesOrder[i] & 0xFF;
        }
        return len;
    }

    public String SHA1(String timeStamp,String nonce, String encrypt) {
        String[] strs = new String[] {token, timeStamp, nonce, encrypt};
        Arrays.sort(strs);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 4; i++) {
            sb.append(strs[i]);
        }
        return DigestUtils.sha1Hex(sb.toString());
    }

    private String getRandomStr(int len) {
        String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuffer sb = new StringBuffer();
        Random rd = new Random();
        for (int i = 0; i < len; i++) {
            sb.append(base.charAt(rd.nextInt(base.length())));
        }
        return sb.toString();
    }
}
