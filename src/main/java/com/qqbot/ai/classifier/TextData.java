package com.qqbot.ai.classifier;

public class TextData {
    private String text;
    private String label;

    public TextData(String text, String label) {
        this.text = text;
        this.label = label;
    }

    public String getText() {
        return text;
    }

    public String getLabel() {
        return label;
    }
}
