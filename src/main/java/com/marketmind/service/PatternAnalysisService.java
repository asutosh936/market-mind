package com.marketmind.service;

import com.marketmind.model.dto.PatternDto;
import com.marketmind.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for detecting candlestick patterns.
 * This service holds all pattern detectors and orchestrates pattern detection.
 * Patterns are sorted by confidence descending.
 */
@Service
public class PatternAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(PatternAnalysisService.class);
    private final List<PatternDetector> detectors;

    public PatternAnalysisService(List<PatternDetector> detectors) {
        this.detectors = detectors;
        logger.info("PatternAnalysisService initialized with {} detectors", detectors.size());
    }

    /**
     * Detect all applicable patterns for the current candle with historical context.
     * 
     * @param currentCandle The current candlestick to analyze
     * @param previousCandles List of previous candles (typically last 5) for pattern context
     * @return List of detected patterns sorted by confidence (highest first)
     */
    public List<PatternDto> detectPatterns(MarketData currentCandle, List<MarketData> previousCandles) {
        logger.debug("Detecting patterns for symbol={}, timestamp={}", 
                   currentCandle.getSymbol(), currentCandle.getTimestamp());

        // Collect results from all detectors
        List<PatternDto> detectedPatterns = detectors.stream()
            .flatMap(detector -> detector.detect(currentCandle, previousCandles).stream())
            .collect(Collectors.toList());

        // Sort by confidence descending
        List<PatternDto> sortedPatterns = detectedPatterns.stream()
            .sorted((p1, p2) -> Double.compare(p2.getConfidence(), p1.getConfidence()))
            .collect(Collectors.toList());

        if (!sortedPatterns.isEmpty()) {
            logger.info("Detected {} patterns for symbol={}: {}", 
                       sortedPatterns.size(),
                       currentCandle.getSymbol(),
                       sortedPatterns.stream()
                           .map(p -> p.getName() + "(" + String.format("%.0f%%", p.getConfidence() * 100) + ")")
                           .collect(Collectors.joining(", ")));
        } else {
            logger.debug("No patterns detected for symbol={}", currentCandle.getSymbol());
        }

        return sortedPatterns;
    }
}
