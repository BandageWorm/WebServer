package mcs.old;

import mcs.FilePath;
import org.ansj.lucene5.AnsjAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.classification.ClassificationResult;
import org.apache.lucene.classification.KNearestNeighborClassifier;
import org.apache.lucene.classification.SimpleNaiveBayesClassifier;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import static mcs.McsUtil.getIndexDir;


/**
 * create index of STC repo including post and comments.
 * Created by kurtg on 17/1/17.
 */
public class Classifier {
    public static void main(String args[]) throws IOException {
        Date startDate = new Date();
        ArrayList<Integer> counterArrayList=new ArrayList<>();
        boolean isKnearst = false;
        KNearestNeighborClassifier kNearestNeighborClassifier = null;
        SimpleNaiveBayesClassifier simpleNaiveBayesClassifier = null;
        ClassificationResult<BytesRef> classificationResult = null;
        if (isKnearst) {
            kNearestNeighborClassifier=new KNearestNeighborClassifier(12);
        }else {
            simpleNaiveBayesClassifier=new SimpleNaiveBayesClassifier();
        }

        IndexReader reader= DirectoryReader.open(getIndexDir("post"));
        LeafReaderContext context=reader.getContext().leaves().get(0);//very important!!--leaves=>list<AtomicReaderContext>
        LeafReader leafReader =context.reader();
        Analyzer analyzer=new AnsjAnalyzer(AnsjAnalyzer.TYPE.index_ansj);
        if (isKnearst) {
            kNearestNeighborClassifier.train(leafReader, "post", "theclass", analyzer);
        }else {
            simpleNaiveBayesClassifier.train(leafReader, "post", "theclass", analyzer);
        }
        //////
        int lenSum = 0, trueSum = 0;
        final File docDir=new File(FilePath.get("TESTs"));
        String[] subdoclist=docDir.list();
        for (int i = 0; i < subdoclist.length; i++) {
            counterArrayList.clear();
            for (int ii = 0; ii < subdoclist.length; ii++) {
                counterArrayList.add(0);
            }
            System.err.println("------------------------"+"now testing on "+classref(i)+"------------------------");
            File docdir2=new File(FilePath.get("TESTs/" + subdoclist[i]));
            String[] fileStrings=docdir2.list();
            if (fileStrings!=null) {

                for (int k = 0; k < fileStrings.length; k++) {
//					System.out.println(fileStrings[0]);
                    BufferedReader in = new BufferedReader(new FileReader(new File(FilePath.get("TESTs/" + subdoclist[i] +"/"+ fileStrings[k]))));
                    String str;StringBuilder stringBuilder=new StringBuilder();
                    while ((str = in.readLine()) != null)
                    {
                        stringBuilder.append(str);
                    }
                    in.close();
                    //////
                    if (isKnearst) {
                        classificationResult=kNearestNeighborClassifier.assignClass(stringBuilder.toString());
                    }else {
                        classificationResult=simpleNaiveBayesClassifier.assignClass(stringBuilder.toString());
                    }
                    BytesRef bytesRef=classificationResult.getAssignedClass();
                    byte[] refs=bytesRef.bytes;
                    StringBuilder stringBuilder2=new StringBuilder();
                    for (byte ref : refs) {
                        stringBuilder2.append((char)ref);
                    }
                    String classString=stringBuilder2.toString().trim();
                    counterArrayList.set(Integer.parseInt(classString.substring(1))-1, counterArrayList.get(Integer.parseInt(classString.substring(1))-1)+1);
                }
            }

            System.out.println("-------------------------------------test results on "+classref(i)+"--------------------------------------");
            for (int j = 0; j <subdoclist.length ; j++) {//7 classes
                System.err.println(classref(j)+"---->"+counterArrayList.get(j));
            }
            System.out.println("total number:"+fileStrings.length);
            lenSum+=fileStrings.length;
            System.out.println("true positives:"+counterArrayList.get(i));
            trueSum+=counterArrayList.get(i);
            System.out.println("accuracy rate:"+(double)counterArrayList.get(i)/fileStrings.length);
            System.err.println("***************************************end for "+classref(i)+"************************************");
        }
        Date end = new Date();
        System.out.println(end.getTime() - startDate.getTime() + " total milliseconds");
        System.out.println("final size:"+lenSum);
        System.out.println("final true positives:"+trueSum);
        System.out.println("final accuracy rate:"+(double)trueSum/lenSum);
    }
    public static String classref(int i){
        switch (i) {
            case 0:
                return "course";
            case 1:
                return "department";
            case 2:
                return "faculty";
            case 3:
                return "other";
            case 4:
                return "project";
            case 5:
                return "staff";
            case 6:
                return "student";
            default:
                return "";
        }
    }
}
