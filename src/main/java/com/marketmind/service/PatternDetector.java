package com.marketmind.service;

import com.marketmind.model.dto.PatternDto;
import com.marketmind.model.MarketData;
import java.util.List;
import java.util.Optional;

/**
 * Interface for candlestick pattern detection.
 * Each detector implementation checks for a specific pattern type.
 */
public interface PatternDetector {
    /**
     * Detect a candlestick pattern in the current candle with historical context.
     * 
     * @param currentCandle The current candlestick data to analyze
     * @param previousCandles List of previous candles for multi-candle pattern analysis (typically last 5)
     * @return Optional containing the detected pattern, or empty if pattern not detected
     */
    Optional<PatternDto> detect(MarketData currentCandle, List<MarketData> previousCandles);
}
