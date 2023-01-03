package com.qqbot.ai.classifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NaiveBayesClassifier {
    private Map<String, Integer> categoryCounts = new HashMap<>();
    private Map<String, Map<String, Integer>> wordCounts = new HashMap<>();
    private int totalDocCount = 0;

    public void train(List<TextFile> documents) {
        for (TextFile document : documents) {
            // 增加该类别的文档数量
            String category = document.getCategory();
            categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
            totalDocCount++;

            // 将文档分割成单词
            String[] words = document.getContent().split("\\s+");
            for (String word : words) {
                // 将单词转换为小写，并去除标点符号
                word = word.toLowerCase().replaceAll("\\W", "");

                // 增加单词在该类别中的出现次数
                wordCounts.putIfAbsent(category, new HashMap<>());
                wordCounts.get(category).put(word, wordCounts.get(category).getOrDefault(word, 0) + 1);
            }
        }
    }

    public String predict(String text) {
        String[] words = text.split("\\s+");

        // 计算文本属于每个类别的概率
        Map<String, Double> scores = new HashMap<>();
        for (String category : categoryCounts.keySet()) {
            scores.put(category, Math.log(categoryCounts.get(category) / (double) totalDocCount));

            for (String word : words) {
                // 将单词转换为小写，并去除标点符号
                word = word.toLowerCase().replaceAll("\\W", "");

                // 统计该单词在该类别中的出现次数
                int count = wordCounts.get(category).getOrDefault(word, 0);

                // 计算单词在该类别中的概率
                double wordProbability = count / (double) categoryCounts.get(category);

                // 将单词的概率乘到总概率上
                scores.put(category, scores.get(category) + Math.log(wordProbability));
            }
        }

        // 返回概率最大的类别
        String bestCategory = null;
        double maxScore = Double.NEGATIVE_INFINITY;
        for (String category : scores.keySet()) {
            double score = scores.get(category);
            if (score > maxScore) {
                maxScore = score;
                bestCategory = category;
            }
        }
        System.out.println(maxScore);
        return bestCategory;
    }
}