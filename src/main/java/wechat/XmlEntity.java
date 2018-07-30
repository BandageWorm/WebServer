package wechat;

/**
 * Received XML entity.
 * Created by kurtg on 17/1/22.
 */
public class XmlEntity {
    private String ToUserName = "";
    private String FromUserName = "";
    private String CreateTime = "";
    private String MsgType = "";
    private String MsgId = "";
    private String Content = "";
    private String Format = "";
    private String MediaId = "";
    private String URL = "";
    private String Recognition = "";

    public String getRecognition() {
        return Recognition;
    }

    public String getFormat() {
        return Format;
    }

    public String getMediaId() {
        return MediaId;
    }

    public String getURL() {
        return URL;
    }

    public String getToUserName() {
        return ToUserName;
    }

    public String getFromUserName() {
        return FromUserName;
    }

    public String getCreateTime() {
        return CreateTime;
    }

    public String getMsgType() {
        return MsgType;
    }

    public String getMsgId() {
        return MsgId;
    }

    public String getContent() {
        return Content;
    }

    public void setToUserName(String toUserName) {
        ToUserName = toUserName;
    }

    public void setFromUserName(String fromUserName) {
        FromUserName = fromUserName;
    }

    public void setCreateTime(String createTime) {
        CreateTime = createTime;
    }

    public void setMsgType(String msgType) {
        MsgType = msgType;
    }

    public void setMsgId(String msgId) {
        MsgId = msgId;
    }

    public void setContent(String content) {
        Content = content;
    }

    public void setFormat(String format) {
        Format = format;
    }

    public void setMediaId(String mediaId) {
        MediaId = mediaId;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public void setRecognition(String recognition) {
        Recognition = recognition;
    }
}
