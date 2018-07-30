package wechat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mcs.FilePath;

/**
 * Utils.
 * Created by kurtg on 17/1/24.
 */
public class Util {
    static Charset CHARSET= Charset.forName("utf-8");
    public static final String Token = "PolyU";
    public static final String AppId = "wxdcf080ddac0843e3";
    public static final String AppSecret = "234d3c642361e31d9ee88b009093fbe0";
    public static final String EncodingAesKey = "WXAkeitfFTBf5NNblmaFCdNd1PYP6XVOQZ1Jttpy2Cp";

    static String getToken() {
        String json = getHTML("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + AppId + "&secret=" + AppSecret);
        Pattern pattern = Pattern.compile("\"access_token\":\"([\\S]+?)\"");
        Matcher m = pattern.matcher(json);
        m.find();
        return m.group(1);
    }

    static String getHTML(String url) {
        System.out.print("url:" + url);
        String html = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");

            conn.connect();
            if (301 == conn.getResponseCode() || 302 == conn.getResponseCode()) {
                String redirectUrl = conn.getHeaderFields().get("Location").get(0);
                conn.disconnect();
                conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
                conn.setUseCaches(false);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setRequestProperty("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
            }
            if (200 == conn.getResponseCode()) {
                InputStream is = conn.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len = 0;
                while(-1 != (len = is.read(buffer))){
                    baos.write(buffer,0,len);
                    baos.flush();
                }
                html = baos.toString("utf-8");

                Pattern encodingPattern = Pattern.compile("<meta.*content=\"text/html; ?charset=(\\S+?)\"");
                Matcher em = encodingPattern.matcher(html);
                if(em.find()) {
                    String encoding = em.group(1);
                    if(!encoding.equals("UTF-8"))
                        html = baos.toString(encoding);
                }
            }
            conn.disconnect();
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("无法获取HTML");
        }
        return html;
    }

    static String getMobileHTML(String url) {
        String html = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-agent", "Mozilla/5.0 (Linux; U; Android 7.0; zh-CN; FRD-AL00 Build/HUAWEIFRD-AL00) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/40.0.2214.89 Quark/1.7.1.916 Mobile Safari/537.36");

            conn.connect();
            if (301 == conn.getResponseCode() || 302 == conn.getResponseCode()) {
                String redirectUrl = conn.getHeaderFields().get("Location").get(0);
                conn.disconnect();
                conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
                conn.setUseCaches(false);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setRequestProperty("User-agent", "Mozilla/5.0 (Linux; U; Android 7.0; zh-CN; FRD-AL00 Build/HUAWEIFRD-AL00) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/40.0.2214.89 Quark/1.7.1.916 Mobile Safari/537.36");
            }
            if (200 == conn.getResponseCode()) {
                InputStream is = conn.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len = 0;
                while(-1 != (len = is.read(buffer))){
                    baos.write(buffer,0,len);
                    baos.flush();
                }
                html = baos.toString("utf-8");

                Pattern encodingPattern = Pattern.compile("<meta.*content=\"text/html; ?charset=(\\S+?)\"");
                Matcher em = encodingPattern.matcher(html);
                if(em.find()) {
                    String encoding = em.group(1);
                    if(!encoding.equals("UTF-8"))
                        html = baos.toString(encoding);
                }
            }
            conn.disconnect();
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("无法获取HTML");
        }
        return html;
    }

    static String getImgID(String mediaUrl) {
//        System.out.println("Token： " + getToken());
        String url = "https://api.weixin.qq.com/cgi-bin/media/upload?access_token="+ getToken() +"&type=image";
        String boundary = "--------2nc7xslg";
        String mediaID = "";
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setUseCaches(false);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            OutputStream os = conn.getOutputStream();

            HttpURLConnection mediaConn = (HttpURLConnection) new URL(mediaUrl).openConnection();
            mediaConn.setDoOutput(true);
            mediaConn.setRequestMethod("GET");

            String contentType = mediaConn.getContentType();
            String fileExt = contentType.split("/")[1];
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write(("Content-Disposition: form-data; name=\"media\"; filename=\"file." + fileExt + "\"\r\n").getBytes());
            os.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes());

            BufferedInputStream bis = new BufferedInputStream(mediaConn.getInputStream());
            byte[] buffer = new byte[2048];
            int length = 0;
            while((length = bis.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
            bis.close();
            os.write(("\r\n--" + boundary + "--\r\n").getBytes());
            os.close();
            mediaConn.disconnect();

            if (200 == conn.getResponseCode()) {
                StringBuffer sb = new StringBuffer();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(),Util.CHARSET));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                String json = sb.toString();
                conn.disconnect();
                JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
                mediaID = jsonObject.get("media_id").getAsString();
            }
            conn.disconnect();
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("无法上传Image");
        }
        return mediaID;
    }

    static String unEscape(String str) {
        StringBuffer sb = new StringBuffer();
        String[] sp = str.split("\\\\u");
        sb.append(sp[0]);
        for (int i = 1; i < sp.length; i++) {
            sb.append((char) Integer.parseInt(sp[i].substring(0,4), 16));
            sb.append(sp[i].substring(4,sp[i].length()));
        }
        return sb.toString();
    }

    static String shrinkMsg(String reply) {
        try {
            byte[] bt = reply.getBytes(Util.CHARSET);
            if (bt.length >= 2048) {
                reply = new String(Arrays.copyOf(bt, 2040) ,Util.CHARSET);
                reply = reply.substring(0, reply.length() - 1) + "...";
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return reply;
    }

    public static String readTxt(String filePath) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(FilePath.get
                    (filePath))), "utf-8"));
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line);
            br.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
