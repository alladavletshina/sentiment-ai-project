package com.example.sentimentaiproject.model;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Компонент для mock-анализа тональности текста
 */
@Component
public class SentimentModel {

    private static final Logger logger = LoggerFactory.getLogger(SentimentModel.class);

    /**
     * -- GETTER --
     *  Всегда false, так как используется mock-анализатор
     */
    @Getter
    private boolean modelLoaded = false;

    // Словари ключевых слов для анализа тональности
    private final String[] positiveWords = {
            "good", "great", "excellent", "amazing", "happy", "love",
            "awesome", "fantastic", "perfect", "wonderful", "best",
            "beautiful", "brilliant", "outstanding", "superb", "nice",
            "cool", "fantastic", "pleased", "delighted", "joy", "smile"
    };

    private final String[] negativeWords = {
            "bad", "terrible", "horrible", "awful", "hate", "sad",
            "worst", "disappointing", "poor", "unhappy", "angry",
            "hateful", "disgusting", "annoying", "frustrating", "ugly",
            "horrible", "dislike", "angry", "mad", "upset", "cry"
    };

    private final String[] neutralWords = {
            "ok", "okay", "fine", "normal", "regular", "usual",
            "standard", "average", "medium", "moderate", "decent"
    };

    @PostConstruct
    public void init() {
        logger.info("✅ Mock-анализатор тональности инициализирован");
        logger.info("📊 Загружено ключевых слов: {} положительных, {} отрицательных, {} нейтральных",
                positiveWords.length, negativeWords.length, neutralWords.length);
    }

    /**
     * Анализ тональности текста с использованием ключевых слов
     */
    public SentimentResult analyzeWithModel(String text) {
        return analyzeSentiment(text);
    }

    /**
     * Основной метод анализа тональности
     */
    private SentimentResult analyzeSentiment(String text) {
        String lowerText = text.toLowerCase().trim();

        // Подсчет совпадений для каждой категории
        int positiveCount = countMatches(lowerText, positiveWords);
        int negativeCount = countMatches(lowerText, negativeWords);
        int neutralCount = countMatches(lowerText, neutralWords);

        // Определение тональности на основе максимального количества совпадений
        String sentiment;
        float confidence;

        if (positiveCount > negativeCount && positiveCount > neutralCount) {
            sentiment = "positive";
            confidence = calculateConfidence(positiveCount, text.length());
        } else if (negativeCount > positiveCount && negativeCount > neutralCount) {
            sentiment = "negative";
            confidence = calculateConfidence(negativeCount, text.length());
        } else if (neutralCount > 0 && neutralCount >= positiveCount && neutralCount >= negativeCount) {
            sentiment = "neutral";
            confidence = calculateConfidence(neutralCount, text.length());
        } else {
            // Если нет явных совпадений, анализируем общий тон
            sentiment = analyzeGeneralTone(lowerText);
            confidence = 0.3f; // Низкая уверенность для общего анализа
        }

        // Корректировка уверенности на основе длины текста
        confidence = adjustConfidenceByTextLength(confidence, text.length());

        return new SentimentResult(text, sentiment, confidence, false);
    }

    /**
     * Подсчет совпадений слов в тексте
     */
    private int countMatches(String text, String[] words) {
        int count = 0;
        for (String word : words) {
            // Ищем целые слова, а не части слов
            if (text.contains(" " + word + " ") ||
                    text.startsWith(word + " ") ||
                    text.endsWith(" " + word) ||
                    text.equals(word)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Расчет уверенности на основе количества совпадений и длины текста
     */
    private float calculateConfidence(int matchCount, int textLength) {
        if (textLength == 0) return 0.0f;

        float baseConfidence = (float) matchCount / (textLength / 10.0f + 1);
        return Math.min(baseConfidence, 0.95f); // Ограничиваем максимальную уверенность
    }

    /**
     * Анализ общего тона текста (если нет явных ключевых слов)
     */
    private String analyzeGeneralTone(String text) {
        // Простой анализ на основе знаков препинания и общих паттернов
        if (text.contains("!") && text.contains("?")) {
            return "surprised";
        } else if (text.contains("!")) {
            return "excited";
        } else if (text.contains("?")) {
            return "curious";
        } else if (text.length() < 10) {
            return "neutral";
        } else {
            // Случайный выбор для разнообразия в демонстрационных целях
            String[] options = {"neutral", "slightly_positive", "slightly_negative"};
            int randomIndex = (text.hashCode() % options.length + options.length) % options.length;
            return options[randomIndex];
        }
    }

    /**
     * Корректировка уверенности на основе длины текста
     */
    private float adjustConfidenceByTextLength(float confidence, int textLength) {
        if (textLength < 5) {
            return confidence * 0.5f; // Низкая уверенность для коротких текстов
        } else if (textLength > 100) {
            return confidence * 1.1f; // Высокая уверенность для длинных текстов
        }
        return confidence;
    }

    /**
     * Получение информации о "модели" (теперь это mock-анализатор)
     */
    public ModelInfo getModelInfo() {
        return new ModelInfo(
                "mock-sentiment-analyzer",
                false, // Всегда false для mock-реализации
                0,     // Нет размера входа
                1,     // Один вход - текст
                1      // Один выход - тональность
        );
    }

    /**
     * Получение статистики по словарям (для отладки и мониторинга)
     */
    public Map<String, Object> getAnalyzerStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("positiveWordsCount", positiveWords.length);
        stats.put("negativeWordsCount", negativeWords.length);
        stats.put("neutralWordsCount", neutralWords.length);
        stats.put("analyzerType", "keyword-based");
        stats.put("version", "1.0.0");
        return stats;
    }

    /**
     * Результат анализа тональности
     */
    public record SentimentResult(String text, String sentiment, float confidence, boolean modelUsed) {
        // modelUsed всегда false для mock-реализации
    }

    /**
     * Информация о "модели"
     */
    public record ModelInfo(String modelPath, boolean loaded, int inputSize, int numInputs, int numOutputs) {
        // Всегда возвращает информацию о mock-анализаторе
    }
}