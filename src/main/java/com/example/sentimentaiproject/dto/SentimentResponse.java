package com.example.sentimentaiproject.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
public class SentimentResponse {
    private String text;
    private String sentiment;
    private float confidence;
    private boolean modelUsed;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }
    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
    public boolean isModelUsed() { return modelUsed; }
    public void setModelUsed(boolean modelUsed) { this.modelUsed = modelUsed; }
}