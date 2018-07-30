package mcs;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import org.ansj.domain.Result;
import org.ansj.lucene5.AnsjAnalyzer;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import word2vec.Word2VEC;
import word2vec.domain.WordEntry;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import static java.lang.Math.min;
import static java.lang.Math.sqrt;

public class Engine {

    private static Engine engine;
    private final int topDocNum = 1000;
    private IndexSearcher searcher;
    private Word2VEC vec;
    private svm_model model;
    private Emotion emotion;

    public synchronized static Engine get() {
        if (engine == null) {
            synchronized (Engine.class) {
                if (engine == null) {
                    engine = new Engine();
                }
            }
        }
        return engine;
    }

    private Engine() {
        try {
            this.searcher = new IndexSearcher(DirectoryReader.open(McsUtil.getIndexDir("pair")));
            this.vec = new Word2VEC();
            this.emotion = new Emotion();
            this.model = svm.svm_load_model(SVM.svmModel);
            vec.loadJavaModel(FilePath.get("Text\\w2vMay")); //预启动word2vec模型
            Result result = ToAnalysis.parse(""); //预启动ansj分词
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getW2VWords(String ask) {
        ArrayList<String> inits = McsUtil.segment(ask);
        ArrayList<String> words = new ArrayList<>();
        words.addAll(inits);
        for (String word : inits)
            for (WordEntry wordEntry : vec.distance(word))
                words.add(wordEntry.name);
        return words;
    }

    ArrayList<Pair> queryPair(ArrayList<String> words) {
        String query = "";
        for (String word : words)
            query += word + " ";
        QueryParser psr = new QueryParser("post", new AnsjAnalyzer(AnsjAnalyzer.TYPE.query_ansj));
        ArrayList<Pair> pairs = new ArrayList<>();
        try {
            Query q = psr.parse(query);
            ScoreDoc[] sdocs = searcher.search(q, topDocNum).scoreDocs;
            for (ScoreDoc sdoc : sdocs) {
                Document doc = searcher.doc(sdoc.doc);
                Pair pair = new Pair(doc.get("postID"), doc.get("post"), doc.get("cmntID"), doc.get("cmnt"));
                pairs.add(pair);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pairs;
    }

    double[] sims(String x, String y) {
        ArrayList<String> xArray = McsUtil.segment(x);
        ArrayList<String> yArray = McsUtil.segment(y);

        if(yArray.size() == 0) return new double[4];

        //余弦相似度
        HashMap<String, Integer> xVec = new HashMap<>();
        HashMap<String, Integer> yVec = new HashMap<>();
        for (String word : xArray)
            if (xVec.containsKey(word)) xVec.put(word, xVec.get(word) + 1);
            else xVec.put(word, 1);
        for (String word : yArray)
            if (yVec.containsKey(word)) yVec.put(word, yVec.get(word) + 1);
            else yVec.put(word, 1);
        HashSet<String> dims = new HashSet<>();
        dims.addAll(xVec.keySet());
        dims.addAll(yVec.keySet());
        int up = 0;
        for (String dim : dims) {
            int xi = xVec.containsKey(dim) ? xVec.get(dim) : 0;
            int yi = yVec.containsKey(dim) ? yVec.get(dim) : 0;
            up += xi * yi;
        }
        int dx = 0;
        int dy = 0;
        for (int c : xVec.values()) dx += c * c;
        for (int c : yVec.values()) dy += c * c;
        double cosSim = up / (sqrt(dx) * sqrt(dy));

        //词相似度
        double ovrSim = (xVec.size() + yVec.size() - dims.size()) / (double) min(xVec.size(), yVec.size());

        //词向量相似度
        int wordVecSize = vec.getWordVector("").length;
        float[] xWordVec = new float[wordVecSize];
        float[] yWordVec = new float[wordVecSize];
        for (String xWord : xArray) {
            float[] tempWordVec = vec.getWordVector(xWord);
            if(tempWordVec == null) continue;
            for (int i = 0; i < wordVecSize; i++) {
                xWordVec[i] += tempWordVec[i];
            }
        }
        for (String yWord : yArray) {
            float[] tempWordVec = vec.getWordVector(yWord);
            if(tempWordVec == null) continue;
            for (int i = 0; i < wordVecSize; i++) {
                yWordVec[i] += tempWordVec[i];
            }
        }
//        for (int i = 0; i < wordVecSize; i++) {
//            xWordVec[i] /= xArray.size();
//            yWordVec[i] /= yArray.size();
//        }

        float mul = 0;
        float xSquare = 0;
        float ySquare = 0;
        for (int i = 0; i < wordVecSize; i++) {
            mul += xWordVec[i] * yWordVec[i];
            xSquare += xWordVec[i] * xWordVec[i];
            ySquare += yWordVec[i] * yWordVec[i];
        }
        double w2vsim = mul / (sqrt(xSquare) * sqrt(ySquare));

        //情感相似度
        boolean xEmo = emotion.isPositive(xArray);
        boolean yEmo = emotion.isPositive(yArray);
        double emoSim = xEmo == yEmo ? 0.5 : 0.0;

        //防除以0
        if (min(xArray.size(), yArray.size()) == 0)
            cosSim = ovrSim = w2vsim = emoSim = 0;

        //汇总
        double[] sims = new double[4];
        sims[0] = cosSim;
        sims[1] = ovrSim;
        sims[2] = w2vsim;
        sims[3] = emoSim;
        return sims;
    }

    ArrayList<Double> features(String ask, Pair pair){
        double[] postSims = sims(ask, pair.getPost());
        double[] cmntSims = sims(ask, pair.getCmnt());
        ArrayList<Double> features = new ArrayList<>();
        for (Double x : postSims) features.add(x);
        for (Double x : cmntSims) features.add(x);
        return features;
    }

    private double polyScore(String ask, Pair pair) {
        ArrayList<Double> features = features(ask, pair);
        String args = "";
        DecimalFormat fmt = new DecimalFormat();
        fmt.setMaximumFractionDigits(6);
        for (double feature : features)
            args += fmt.format(feature) + ",";
        System.out.println(args);
        double[] coefs = {
                2.66855958e-06, -3.73262290e-05, 2.59944365e-05, -3.63024219e-04,
                1.41687537e-05, -9.48557710e-05, 4.64034300e-05, 3.05077368e-04,
                1.00009547e+05, -1.21698908e-03, 1.33615582e-03, 5.22030724e-03,
                -4.75734068e-04, 5.87127142e-04, -3.52776828e-04, -1.22548336e-03,
                4.19866891e+00, -2.61260140e-04, -3.17005369e-03, 2.76617404e-04,
                -9.53653271e-04, 9.64713301e-04, -3.27205078e-04, -1.29314263e+00,
                -5.33771496e-04, 5.55236444e-04, 5.87862497e-04, -1.28739310e-03,
                3.01297866e-03, 5.58760528e+00, 7.14175076e-06, -3.85873846e-05,
                -7.75138374e-05, 1.93212640e-04, 6.02352569e-01, 2.23038082e-04,
                -1.01498197e-04, 2.43615655e-03, 3.75995387e+00, -3.25132537e-04,
                -5.44100576e-05, -3.74128928e-01, -3.05150829e-03, -7.38910897e+00,
                -6.00018566e+05, 4.48106064e-04, -6.06249190e-04, -2.57496926e-04,
                1.40070449e-03, -1.51164285e-04, -2.90487495e-04, 1.99763664e-03,
                -1.43447891e-04, -1.59890705e-04, 1.93998037e-03, -1.14185920e-03,
                -8.06119898e-04, 1.07634980e-03, -1.64781875e-03, 5.80404412e-06,
                -1.10211896e-02, -2.86237795e-03, -1.01877062e-03, -8.61575791e-06,
                -2.88904425e-03, 1.09031999e-03, -2.38520900e-04, -6.60889585e-05,
                5.62001569e-04, 1.25773247e-03, 6.64662222e-04, 1.05181412e-03,
                -1.71299914e-03, 2.54088731e-03, 1.42427159e-04, 8.25519037e-05,
                3.23699725e-03, -7.01712215e-04, -7.11562346e-03, -5.89035081e-04,
                -8.39712440e+00, 2.72624369e-04, -1.19112672e-03, -2.67165069e-06,
                1.29993492e-03, -1.10754871e-03, -6.72753231e-04, 3.47796518e-04,
                6.37485385e-03, 9.27279723e-04, 5.67010702e-05, 1.60009997e-04,
                4.31323680e-03, -1.01740510e-03, 1.38282368e-04, -3.07466170e-04,
                -3.37549403e-04, 2.12817161e-03, -4.91575174e-04, -2.91417212e-04,
                9.38162219e-04, -3.62943711e-03, 6.03045250e-04, -3.07528160e-04,
                -2.24746993e-03, 2.89590239e-04, 9.23059376e-03, -3.98308900e-04,
                2.58580735e+00, 2.50874833e-03, 1.72400604e-03, 5.84667374e-03,
                -1.18374056e-03, -6.87585327e-03, -9.78005699e-04, 2.77633216e-04,
                1.34300611e-03, -4.53386161e-04, -8.08260361e-03, 2.61135457e-04,
                -1.07844961e-02, 1.11078681e-02, 5.96311297e-03, -2.62817850e-03,
                -5.83504493e-04, -1.05038204e-02, 1.30554278e-03, 4.56732328e-03,
                5.79918140e-03, -1.11757790e+01, 3.56374039e-06, -1.92699008e-05,
                -3.87931855e-05, 9.66841690e-05, 3.01175701e-01, -1.77641298e-03,
                1.47064081e-03, 3.10939748e-03, -9.32932162e-05, -1.28462177e-04,
                -1.33619984e-03, 9.03734433e-05, -3.08052726e-03, -1.99464916e-05,
                -1.50596177e+00, 1.20403167e-03, -1.43640996e-03, 2.13970855e-03,
                1.41014323e-03, 3.27761372e-04, -1.98571917e-03, -8.91769523e-04,
                -1.26910501e-02, 1.13539230e-03, -7.52125833e+00, 2.67266078e-04,
                -3.43430174e-04, -7.67689326e-05, 7.12365187e-03, -2.34796234e-04,
                7.49170759e-01, 6.37692692e-03, -9.00027141e-04, 1.47773575e+01,
                7.99998946e+05
        };
        int i = 0;
        double rank = 0;
        rank += coefs[i++];
        for (int k = 0; k < 8; k++)
            rank += coefs[i++] * features.get(k);

        for (int k = 0; k < 8; k++)
            for (int l = k; l < 8; l++)
                rank += coefs[i++] * features.get(k) * features.get(l);

        for (int k = 0; k < 8; k++)
            for (int l = k; l < 8; l++)
                for (int j = l; j < 8; j++)
                    rank += coefs[i++] * features.get(k) * features.get(l) * features.get(j);

        return rank;
    }

    private double pyScore(String ask, Pair pair) {
        ArrayList<Double> features = features(ask, pair);
        String args = "";
        DecimalFormat fmt = new DecimalFormat();
        fmt.setMaximumFractionDigits(6);
        for (double feature : features)
            args += fmt.format(feature) + ",";
        String cmd = "python D:\\PolyU\\Project\\Text\\p2j.py " + args;
        double rank = 0;
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            InputStream is = p.getInputStream();
            BufferedReader bf = new BufferedReader(new InputStreamReader(is));
            String rs = bf.readLine();
            rank = Double.valueOf(rs);
        }catch (Exception e){
            e.printStackTrace();
        }
        return rank;
    }

    private double svrScore(String ask, Pair pair) {
        ArrayList<Double> features = features(ask, pair);
        // 获取数据
        svm_node[] nodes = new svm_node[features.size()];
        int i = 0;
        for (double feature : features) {
            svm_node node = new svm_node();
            node.index = i + 1;
            node.value = feature;
            nodes[i++] = node;
        }
        return svm.svm_predict(model, nodes);
    }

    public String reply(String ask) {
        ArrayList<String> words = McsUtil.segment(ask);
        ArrayList<Pair> pairs = queryPair(words);
        String res;
        Pair best = pairs.get(0);
        double bstScore = 0;
        for (Pair pair : pairs) {
            if (McsUtil.segment(pair.getCmnt()).size() == 0) continue;
            double score = svrScore(ask, pair);
            if (score > bstScore) {
                best = pair;
                bstScore = score;
            }
        }
        res = best.getCmnt();
        System.out.println("Best score: " + bstScore);
        return res;
    }

    public static void screen() {
        long initime = System.currentTimeMillis();
        Engine engine = new Engine();
        Scanner message = new Scanner(System.in);
        System.out.println("Init time: " + (System.currentTimeMillis() - initime) / 1000.0 + "s");
        while (true) {
            String ask = message.nextLine();
            if (ask.equals("bye")) {
                System.out.println("再见!");
                break;
            } else {
                if (ask.equals("")) continue;
                long timer = System.currentTimeMillis();
                System.out.println(engine.reply(ask));
                System.out.println((System.currentTimeMillis() - timer) / 1000.0 + " s");
            }
        }
    }
}
