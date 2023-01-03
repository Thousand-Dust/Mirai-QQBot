package com.qqbot.ai.classifier;

public class TextFile {
    private String content;
    private String category;

    public TextFile(String content, String category) {
        this.content = content;
        this.category = category;
    }

    public String getContent() {
        return content;
    }

    public String getCategory() {
        return category;
    }
}

