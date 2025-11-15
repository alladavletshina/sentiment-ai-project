package com.example.sentimentaiproject.dto;

import lombok.Data;

@Data
public class BatchTextRequest {
    private String[] texts;
}
