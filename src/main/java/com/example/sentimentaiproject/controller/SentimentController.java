package com.example.sentimentaiproject.controller;

import com.example.sentimentaiproject.config.ActuatorConfig;
import com.example.sentimentaiproject.dto.SentimentResponse;
import com.example.sentimentaiproject.model.SentimentModel;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SentimentController {

    private final SentimentModel sentimentModel;
    private final ActuatorConfig actuatorConfig; // ✅ ДОБАВИТЬ ЭТУ СТРОКУ

    public SentimentController(SentimentModel sentimentModel, ActuatorConfig actuatorConfig) {
        this.sentimentModel = sentimentModel;
        this.actuatorConfig = actuatorConfig;
    }

    @GetMapping("/sentiment")
    public SentimentResponse analyzeSentiment(@RequestParam String text) {

        actuatorConfig.incrementTotalRequests();

        long startTime = System.nanoTime();  // Для измерения времени

        SentimentModel.SentimentResult result = sentimentModel.analyzeWithModel(text);

        long duration = (System.nanoTime() - startTime) / 1_000_000;
        actuatorConfig.addAnalysisDuration(duration);
        actuatorConfig.incrementSuccessfulRequests();

        return new SentimentResponse(
                result.text(),
                result.sentiment(),
                result.confidence(),
                result.modelUsed()
        );
    }

    @GetMapping("/model/info")
    public Map<String, Object> getModelInfo() {
        actuatorConfig.incrementTotalRequests(); // ✅ МЕТРИКА ДЛЯ ВСЕХ ЗАПРОСОВ

        SentimentModel.ModelInfo modelInfo = sentimentModel.getModelInfo();
        Map<String, Object> info = new HashMap<>();
        info.put("modelPath", modelInfo.modelPath());
        info.put("modelLoaded", modelInfo.loaded());
        info.put("inputSize", modelInfo.inputSize());
        info.put("numInputs", modelInfo.numInputs());
        info.put("numOutputs", modelInfo.numOutputs());
        return info;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        actuatorConfig.incrementTotalRequests(); // ✅ МЕТРИКА ДЛЯ ВСЕХ ЗАПРОСОВ

        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "Sentiment Analysis API");
        status.put("modelStatus", sentimentModel.isModelLoaded() ? "LOADED" : "MOCK");
        return status;
    }
}