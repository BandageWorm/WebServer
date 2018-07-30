package mcs;

import com.hankcs.hanlp.HanLP;
import org.ansj.domain.Result;
import org.ansj.recognition.impl.StopRecognition;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.in;

/**
 * McsUtil methods.
 * Created by kurtg on 17/1/22.
 */
public class McsUtil {
    public static Charset CHARSET= Charset.forName("utf-8");

    public static Directory getIndexDir(String repo) {
        //通过索引库名返回索引
        Path path = Paths.get(FilePath.get("Index"), repo);
        Directory res = null;
        try {
            res = FSDirectory.open(path);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public static ArrayList<String> segment(String text) {
        text = HanLP.convertToSimplifiedChinese(text);
        text = text.replaceAll("[\uD800\uDC00-\uDBFF\uDFFF]","");
        text = text.replaceAll("[\\x21-\\x7e]","").replaceAll("\\pP"," ").replaceAll("[ ]+"," ");
        ArrayList<String> words = new ArrayList<>();
        StopRecognition filter = new StopRecognition();
        //过滤词性：u/助词/ule了喽/uyy一样/uls来说/udeng等等
        //e叹词/y语气词/w标点/m数词/p介词/o拟声词/h前缀/k后缀
        filter.insertStopNatures("u","e","y","w","m","p","uyy","uls","udeng");
        filter.insertStopWords("的","了","是","有","着","得","之");
        Result result = ToAnalysis.parse(text).recognition(filter);
        String[] res;
        if (!result.toString().equals(""))
            res = result.toStringWithOutNature(" ").split(" ");
        else
            return new ArrayList<>();
        for(String word : res)
            if(!word.equals("")) words.add(word);
        return words;
    }

    public static String monoSegment(String text) {
        text = HanLP.convertToSimplifiedChinese(text);
        text = text.replaceAll("[\\w]","");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ' ') continue;
            sb.append(ch);
            sb.append('/');
        }
        String ret = sb.toString();
        if (ret.length() == 0) return "";
        else return ret.substring(0, ret.length() - 1);
    }
}
