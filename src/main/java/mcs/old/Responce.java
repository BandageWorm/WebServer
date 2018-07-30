package mcs.old;

import mcs.FilePath;
import mcs.McsUtil;

import org.ansj.lucene5.AnsjAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import word2vec.Word2VEC;
import word2vec.domain.WordEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

/**
 * New.
 * Created by kurtg on 17/1/25.
 */
public class Responce {

    private IndexSearcher searcher;
    private Word2VEC vec;

    public Responce() throws Exception {
        this.searcher = new IndexSearcher(DirectoryReader.open(McsUtil.getIndexDir("pair")));
        this.vec = new Word2VEC();
        vec.loadJavaModel(FilePath.get("Text\\w2v_pc"));
    }

    public HashSet<String> getW2VWords(String word) {
        HashSet<String> words = new HashSet<>();
        vec.setTopNSize(3);
        for (WordEntry wordEntry : vec.distance(word)) {
            words.add(wordEntry.name);
        }
        return words;
    }

        public String getResponse(String ask) throws Exception{
        ArrayList<String> words = McsUtil.segment(ask);
        HashSet<String> w2vs = new HashSet<>();
        for(String word : words)
            w2vs.addAll(getW2VWords(word));
        words.addAll(w2vs);
        String query = "";
        for(String word : words)
            query += word + " ";
        System.out.println("query words: " + query);
        QueryParser psr = new QueryParser("post",new AnsjAnalyzer(AnsjAnalyzer.TYPE.query_ansj));
        QueryParser csr = new QueryParser("cmnt",new AnsjAnalyzer(AnsjAnalyzer.TYPE.query_ansj));
        Query q = psr.parse(query);
        Query cq = csr.parse(query);
        ScoreDoc[] sPdocs = searcher.search(q,200).scoreDocs;
        ScoreDoc[] sCdocs = searcher.search(cq,200).scoreDocs;
        ScoreDoc best = sPdocs[0];
        double alpha = 0.7;
        double score = 0.0;
        for(ScoreDoc sPdoc : sPdocs){
            for(ScoreDoc sCdoc : sCdocs){
                if (sCdoc.doc == sPdoc.doc){
                    double thisscore = alpha*sPdoc.score + (1-alpha)*sCdoc.score;
                    if (thisscore >= score){
                        score = thisscore;
                        best = sCdoc;
                    }
                }
            }
        }
        if (score == 0.0) return "not found!";
        return searcher.doc(best.doc).get("cmnt");
    }

    private static void test(String ask1, boolean single) throws Exception{
        Responce res = new Responce();
        if (single) {
            System.out.println(res.getResponse(ask1));
        }
        else {
            Scanner message = new Scanner(System.in);
            while (true) {
                String ask = message.nextLine();
                if (ask.equals("bye")) {
                    System.out.println("再见!");
                    break;
                } else {
                    if (ask.equals("")) continue;
                    System.out.println(res.getResponse(ask));
                }
            }
        }
    }

    public static void main(String[] args) throws Exception{
        test("昨天晚上程序挂掉了。",true);
    }
}
