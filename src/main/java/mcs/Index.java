package mcs;

import org.ansj.lucene5.AnsjAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


/**
 * create index of STC repo including post and comments.
 * Created by kurtg on 17/1/17.
 */
public class Index {
    private static void createIndex() {
        int count = 1;
        Directory postDir = McsUtil.getIndexDir("post");
        Directory cmntDir = McsUtil.getIndexDir("cmnt");
        Analyzer ansj = new AnsjAnalyzer(AnsjAnalyzer.TYPE.index_ansj);
        try {
        IndexWriterConfig postConfig = new IndexWriterConfig(ansj);
        IndexWriterConfig cmntConfig = new IndexWriterConfig(ansj);
        IndexWriter postWriter = new IndexWriter(postDir, postConfig);
        IndexWriter cmntWriter = new IndexWriter(cmntDir, cmntConfig);
            String dataPath = "D:\\Project\\DataSet\\repos\\";
            BufferedReader postReader = new BufferedReader(new FileReader(new File(dataPath + "repos-id-post-cn")));
            BufferedReader cmntReader = new BufferedReader(new FileReader(new File(dataPath + "repos-id-cmnt-cn")));
            String postLine;
            String cmntLine;
            while ((postLine = postReader.readLine()) != null && (cmntLine = cmntReader.readLine()) != null) {
                String[] post = postLine.split("\t");
                String[] cmnt = cmntLine.split("\t");
                String postID = post[0].split("-")[2];
                String cmntID = cmnt[0].split("-")[2];
                Document postDoc = new Document();
                Document cmntDoc = new Document();
                postDoc.add(new StringField("n", String.valueOf(count), Field.Store.YES));
                postDoc.add(new StringField("postID", postID, Field.Store.YES));
                postDoc.add(new TextField("post", post[1], Field.Store.YES));
                cmntDoc.add(new StringField("n", String.valueOf(count), Field.Store.YES));
                cmntDoc.add(new StringField("cmntID", cmntID, Field.Store.YES));
                cmntDoc.add(new TextField("cmnt", cmnt[1], Field.Store.YES));
                postWriter.addDocument(postDoc);
                cmntWriter.addDocument(cmntDoc);
                count++;
            }
            postWriter.close();
            cmntWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Indexed doc number: " + count);
        }
    }

    static void combineIndex() {
        try {
            Directory dir = McsUtil.getIndexDir("pair");
            IndexWriterConfig config = new IndexWriterConfig(new AnsjAnalyzer(AnsjAnalyzer.TYPE.index_ansj));
            IndexWriter writer = new IndexWriter(dir, config);
            String dataPath = "D:\\PolyU\\Project\\DataSet\\repos\\";
            BufferedReader postReader = new BufferedReader(new FileReader(new File(dataPath + "repos-id-post-cn")));
            BufferedReader cmntReader = new BufferedReader(new FileReader(new File(dataPath + "repos-id-cmnt-cn")));
            String postLine;
            String cmntLine;
            while ((postLine = postReader.readLine()) != null && (cmntLine = cmntReader.readLine()) != null) {
                String[] post = postLine.split("\t");
                String[] cmnt = cmntLine.split("\t");
                String postID = post[0].split("-")[2];
                String cmntID = cmnt[0].split("-")[2];
                Document doc = new Document();
                doc.add(new StringField("postID", postID, Field.Store.YES));
                doc.add(new StringField("cmntID", cmntID, Field.Store.YES));
                doc.add(new TextField("post", post[1], Field.Store.YES));
                doc.add(new TextField("cmnt", cmnt[1], Field.Store.YES));
                writer.addDocument(doc);
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Indexed doc number: ");
        }
    }

    public static void main(String[] args) {
        createIndex();
    }
}
