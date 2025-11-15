package com.example.sentimentaiproject.service;

import ai.onnxruntime.*;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SentimentService {

    private OrtEnvironment environment;
    private OrtSession session;
    private boolean modelLoaded = false;

    // Кэш для результатов анализа (упрощает нагрузочное тестирование)
    private final Map<String, String> analysisCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            environment = OrtEnvironment.getEnvironment();
            // Попытка загрузить ONNX модель
            session = environment.createSession("model.onnx", new OrtSession.SessionOptions());
            modelLoaded = true;
            System.out.println("✅ ONNX model loaded successfully");
        } catch (Exception e) {
            System.out.println("❌ ONNX model not found, using enhanced mock analyzer");
            System.out.println("💡 To use real model: Place model.onnx in resources directory");
            modelLoaded = false;
        }
    }

    public String analyze(String text, String method) {
        // Проверка кэша
        String cacheKey = text.toLowerCase().trim();
        if (analysisCache.containsKey(cacheKey)) {
            return analysisCache.get(cacheKey);
        }

        String sentiment;
        try {
            if (modelLoaded && "onnx".equals(method)) {
                sentiment = analyzeWithONNX(text);
            } else {
                sentiment = enhancedAnalyze(text);
            }
        } catch (Exception e) {
            sentiment = enhancedAnalyze(text); // Fallback
        }

        // Сохранение в кэш
        analysisCache.put(cacheKey, sentiment);
        return sentiment;
    }

    public Map<String, String> analyzeBatch(String[] texts) {
        Map<String, String> results = new HashMap<>();
        for (String text : texts) {
            results.put(text, analyze(text, "auto"));
        }
        return results;
    }

    private String analyzeWithONNX(String text) throws OrtException {
        try {
            // Препроцессинг текста
            float[] inputData = preprocessTextForONNX(text);
            long[] shape = {1, inputData.length};

            // Создание входного тензора
            OnnxTensor inputTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(inputData), shape);
            Map<String, OnnxTensor> inputs = Collections.singletonMap("input", inputTensor);

            // Выполнение инференса
            try (OrtSession.Result results = session.run(inputs)) {
                OnnxTensor outputTensor = (OnnxTensor) results.get(0);
                float[] predictions = (float[]) outputTensor.getValue();
                return interpretONNXPrediction(predictions);
            } finally {
                inputTensor.close();
            }
        } catch (Exception e) {
            throw new OrtException("ONNX inference failed: " + e.getMessage());
        }
    }

    private String enhancedAnalyze(String text) {
        text = text.toLowerCase().trim();

        // Расширенный анализ на основе ключевых слов и контекста
        Map<String, Integer> positiveWords = Map.ofEntries(
                Map.entry("good", 2),
                Map.entry("great", 3),
                Map.entry("excellent", 4),
                Map.entry("amazing", 3),
                Map.entry("happy", 2),
                Map.entry("love", 3),
                Map.entry("awesome", 3),
                Map.entry("fantastic", 3),
                Map.entry("perfect", 4),
                Map.entry("wonderful", 3),
                Map.entry("best", 3),
                Map.entry("beautiful", 2)
        );

        Map<String, Integer> negativeWords = Map.ofEntries(
                Map.entry("bad", 2),
                Map.entry("terrible", 4),
                Map.entry("hate", 3),
                Map.entry("awful", 3),
                Map.entry("sad", 2),
                Map.entry("horrible", 4),
                Map.entry("worst", 4),
                Map.entry("disappointing", 3),
                Map.entry("poor", 2),
                Map.entry("unhappy", 2),
                Map.entry("angry", 2),
                Map.entry("hateful", 3)
        );

        int positiveScore = calculateSentimentScore(text, positiveWords);
        int negativeScore = calculateSentimentScore(text, negativeWords);

        // Анализ усилителей и отрицаний
        double adjustedScore = adjustScoreWithContext(text, positiveScore - negativeScore);

        if (adjustedScore > 1.5) return "positive";
        else if (adjustedScore < -1.5) return "negative";
        else return "neutral";
    }

    private int calculateSentimentScore(String text, Map<String, Integer> wordScores) {
        int score = 0;
        for (Map.Entry<String, Integer> entry : wordScores.entrySet()) {
            if (text.contains(entry.getKey())) {
                score += entry.getValue();
            }
        }
        return score;
    }

    private double adjustScoreWithContext(String text, int baseScore) {
        double adjustment = baseScore;

        // Усилители
        if (text.contains("very") || text.contains("really") || text.contains("extremely")) {
            adjustment *= 1.5;
        }

        // Отрицания
        if (text.contains("not ") || text.contains("no ") || text.contains("never ")) {
            adjustment *= -0.7;
        }

        // Восклицательные знаки (показывают эмоциональную окраску)
        long exclamationCount = text.chars().filter(ch -> ch == '!').count();
        adjustment += exclamationCount * 0.3;

        return adjustment;
    }

    private float[] preprocessTextForONNX(String text) {
        String cleaned = text.toLowerCase()
                .replaceAll("[^a-zA-Z\\s]", "")
                .trim();

        // Создание фиксированного размера вектора признаков
        float[] features = new float[128];
        Arrays.fill(features, 0.0f);

        // Простая бинарная векторизация (в реальности используйте Word2Vec/BERT)
        String[] words = cleaned.split("\\s+");
        for (int i = 0; i < Math.min(words.length, features.length); i++) {
            features[i] = words[i].hashCode() % 100 / 100.0f; // Простой хэш-признак
        }

        return features;
    }

    private String interpretONNXPrediction(float[] predictions) {
        if (predictions == null || predictions.length < 3) {
            return "neutral";
        }

        // Предполагаем, что модель выдает вероятности для [negative, neutral, positive]
        int maxIndex = 0;
        for (int i = 1; i < predictions.length; i++) {
            if (predictions[i] > predictions[maxIndex]) {
                maxIndex = i;
            }
        }

        return switch (maxIndex) {
            case 0 -> "negative";
            case 1 -> "neutral";
            case 2 -> "positive";
            default -> "unknown";
        };
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (session != null) {
                session.close();
                System.out.println("✅ ONNX session closed");
            }
            if (environment != null) {
                environment.close();
                System.out.println("✅ ONNX environment closed");
            }
        } catch (Exception e) {
            System.err.println("Error cleaning up ONNX resources: " + e.getMessage());
        }
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }
}