package com.example.sentimentaiproject.model;

import ai.onnxruntime.*;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.Map;

/**
 * Компонент для работы с ONNX моделью анализа тональности
 */
@Component
public class SentimentModel {

    private static final Logger logger = LoggerFactory.getLogger(SentimentModel.class);

    private OrtEnvironment environment;
    private OrtSession session;
    /**
     * -- GETTER --
     *  Проверка загружена ли модель
     */
    @Getter
    private boolean modelLoaded = false;

    // Конфигурация модели
    private final String MODEL_PATH = "model.onnx";
    private final int INPUT_SIZE = 128; // Размер входного вектора

    @PostConstruct
    public void init() {
        try {
            environment = OrtEnvironment.getEnvironment();

            // Попытка загрузить модель из classpath
            var modelUrl = getClass().getClassLoader().getResource(MODEL_PATH);
            if (modelUrl != null) {
                session = environment.createSession(modelUrl.getPath());
                modelLoaded = true;
                logger.info("✅ ONNX модель успешно загружена: {}", MODEL_PATH);
            } else {
                logger.warn("⚠️ ONNX модель не найдена: {}. Используется mock-анализатор", MODEL_PATH);
                modelLoaded = false;
            }
        } catch (Exception e) {
            logger.error("❌ Ошибка загрузки ONNX модели: {}", e.getMessage());
            modelLoaded = false;
        }
    }

    /**
     * Анализ тональности текста с использованием ONNX модели
     */
    public SentimentResult analyzeWithModel(String text) {
        if (!modelLoaded) {
            return fallbackAnalysis(text);
        }

        try {
            // Препроцессинг текста
            float[] inputData = preprocessText(text);

            // Создание входного тензора
            OnnxTensor inputTensor = OnnxTensor.createTensor(environment, inputData);
            Map<String, OnnxTensor> inputs = Collections.singletonMap("input", inputTensor);

            // Выполнение инференса
            try (OrtSession.Result results = session.run(inputs)) {
                OnnxTensor outputTensor = (OnnxTensor) results.get(0);
                float[] predictions = (float[]) outputTensor.getValue();

                SentimentResult result = interpretModelOutput(predictions, text);
                inputTensor.close();

                return result;
            }
        } catch (Exception e) {
            logger.error("Ошибка при анализе текста моделью: {}", e.getMessage());
            return fallbackAnalysis(text);
        }
    }

    /**
     * Препроцессинг текста для модели
     */
    private float[] preprocessText(String text) {

        String cleaned = text.toLowerCase()
                .replaceAll("[^a-zA-Zа-яА-Я0-9\\s]", "")
                .trim();

        float[] features = new float[INPUT_SIZE];

        // Простая векторизация на основе хэшей слов
        String[] words = cleaned.split("\\s+");
        for (int i = 0; i < Math.min(words.length, INPUT_SIZE); i++) {
            if (!words[i].isEmpty()) {
                features[i] = (words[i].hashCode() % 1000) / 1000.0f;
            }
        }

        // Заполнение оставшихся позиций
        for (int i = words.length; i < INPUT_SIZE; i++) {
            features[i] = 0.0f;
        }

        return features;
    }

    /**
     * Интерпретация выхода модели
     */
    private SentimentResult interpretModelOutput(float[] predictions, String originalText) {
        if (predictions == null || predictions.length < 3) {
            return fallbackAnalysis(originalText);
        }

        // Предполагаем формат выхода: [negative, neutral, positive]
        float negativeScore = predictions[0];
        float neutralScore = predictions[1];
        float positiveScore = predictions[2];

        String sentiment;
        float confidence;

        if (positiveScore > negativeScore && positiveScore > neutralScore) {
            sentiment = "positive";
            confidence = positiveScore;
        } else if (negativeScore > positiveScore && negativeScore > neutralScore) {
            sentiment = "negative";
            confidence = negativeScore;
        } else {
            sentiment = "neutral";
            confidence = neutralScore;
        }

        return new SentimentResult(originalText, sentiment, confidence, true);
    }

    /**
     * Резервный анализ на основе ключевых слов
     */
    private SentimentResult fallbackAnalysis(String text) {
        String lowerText = text.toLowerCase();

        // Расширенный словарь ключевых слов
        String[] positiveWords = {"good", "great", "excellent", "amazing", "happy", "love",
                "awesome", "fantastic", "perfect", "wonderful", "best",
                "beautiful", "brilliant", "outstanding", "superb"};

        String[] negativeWords = {"bad", "terrible", "horrible", "awful", "hate", "sad",
                "worst", "disappointing", "poor", "unhappy", "angry",
                "hateful", "disgusting", "annoying", "frustrating"};

        // Подсчет совпадений
        int positiveCount = countMatches(lowerText, positiveWords);
        int negativeCount = countMatches(lowerText, negativeWords);

        String sentiment;
        float confidence;

        if (positiveCount > negativeCount) {
            sentiment = "positive";
            confidence = Math.min(positiveCount / 10.0f, 1.0f);
        } else if (negativeCount > positiveCount) {
            sentiment = "negative";
            confidence = Math.min(negativeCount / 10.0f, 1.0f);
        } else {
            sentiment = "neutral";
            confidence = 0.5f;
        }

        return new SentimentResult(text, sentiment, confidence, false);
    }

    private int countMatches(String text, String[] words) {
        int count = 0;
        for (String word : words) {
            if (text.contains(word)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Получение информации о модели
     */
    public ModelInfo getModelInfo() {
        return new ModelInfo(
                MODEL_PATH,
                modelLoaded,
                INPUT_SIZE,
                Math.toIntExact(session != null ? session.getNumInputs() : 0),
                Math.toIntExact(session != null ? session.getNumOutputs() : 0)
        );
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (session != null) {
                session.close();
                logger.info("✅ ONNX сессия закрыта");
            }
            if (environment != null) {
                environment.close();
                logger.info("✅ ONNX окружение закрыто");
            }
        } catch (Exception e) {
            logger.error("Ошибка при очистке ресурсов модели: {}", e.getMessage());
        }
    }

    /**
     * Результат анализа тональности
     *
     * @param text Getters
     */
        public record SentimentResult(String text, String sentiment, float confidence, boolean modelUsed) {

    }

    /**
     * Информация о модели
     *
     * @param modelPath Getters
     */
        public record ModelInfo(String modelPath, boolean loaded, int inputSize, int numInputs, int numOutputs) {

    }
}
