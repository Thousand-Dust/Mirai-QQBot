package com.qqbot;

import com.qqbot.ai.TextClassifier;

public class AiMain {

    public static void main(String[] args) {
        String[] paths = new String[]{"ai/data1.txt", "ai/data2.txt"};
        TextClassifier classifier = new TextClassifier("ai/model.bin", paths);
        classifier.drillModel(paths, "ai/model2.bin", 500_000);
        System.out.println("训练完毕，模型准确率：" + classifier.getAccuracy());
    }

}
