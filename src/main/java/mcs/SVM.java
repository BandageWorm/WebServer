package mcs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;


public class SVM {
    static String svmModel = FilePath.get("/Text/SVM_model.txt");

    public static void main(String[] args) {
        saveModel(svmModel);
    }

    private static void saveModel(String filename){
        List<Double> label = new ArrayList<Double>();
        List<svm_node[]> nodeSet = new ArrayList<svm_node[]>();
        getData(nodeSet, label, FilePath.get("/Text/svm.txt"));

        int dataRange=nodeSet.get(0).length;
        svm_node[][] datas = new svm_node[nodeSet.size()][dataRange]; // 训练集的向量表
        for (int i = 0; i < datas.length; i++) {
            for (int j = 0; j < dataRange; j++) {
                datas[i][j] = nodeSet.get(i)[j];
            }
        }
        double[] lables = new double[label.size()]; // a,b 对应的lable
        for (int i = 0; i < lables.length; i++) {
            lables[i] = label.get(i);
        }
        // 定义svm_problem对象
        svm_problem problem = new svm_problem();
        problem.l = nodeSet.size(); // 向量个数
        problem.x = datas; // 训练集向量表
        problem.y = lables; // 对应的lable数组
        // 定义svm_parameter对象
        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.EPSILON_SVR;
        param.kernel_type = svm_parameter.RBF;
        param.cache_size = 1000;
        param.eps = 0.000001;
        param.C = 2;
        param.gamma = 20;
        // 如果参数没有问题，则svm.svm_check_parameter()函数返回null,否则返回error描述。

        System.out.println(svm.svm_check_parameter(problem, param));
        // svm.svm_train()训练出SVM分类模型
        svm_model model = svm.svm_train(problem, param);
        try {
            svm.svm_save_model(filename, model);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void getData(List<svm_node[]> nodeSet, List<Double> label,
                               String filename) {
        try {
            FileReader fr = new FileReader(new File(filename));
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                String[] datas = line.split(",");
                svm_node[] vector = new svm_node[datas.length - 1];
                for (int i = 1; i < datas.length; i++) {
                    svm_node node = new svm_node();
                    node.index = i - 1;
                    node.value = Double.parseDouble(datas[i]);
                    vector[i-1] = node;
                }
                nodeSet.add(vector);
                double lablevalue = Double.parseDouble(datas[0]);
                label.add(lablevalue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}