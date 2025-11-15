package com.example.sentimentaiproject.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SentimentResponse {
    private String text;
    private String sentiment;
    private float confidence;
    private boolean modelUsed;

}