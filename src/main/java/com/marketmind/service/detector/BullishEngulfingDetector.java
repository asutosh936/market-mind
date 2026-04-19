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
 * Detector for Bullish Engulfing pattern - strong bullish reversal signal.
 * Current bullish candle fully covers prior bearish candle body.
 */
@Component
public class BullishEngulfingDetector implements PatternDetector {
    private static final Logger logger = LoggerFactory.getLogger(BullishEngulfingDetector.class);
    private static final double CONFIDENCE = 0.88;

    @Override
    public Optional<PatternDto> detect(MarketData currentCandle, List<MarketData> previousCandles) {
        try {
            // Need at least one previous candle
            if (previousCandles == null || previousCandles.isEmpty()) {
                return Optional.empty();
            }

            MarketData previousCandle = previousCandles.get(previousCandles.size() - 1);

            // Current candle must be bullish (close > open)
            boolean currentIsBullish = currentCandle.getClosePrice() > currentCandle.getOpenPrice();
            if (!currentIsBullish) return Optional.empty();

            // Previous candle must be bearish (close < open)
            boolean previousIsBearish = previousCandle.getClosePrice() < previousCandle.getOpenPrice();
            if (!previousIsBearish) return Optional.empty();

            // Current candle's open must be below or equal to previous close
            double prevBodyLow = Math.min(previousCandle.getOpenPrice(), previousCandle.getClosePrice());
            double prevBodyHigh = Math.max(previousCandle.getOpenPrice(), previousCandle.getClosePrice());

            boolean currentOpenBelowPrevClose = currentCandle.getOpenPrice() <= prevBodyHigh;
            boolean currentClosesAbovePrevOpen = currentCandle.getClosePrice() >= prevBodyLow;

            if (currentOpenBelowPrevClose && currentClosesAbovePrevOpen) {
                String description = String.format(
                    "Bullish Engulfing detected: Current candle body (%.2f-%.2f) engulfs previous body (%.2f-%.2f)",
                    currentCandle.getOpenPrice(), currentCandle.getClosePrice(),
                    prevBodyLow, prevBodyHigh
                );
                logger.debug("Pattern detected: {}", description);
                return Optional.of(new PatternDto(
                    PatternType.BULLISH_ENGULFING.getDisplayName(),
                    PatternType.BULLISH_ENGULFING,
                    CONFIDENCE,
                    description
                ));
            }
        } catch (Exception e) {
            logger.error("Error detecting Bullish Engulfing pattern", e);
        }
        return Optional.empty();
    }
}
