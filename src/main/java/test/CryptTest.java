package test;


import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import wechat.Crypt;
import wechat.Util;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CryptTest
 * Created by kurtg on 17/6/11.
 */
public class CryptTest {
    String encodingAesKey = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
    String token = "pamtest";
    String timestamp = "1409304348";
    String nonce = "xxxxxx";
    String appId = "wxb11529c136998cb6";
    String replyMsg = "我是中文abcd123";
    String xmlFormat = "<xml><ToUserName><![CDATA[toUser]]></ToUserName><Encrypt><![CDATA[%1$s]]></Encrypt></xml>";
    String afterEncrypt = "jn1L23DB+6ELqJ+6bruv21Y6MD7KeIfP82D6gU39rmkgczbWwt5+3bnyg5K55bgVtVzd832WzZGMhkP72vVOfg==";
    String randomStr = "aaaabbbbccccdddd";

    String newXml = "<xml>\n" +
            "    <Encrypt>\n" +
            "        <![CDATA[9L/muLIVUqHo1aZp8+oczOH/OZxY+P42OxPb5Eve6ozCBpiw8l2m1q4EhAciPq/D4Wtl9k+ZjqeVH6OxVrzPmz4tlW7P/05JP2T07dGLVU3oFyFl7IayXUrZusyFOLF7oRXZ6c5gY4TeY7CcYRux5gpeCoxppW7uOBRcq/IDjkUYVuHl6oJrtgv4PHKQXUwPe/ZbZucLIcTdzwr+zi2B4N6dOlHU6F0KKLLsO6MWmL7UPid5L/gJalskydAzsL/2t4hSEaBLrrQwNlGmbgc2lUvhDP+JII6zp491oJ9jI6hBZ1cY4LUOAvq/UB0HKTVzQYLbc1C7X0hr/VIt0xyCMMJTe1h7VKhPAawXTzGdjBpkJ/t4Guuw28tNJRbZ3CP0]]>\n" +
            "    </Encrypt>\n" +
            "    <MsgSignature>\n" +
            "        <![CDATA[1525cedb7a8f6d3c01cdaf2e667bf032f2314de0]]>\n" +
            "    </MsgSignature>\n" +
            "    <TimeStamp>1497205524172</TimeStamp>\n" +
            "    <Nonce>\n" +
            "        <![CDATA[2090614803]]>\n" +
            "    </Nonce>\n" +
            "</xml>";

    @Test
    public void testEncrypt() {
        try {
            Crypt crypt = new Crypt(token, encodingAesKey, appId);

            String encrypted = crypt.encrypt(randomStr, replyMsg);
            assert encrypted.equals(afterEncrypt);

            String xml = crypt.createEncryptXml(replyMsg, timestamp, nonce);
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                InputSource is = new InputSource(new StringReader(xml));
                Document doc = db.parse(is);

                Element root = doc.getDocumentElement();
                String  decrypted= crypt.decrypt(root.getElementsByTagName("Encrypt").item(0).getTextContent());
                String  signature= root.getElementsByTagName("MsgSignature").item(0).getTextContent();
                assert decrypted.equals(replyMsg);
//                assert signature.equals(sig);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDecrypt() {
        try {
            Crypt crypt = new Crypt(token, encodingAesKey, appId);
            String valiXml = String.format(xmlFormat, crypt.encrypt(randomStr, replyMsg));
//            String xml = crypt.decryptXml(valiXml);
//            System.out.println(xml);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testSHA1() throws Exception{
        Crypt crypt = new Crypt(Util.Token, Util.EncodingAesKey, Util.AppId);
//        String testXml = crypt.createEncryptXml(replyMsg, "", nonce);
        String testXml = newXml;
        String encrypt = crypt.extractEncryptXml(testXml);
        System.out.println(testXml);
        System.out.println(crypt.decrypt(encrypt));

        Matcher m = Pattern.compile("<TimeStamp>(\\d+?)</TimeStamp>").matcher(testXml);
        m.find(); String newTS = m.group(1);
        m = Pattern.compile("<Nonce>[\\s]*<!\\[CDATA\\[(\\w+?)]]>[\\s]*</Nonce>").matcher(testXml);
        m.find(); String newNonce = m.group(1);
        m = Pattern.compile("<MsgSignature>[\\s]*<!\\[CDATA\\[(\\w+?)]]>[\\s]*</MsgSignature>").matcher(testXml);
        m.find(); String newSig = m.group(1);

        String genSig = crypt.SHA1(newTS, newNonce, encrypt);
        System.out.println(genSig);
        assert newSig.equals(genSig);
    }
}
