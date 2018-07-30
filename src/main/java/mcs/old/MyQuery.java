package mcs.old;

import mcs.Emotion;
import mcs.McsUtil;

import org.ansj.lucene5.AnsjAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

/**
 * Conversation with built index.
 * Created by kurtg on 17/1/20.
 */
public class MyQuery {
    private IndexSearcher postSearcher;
    private IndexSearcher cmntSearcher;
    private Analyzer analyzer;

    public MyQuery(IndexSearcher postSearcher, IndexSearcher cmntSearcher, Analyzer analyzer) {
        this.postSearcher = postSearcher;
        this.cmntSearcher = cmntSearcher;
        this.analyzer = analyzer;
    }

    public MyQuery() throws Exception{
        this.postSearcher = new IndexSearcher(DirectoryReader.open(McsUtil.getIndexDir("post")));
        this.cmntSearcher = new IndexSearcher(DirectoryReader.open(McsUtil.getIndexDir("cmnt")));
        this.analyzer = new AnsjAnalyzer(AnsjAnalyzer.TYPE.query_ansj);
    }

    private HashMap<Integer, String> Answer(String query, int N) throws Exception {
        //分析乐观/悲观情感
        Emotion eParser = new Emotion();
        boolean pos = eParser.isPositive(McsUtil.segment(query));
        System.out.println(pos ? "\uD83D\uDE01" : "\uD83D\uDE22");
        //解析输入语句，生成query
        QueryParser parser = new QueryParser("post", analyzer);
        Query post = parser.parse(query);
        //搜索对应post，打印对应cmnt
        ScoreDoc[] postHits = postSearcher.search(post, N).scoreDocs;
        HashMap<Integer, String> getID = new HashMap<>();
        for (int i = 0; i < N; i++) {
            Document postHitDoc = postSearcher.doc(postHits[i].doc);
            String n = postHitDoc.get("n");
            Query cq = new TermQuery(new Term("n", n));
            ScoreDoc hit = cmntSearcher.search(cq, 1).scoreDocs[0];
            Document cmnt = cmntSearcher.doc(hit.doc);
            System.out.println(i + ".\t" + cmnt.get("cmnt"));
            getID.put(i, cmnt.get("cmntID"));
        }
        //返回cmnt语句得分顺序<K>对应的cmntID<V>
        return getID;
    }

    public String Reply(String query) throws Exception {
        //分析乐观/悲观情感
        int N = 4;
        Emotion eParser = new Emotion();
        boolean pos = eParser.isPositive(McsUtil.segment(query));
        System.out.println(pos ? "\uD83D\uDE01" : "\uD83D\uDE22");
        //解析输入语句，生成query
        QueryParser parser = new QueryParser("post", analyzer);
        Query post = parser.parse(query);
        //搜索对应post，打印对应cmnt
        ScoreDoc[] postHits = postSearcher.search(post, N).scoreDocs;
        Random rd = new Random();
        int i = rd.nextInt(N);
        Document postHitDoc = postSearcher.doc(postHits[i].doc);
        String n = postHitDoc.get("n");
        Query cq = new TermQuery(new Term("n", n));
        ScoreDoc hit = cmntSearcher.search(cq, 1).scoreDocs[0];
        Document cmnt = cmntSearcher.doc(hit.doc);
        return cmnt.get("cmnt");
    }

    private static void testQuery(int N) throws Exception {
        //打印前N个cmnt结果
        DirectoryReader postReader = DirectoryReader.open(McsUtil.getIndexDir("post"));
        DirectoryReader cmntReader = DirectoryReader.open(McsUtil.getIndexDir("cmnt"));
        IndexSearcher postSearcher = new IndexSearcher(postReader);
        IndexSearcher cmntSearcher = new IndexSearcher(cmntReader);
        Analyzer ik = new AnsjAnalyzer(AnsjAnalyzer.TYPE.query_ansj);
        MyQuery query = new MyQuery(postSearcher, cmntSearcher, ik);
        Scanner message = new Scanner(System.in);
        while (true) {
            String ask = message.nextLine();
            if (ask.equals("bye")) {
                System.out.println("再见!");
                break;
            } else {
                query.Answer(ask, N);
            }
        }
    }

    private static void trainQuery() throws Exception {
        //打印前20个cmnt结果，人为判断最优结果并输入顺序号，人为判断结果存入material.txt
        DirectoryReader postReader = DirectoryReader.open(McsUtil.getIndexDir("post"));
        DirectoryReader cmntReader = DirectoryReader.open(McsUtil.getIndexDir("cmnt"));
        IndexSearcher postSearcher = new IndexSearcher(postReader);
        IndexSearcher cmntSearcher = new IndexSearcher(cmntReader);
        Analyzer ik = new AnsjAnalyzer(AnsjAnalyzer.TYPE.base_ansj);
        MyQuery query = new MyQuery(postSearcher, cmntSearcher, ik);
        Scanner message = new Scanner(System.in);
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File("material.txt"), true));
        try {
            while (true) {
                String ask = message.nextLine();
                if (ask.equals("bye")) {
                    System.out.println("再见!");
                    break;
                } else {
                    HashMap<Integer, String> hits = query.Answer(ask, 20);
                    String rank = message.nextLine();
                    String id = hits.get(Integer.valueOf(rank));
                    System.out.println(id);
                    writer.write(ask + "\t" + id + "\n");
                }
            }
        } finally {
            writer.close();
        }
    }

    public static void main(String[] args) throws Exception {
        testQuery(5);
    }
}
