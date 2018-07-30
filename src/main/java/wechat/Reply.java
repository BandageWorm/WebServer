package wechat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import mcs.Engine;
import mcs.FilePath;

import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Reply {

    public enum ReplyType {reply, question, phone, image, weather, none, direct}

    public enum XmlType {txt, url, img}

    public String ask;
    public String ans;
    public ReplyType replyType;
    public XmlType xmlType;

    private Engine engine;
    private static JsonParser jsonParser = new JsonParser();

    private Reply() {}

    public Reply(String ask) {
        this.ask = ask;
        xmlType = XmlType.txt;
        boolean oChecked = false;
    }

    public void processReply() {
        System.out.println("processReply");
        //空字符串
        if (ask == null || ask.matches("\\s+")) {
            replyType = ReplyType.none;
        }
        //非查询问句
        else if (ask.matches("[你您].+")) {
            replyType = ReplyType.reply;
        }
        //天气
        else if (ask.contains("天气")) {
            try {
                String data = Util.readTxt("Text\\city.csv");
                System.out.println("---" + data);
                Matcher am = Pattern.compile("(\\S+?)(.天)?的?天气[\\S 　]*").matcher(ask);
                am.find();
                String cityName = am.group(1);
                if (data.contains(cityName)) {
                    System.out.println(cityName);
                    replyType = ReplyType.weather;
                    ask = cityName;
                } else {
                    replyType = ReplyType.reply;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //图片
        else if (ask.matches(".*(啥样|什么样|图片|表情|照片|美景|景色|摄影|自拍|风景).*")) {
            replyType = ReplyType.image;
            xmlType = XmlType.img;
        }
        //百度知道
        else if ((ask.matches(".+[吗啥嘛?？]") || ask.matches(".*(如何|怎么|怎样|咋样|什么|哪些|为啥|有啥|是否|多少).*")
                || ask.matches("(假设|假如|如果|推荐).*") || ask.matches(".*(怎么办|咋办)")) && ask.length() > 5) {
            replyType = ReplyType.question;
        }
        //电话号码
        else if (ask.matches("1\\d{10}")) {
            replyType = ReplyType.phone;
        } else {
            replyType = ReplyType.reply;
        }

        switch (replyType) {
            case reply:
                ans = getReply(ask);
                break;
            case question:
                ans = getZhidaoAns(ask);
                break;
            case phone:
                ans = getPhoneLoc(ask);
                break;
            case image:
                ans = getImage(ask);
                break;
            case weather:
                System.out.println("weatherReply");
                ans = getWeather(ask);
                break;
            case direct:
                break;
            case none:
                ans = getNone();
                break;
            default:
                ans = getReply(ask);
                break;
        }
    }

    public void testProcessReply() {
        replyType = ReplyType.reply;
        ans = getReply(ask);
    }

    private String getReply(String ask) {
        String reply = Engine.get().reply(ask);
        if (reply == null || reply.isEmpty()) {
            reply = "我无法理解或服务器繁忙(╥╯^╰╥)";
        }
        return reply;
    }

    String getWeather(String ask) {
        String res = "暂时无法查询到天气信息或城市信息有误╮(๑•́ ₃•̀๑)╭";
        try {
            String url = "http://v.juhe.cn/weather/index?format=2&cityname=" + URLEncoder.encode(ask, "utf-8")
                    + "&key=adf9483506f8af73fbc3eac624fcbb11";
            String json = Util.getHTML(url);
//            String json = McsUtil.readTxt(FilePath.get("Text\\weather.json"));
            JsonObject jo = jsonParser.parse(json).getAsJsonObject();
            if (jo.get("resultcode").getAsString().equals("200")) {
                res = "";
                JsonObject result = jo.get("result").getAsJsonObject();
                JsonObject sk = result.get("sk").getAsJsonObject();
                res += String.format("实时天气(%s)：", sk.get("time").getAsString());
                res += "温度" + sk.get("temp").getAsString() + "℃，";
                res += sk.get("wind_direction").getAsString();
                res += sk.get("wind_strength").getAsString() + "，";
                res += "湿度" + sk.get("humidity").getAsString() + "。\n";

                JsonObject today = result.get("today").getAsJsonObject();
                res += String.format("今日天气(%s)：", today.get("date_y").getAsString());
                res += today.get("temperature").getAsString() + "，";
                res += today.get("weather").getAsString() + "，";
                res += today.get("wind").getAsString() + "。\n";
                res += "评价：天气" + today.get("dressing_index").getAsString() + "，";
                res += "洗车" + today.get("wash_index").getAsString() + "，";
                res += "旅游" + today.get("travel_index").getAsString() + "，";
                res += "晨练" + today.get("exercise_index").getAsString() + "，";
                res += today.get("dressing_advice").getAsString() + "\n";

                res += "未来天气：\n";
                JsonArray future = result.getAsJsonArray("future");
                for (JsonElement e : future) {
                    JsonObject day = e.getAsJsonObject();
                    String date = day.get("date").getAsString();
                    res += String.format("%s月%s日", date.substring(4, 6), date.substring(6, 8));
                    res += day.get("week").getAsString() + "：";
                    res += day.get("temperature").getAsString() + "，";
                    res += day.get("weather").getAsString() + "。\n";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    String getZhidaoAns(String ask) {
        try {
            String html = Util.getHTML("https://zhidao.baidu.com/search?ct=17&pn=0&tn=ikaslist&rn=10&word=" + URLEncoder
                    .encode(ask, "utf-8") + "&fr=wwwt");
            Matcher dlm = Pattern.compile("<dl([\\s\\S]+?)</dl>").matcher(html);
            while (dlm.find()) {
                Matcher m = Pattern.compile("<a href=\"(http://zhidao.baidu.com/.+?)\"").matcher(dlm.group(1));
                if (m.find()) {
                    String res = getAnsInZhidao(m.group(1).replace("http://", "https://"));
                    if (res != null) {
                        return res;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getReply(ask);
    }

    private String getAnsInZhidao(String url) {
        String ansHTML = Util.getMobileHTML(url);
        String ans = null;
        if (ansHTML != null) {
            Matcher am = Pattern.compile("<div class=\"full-content\">\\s*([\\s\\S]+?)\\s*</div>").matcher(ansHTML);
            if (am.find()) {
                ans = am.group(1);
                ans = ans.replaceAll("<br[\\s\\S]*?/>", "\n");
                ans = ans.replaceAll("<img[\\s\\S]*?/>", "");
                ans = ans.replace("<p>", "").replace("</p>", "\n\n");
                ans = ans.replaceAll("<file[\\s\\S]+?/>", "");
                ans = ans.replaceAll("<a href[\\s\\S]+?></a>", "");
                ans = ans.replaceAll("&quot;", "\"");
                ans = ans.replaceAll("&amp;", "&");
                ans = ans.replaceAll("&lt;", "<");
                ans = ans.replaceAll("&gt;", ">");
                ans = ans.replaceAll("&nbsp;", " ");
            }
        }
        return ans;
    }

    String getPhoneLoc(String phoneNo) {
        String res = "暂时无法查询到该手机号码~";
        String json = Util.getHTML("https://www.iteblog.com/api/mobile.php?mobile=" + phoneNo);
        if (json != null) {
            JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
            String province = jsonObject.get("province").getAsString();
            String city = jsonObject.get("city").getAsString();
            if (city.equals(province)) {
                city = "";
            }
            String operator = jsonObject.get("operator").getAsString();
            res = "该号码归属地为" + province + city + "，运营商为" + operator + "。";
        }
        return res;
    }

    static ArrayList<String> getOntology(String word) {
        ArrayList<String> as = new ArrayList<>();
        try {
            String res = Util
                    .getHTML("http://shuyantech.com/api/cnprobase/ment2ent?q=" + URLEncoder.encode(word, "utf-8"));
            JsonObject jo = jsonParser.parse(res).getAsJsonObject();
            JsonArray array = jo.getAsJsonArray("ret");
            for (JsonElement j : array) {
                as.add(j.getAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return as;
    }

    static String getBaike(String word) {
        String res = "";
        try {
            String html = Util.getHTML("http://baike.baidu.com/search/none?word=" + URLEncoder.encode(word, "utf-8"));
            Matcher m = Pattern.compile("class=\"result-title\" href=\"(\\S+)\"").matcher(html);
            if (m.find()) {
                res = m.group(1);
            } else {
                return "";
            }
            if (res.startsWith("/")) {
                res = "http://baike.baidu.com" + res;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    String getImage(String ask) {
        String picUrl = "http://img.qqzhi.com/upload/img_0_2062652211D556319529_23.jpg";
        try {
            String url
                    = "https://image.baidu.com/search/index?tn=resultjson&ipn=r&ct=201326592&cl=2&lm=-1&st=-1&fm=result&fr=&sf=1&fmq=1497088452491_R&pv=&ic=0&nc=1&z=&se=1&showtab=0&fb=0&width=&height=&face=0&istype=2&ie=utf-8&word=";
            String json = Util.getHTML(url + URLEncoder.encode(ask, "utf-8"));
            JsonObject job = jsonParser.parse(json).getAsJsonObject();
            JsonArray jar = job.get("data").getAsJsonArray();
            job = jar.get(0).getAsJsonObject();
            picUrl = job.get("middleURL").getAsString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Util.getImgID(picUrl);
    }

    static String getNone() {
        return "我无法理解或服务器繁忙(╥╯^╰╥)";
    }
}
