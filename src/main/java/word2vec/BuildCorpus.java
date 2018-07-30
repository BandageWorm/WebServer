package word2vec;

import mcs.FilePath;
import mcs.McsUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * build corpus for word2vec model building.
 * Created by kurtg on 17/1/25.
 */
public class BuildCorpus {

    static void writePCCorpus(String path) throws Exception {
        FileWriter fw = new FileWriter(new File(FilePath.get("Text\\corpuspc")), true);
        BufferedReader brp = new BufferedReader(new InputStreamReader(new FileInputStream(new File(
                FilePath.get(path + "repos-id-post-cn"))), "utf-8"));
        BufferedReader brc = new BufferedReader(new InputStreamReader(new FileInputStream(new File(
                FilePath.get(path + "repos-id-cmnt-cn"))), "utf-8"));
        String post;
        String cmnt;
        int i = 0;
        while ((post = brp.readLine()) != null && (cmnt = brc.readLine()) != null) {
            if (++i % 10000 == 0) {
                System.out.println(i);
            }
            post = post.split("\t")[1];
            cmnt = cmnt.split("\t")[1];
            ArrayList<String> postA = McsUtil.segment(post);
            ArrayList<String> cmntA = McsUtil.segment(cmnt);
            postA.addAll(cmntA);
            fw.write(writeLine(postA));
//            fw.write(writeLine(cmntA));
            fw.flush();
        }
        fw.close();
    }

    private static String writeLine(ArrayList<String> words) {
        StringBuilder sb = new StringBuilder();
        if (words.size() != 0) {
            for (String word : words) {
                sb.append(word + " ");
            }
        }
        String out = sb.toString().replaceAll("[ ]+", " ");

        return out + "\n";
    }

    static void reduce(String path) throws Exception {
        FileWriter fw = new FileWriter(new File(path + "_reduce"), true);
        BufferedReader br = new BufferedReader(new FileReader(new File(path)));
        String line;
        HashMap<Integer, String> cps = new HashMap<>();
        int i = 0;
        while ((line = br.readLine()) != null) {
            cps.put(++i, line);
        }
        Random rd = new Random();
        for (int j = 0; j < 250000; j++) {
            fw.write(cps.get(rd.nextInt(i)) + "\n");
            fw.flush();
        }
        fw.close();
    }

    public static void main(String[] args) throws Exception {
//        writeCorpus("D:\\PolyU\\Project\\Text\\wiki.txt");
        writePCCorpus(FilePath.get("DataSet\\repos\\"));
//        reduce("D:\\PolyU\\Project\\Text\\corpus");
    }
}
