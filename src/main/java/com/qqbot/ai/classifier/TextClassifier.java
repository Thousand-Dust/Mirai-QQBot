package com.qqbot.ai.classifier;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 朴素贝叶斯文本分类器
 * @author Thousand Dust
 */
public class TextClassifier {

    // 记录所有单词出现的总次数
    private int totalWords;
    // 记录每个类别中单词出现的总次数
    private Map<String, Integer> classWordCounts;
    // 记录每个单词在每个类别中出现的次数
    private Map<String, Map<String, Integer>> wordCountsInClass;
    // 记录每个单词的总出现次数
    private Map<String, Integer> wordCounts;

    public TextClassifier() {
        this.totalWords = 0;
        this.classWordCounts = new HashMap<>();
        this.wordCountsInClass = new HashMap<>();
        this.wordCounts = new HashMap<>();
    }

    public void train(List<TextData> dataList) {
        for (TextData data : dataList) {
            String text = data.getText();
            String label = data.getLabel();

            // 统计类别中单词出现的总次数
            int count = classWordCounts.getOrDefault(label, 0);
            classWordCounts.put(label, count + text.split(" ").length);

            // 统计每个单词在类别中出现的次数
            Map<String, Integer> wordCounts = wordCountsInClass.getOrDefault(label, new HashMap<>());
            for (String word : text.split(" ")) {
                int wordCount = wordCounts.getOrDefault(word, 0);
                wordCounts.put(word, wordCount + 1);
            }
            wordCountsInClass.put(label, wordCounts);

            // 统计每个单词的总出现次数
            for (String word : text.split(" ")) {
                count = this.wordCounts.getOrDefault(word, 0);
                this.wordCounts.put(word, count + 1);
                totalWords++;
            }
        }
    }

    public String classify(String text) {
        String label = "";
        double maxProb = Double.MIN_VALUE;

        for (String classLabel : classWordCounts.keySet()) {
            // 计算 P(C)
            double classProb = (double) classWordCounts.get(classLabel) / totalWords;
            double prob = classProb;
            // 计算 P(W | C)
            for (String word : text.split(" ")) {
                int count = wordCountsInClass.get(classLabel).getOrDefault(word, 0);
                double wordProb = (count + 1.0) / (classWordCounts.get(classLabel) + wordCounts.size());
                prob *= wordProb;
            }

            // 取最大概率
            if (prob > maxProb) {
                maxProb = prob;
                label = classLabel;
            }
        }

        return label;
    }

    /**
     * 保存模型
     * @param filePath
     * @throws IOException
     */
    public void saveModel(String filePath) throws IOException {
        // 创建一个 ObjectOutputStream 对象，用于将训练结果写入文件
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            // 将训练结果写入文件
            out.writeObject(totalWords);
            out.writeObject(classWordCounts);
            out.writeObject(wordCountsInClass);
            out.writeObject(wordCounts);
        }
    }

    /**
     * 加载模型
     * @param filePath
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void loadModel(String filePath) throws IOException, ClassNotFoundException {
        //判断文件是否存在
        if (!new File(filePath).exists()) {
            return;
        }
        // 创建一个 ObjectInputStream 对象，用于从文件中读取训练结果
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath))) {
            // 从文件中读取训练结果
            totalWords = (int) in.readObject();
            classWordCounts = (Map<String, Integer>) in.readObject();
            wordCountsInClass = (Map<String, Map<String, Integer>>) in.readObject();
            wordCounts = (Map<String, Integer>) in.readObject();
        }
    }


}
