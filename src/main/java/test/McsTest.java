package test;

import mcs.Emotion;
import mcs.Engine;
import mcs.FilePath;
import mcs.McsUtil;
import org.junit.Test;
import wechat.Reply;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Created by kurtg on 17/6/16.
 */
public class McsTest {
    String testFilePath = FilePath.get("Text\\test-id-post-cn");

    @Test
    public void allTest() throws Exception{
        BufferedReader fr = new BufferedReader(new FileReader(new File(testFilePath)));
        String ask;
        while ((ask = fr.readLine()) != null){
            ask = ask.split("\t")[1];
            System.out.println(ask);
            long timer = System.currentTimeMillis();
            Reply re = new Reply(ask);
            re.testProcessReply();
            System.out.println(re.ans);
            timer = System.currentTimeMillis()-timer;
            System.out.print("takes " + timer/1000.0 + " s.\n\n");
        }
    }


    @Test
    public void emotionTest() throws Exception {
        Emotion eParser = new Emotion();
        BufferedReader testReader = new BufferedReader(new FileReader(new File(testFilePath)));
        String line;
        while ((line = testReader.readLine()) != null) {
            String text = line.split("\t")[1];
            ArrayList<String> words = McsUtil.segment(text);
            System.out.print((eParser.isPositive(words) ? "\uD83D\uDE01" : "\uD83D\uDE22") + "\t" + text);
            System.out.print(" | ");
            for (String s : words)
                System.out.print(" " + s);
            System.out.print("\n");
        }
    }

//    @Test
//    public void screenTest() throws Exception {
//        Engine.screen();
//    }
}
