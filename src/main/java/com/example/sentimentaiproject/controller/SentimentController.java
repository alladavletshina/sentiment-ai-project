package com.example.sentimentaiproject.controller;

import com.example.sentimentaiproject.dto.SentimentResponse;
import com.example.sentimentaiproject.model.SentimentModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SentimentController {

    @Autowired
    private SentimentModel sentimentModel;

    @GetMapping("/sentiment")
    public SentimentResponse analyzeSentiment(@RequestParam String text) {
        SentimentModel.SentimentResult result = sentimentModel.analyzeWithModel(text);
        return new SentimentResponse(
                result.getText(),
                result.getSentiment(),
                result.getConfidence(),
                result.isModelUsed()
        );
    }

    @GetMapping("/model/info")
    public Map<String, Object> getModelInfo() {
        SentimentModel.ModelInfo modelInfo = sentimentModel.getModelInfo();
        Map<String, Object> info = new HashMap<>();
        info.put("modelPath", modelInfo.getModelPath());
        info.put("modelLoaded", modelInfo.isLoaded());
        info.put("inputSize", modelInfo.getInputSize());
        info.put("numInputs", modelInfo.getNumInputs());
        info.put("numOutputs", modelInfo.getNumOutputs());
        return info;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "Sentiment Analysis API");
        status.put("modelStatus", sentimentModel.isModelLoaded() ? "LOADED" : "MOCK");
        return status;
    }
}