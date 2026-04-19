package com.marketmind.service.detector;

import com.marketmind.model.dto.PatternDto;
import com.marketmind.model.enums.PatternType;
import com.marketmind.model.MarketData;
import com.marketmind.service.PatternDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

/**
 * Detector for Bearish Engulfing pattern - strong bearish reversal signal.
 * Current bearish candle fully covers prior bullish candle body.
 */
@Component
public class BearishEngulfingDetector implements PatternDetector {
    private static final Logger logger = LoggerFactory.getLogger(BearishEngulfingDetector.class);
    private static final double CONFIDENCE = 0.88;

    @Override
    public Optional<PatternDto> detect(MarketData currentCandle, List<MarketData> previousCandles) {
        try {
            // Need at least one previous candle
            if (previousCandles == null || previousCandles.isEmpty()) {
                return Optional.empty();
            }

            MarketData previousCandle = previousCandles.get(previousCandles.size() - 1);

            // Current candle must be bearish (close < open)
            boolean currentIsBearish = currentCandle.getClosePrice() < currentCandle.getOpenPrice();
            if (!currentIsBearish) return Optional.empty();

            // Previous candle must be bullish (close > open)
            boolean previousIsBullish = previousCandle.getClosePrice() > previousCandle.getOpenPrice();
            if (!previousIsBullish) return Optional.empty();

            // Current candle's open must be above or equal to previous close
            double prevBodyLow = Math.min(previousCandle.getOpenPrice(), previousCandle.getClosePrice());
            double prevBodyHigh = Math.max(previousCandle.getOpenPrice(), previousCandle.getClosePrice());

            boolean currentOpenAbovePrevClose = currentCandle.getOpenPrice() >= prevBodyLow;
            boolean currentClosesBelowPrevOpen = currentCandle.getClosePrice() <= prevBodyHigh;

            if (currentOpenAbovePrevClose && currentClosesBelowPrevOpen) {
                String description = String.format(
                    "Bearish Engulfing detected: Current candle body (%.2f-%.2f) engulfs previous body (%.2f-%.2f)",
                    currentCandle.getOpenPrice(), currentCandle.getClosePrice(),
                    prevBodyLow, prevBodyHigh
                );
                logger.debug("Pattern detected: {}", description);
                return Optional.of(new PatternDto(
                    PatternType.BEARISH_ENGULFING.getDisplayName(),
                    PatternType.BEARISH_ENGULFING,
                    CONFIDENCE,
                    description
                ));
            }
        } catch (Exception e) {
            logger.error("Error detecting Bearish Engulfing pattern", e);
        }
        return Optional.empty();
    }
}
