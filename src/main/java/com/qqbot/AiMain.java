package com.qqbot;

import com.qqbot.ai.TextClassifier;

public class AiMain {

    public static void main(String[] args) {
        TextClassifier classifier = new TextClassifier("ai/classifier.bin", new String[0]);
        classifier.drillModel(new String[]{"ai/data1.txt", "ai/data2.txt"}, "ai/model2.bin");
        System.out.println("训练结束");
    }

}
