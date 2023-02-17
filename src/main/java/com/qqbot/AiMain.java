package com.qqbot;

import com.qqbot.ai.TextClassifier;
import opennlp.tools.doccat.DoccatModel;

public class AiMain {

    public static void main(String[] args) {
        //数据文件的路径
        String[] paths = new String[]{"ai/data1.txt", "ai/data2.txt"};
        TextClassifier classifier = new TextClassifier("ai/model.bin", paths);
        System.out.println("现在的模型准确率：" + classifier.getAccuracy());
        DoccatModel model = classifier.drillModel(paths, "ai/model2.bin", 350_000);
        classifier.setModel(model);
        System.out.println("训练完毕，模型准确率：" + classifier.getAccuracy());
    }

}
