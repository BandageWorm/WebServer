package mcs;

import java.io.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Format label data.
 * Created by kurtg on 17/2/3.
 */
public class Label {
    private Engine psr = Engine.get();
    class label{
        String askID;
        String cmntID;
        int label;

        label(String askID, String cmntID, int label) {
            this.askID = askID;
            this.cmntID = cmntID;
            this.label = label;
        }
    }

    private void writeMaterial(){
        try {
            BufferedReader lbf = new BufferedReader(new FileReader(new File(FilePath.get("Text\\train-label"))));
            BufferedReader tbf = new BufferedReader(new FileReader(new File(FilePath.get("Text\\train-id-post-cn"))));
            FileWriter fw = new FileWriter(new File(FilePath.get("Text\\svm.txt")));
            HashSet<label> labels = new HashSet<>();
            String lbfLine = "";
            while ((lbfLine = lbf.readLine()) != null){
                String[] decom = lbfLine.split("\t");
                String askID = decom[0].split("-")[2];
                String cmntID = decom[1].split("-")[2];
                int intL = Integer.valueOf(decom[2].substring(1,2));
                labels.add(new label(askID, cmntID, intL));
            }
            lbf.close();

            String tbfLine = "";
            int i = 0;
            while ((tbfLine = tbf.readLine()) != null){
                String ask = tbfLine.split("\t")[1];
                String askID = tbfLine.split("\t")[0].split("-")[2];
                ArrayList<String> words = McsUtil.segment(ask);
                ArrayList<Pair> recalls = psr.queryPair(words);
                for (Pair pair : recalls){
                    String cmntID = pair.getCmntID();
                    for (label lb : labels){
                        if (lb.askID.equals(askID) && lb.cmntID.equals(cmntID)){
                            if (i++ % 100 == 0) System.out.println(i + " data finished.");
                            String wline = makeCSV(ask, pair,lb.label);
                            fw.write(wline);
                            fw.flush();
                        }
                    }
                }
            }
            tbf.close();
            fw.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private String makeLine(String ask, Pair pair, int label){
        ArrayList<Double> features = psr.features(ask, pair);
        String line = "+" + label;
        int i = 1;
        DecimalFormat fmt = new DecimalFormat();
        fmt.setMaximumFractionDigits(4);
        fmt.setMinimumFractionDigits(4);
        fmt.setRoundingMode(RoundingMode.UP);
        for (double sim: features) {
            line += " " + Integer.toString(i++) + ":" + fmt.format(sim);
        }
        return line + Integer.toString(label) + "\n";
    }

    private String makeCSV(String ask, Pair pair, int label){
        ArrayList<Double> features = psr.features(ask, pair);
        String line = "" + label;
        DecimalFormat df = new DecimalFormat("#.000000");
        for (double sim: features)
            line += "," + df.format(sim);
        return line + "\n";
    }

    public static void main(String[] args){
        Label label = new Label();
        label.writeMaterial();
    }
}
