package com.example.sentimentaiproject.dto;
import lombok.Data;
import java.util.Map;

@Data
public class BatchSentimentResponse {
    private Map<String, String> results;
    private String status;
}
