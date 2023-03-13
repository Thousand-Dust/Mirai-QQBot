package com.qqbot;

import com.qqbot.ai.TextClassifier;
import opennlp.tools.doccat.DoccatModel;

public class AiMain {

    public static void main(String[] args) {
        //数据文件的路径
        String[] paths = new String[]{Info.AI_DATA_PATH + "/data1.txt", Info.AI_DATA_PATH + "/data2.txt"};
        TextClassifier classifier = new TextClassifier(Info.AI_DATA_PATH + "/model.bin", paths);
        System.out.println("现在的模型准确率：" + classifier.getAccuracy());
        DoccatModel model = classifier.drillModel(paths, Info.AI_DATA_PATH + "/model2.bin", 380_000);
        classifier.setModel(model);
        System.out.println("训练完毕，模型准确率：" + classifier.getAccuracy());
    }

}
