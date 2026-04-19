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
 * Detector for Evening Star pattern - strong bearish reversal signal.
 * 3-candle: large bullish → small-body → large bearish below midpoint.
 */
@Component
public class EveningStarDetector implements PatternDetector {
    private static final Logger logger = LoggerFactory.getLogger(EveningStarDetector.class);
    private static final double CONFIDENCE = 0.85;

    @Override
    public Optional<PatternDto> detect(MarketData currentCandle, List<MarketData> previousCandles) {
        try {
            // Need at least 2 previous candles (3 candles total)
            if (previousCandles == null || previousCandles.size() < 2) {
                return Optional.empty();
            }

            MarketData firstCandle = previousCandles.get(previousCandles.size() - 2);
            MarketData secondCandle = previousCandles.get(previousCandles.size() - 1);

            // First candle must be large and bullish
            boolean firstIsBullish = firstCandle.getClosePrice() > firstCandle.getOpenPrice();
            double firstBodySize = Math.abs(firstCandle.getOpenPrice() - firstCandle.getClosePrice());
            double firstRange = firstCandle.getHighPrice() - firstCandle.getLowPrice();
            if (firstRange == 0) return Optional.empty();
            boolean firstIsLarge = (firstBodySize / firstRange) > 0.50; // At least 50% of range
            
            if (!(firstIsBullish && firstIsLarge)) return Optional.empty();

            // Second candle must have small body (star)
            double secondBodySize = Math.abs(secondCandle.getOpenPrice() - secondCandle.getClosePrice());
            double secondRange = secondCandle.getHighPrice() - secondCandle.getLowPrice();
            if (secondRange == 0) return Optional.empty();
            boolean secondIsSmallBody = (secondBodySize / secondRange) < 0.30; // Less than 30% of range

            if (!secondIsSmallBody) return Optional.empty();

            // Third candle (current) must be large and bearish, closing below midpoint of first candle
            boolean thirdIsBearish = currentCandle.getClosePrice() < currentCandle.getOpenPrice();
            double thirdBodySize = Math.abs(currentCandle.getOpenPrice() - currentCandle.getClosePrice());
            double thirdRange = currentCandle.getHighPrice() - currentCandle.getLowPrice();
            if (thirdRange == 0) return Optional.empty();
            boolean thirdIsLarge = (thirdBodySize / thirdRange) > 0.50;

            double firstMidpoint = (firstCandle.getHighPrice() + firstCandle.getLowPrice()) / 2.0;
            boolean closesBelowFirstMidpoint = currentCandle.getClosePrice() < firstMidpoint;

            if (thirdIsBearish && thirdIsLarge && closesBelowFirstMidpoint) {
                String description = String.format(
                    "Evening Star detected: Bullish (%.2f-%.2f) → Small body (%.2f-%.2f) → Bearish (%.2f-%.2f) closes below %.2f",
                    firstCandle.getOpenPrice(), firstCandle.getClosePrice(),
                    secondCandle.getOpenPrice(), secondCandle.getClosePrice(),
                    currentCandle.getOpenPrice(), currentCandle.getClosePrice(),
                    firstMidpoint
                );
                logger.debug("Pattern detected: {}", description);
                return Optional.of(new PatternDto(
                    PatternType.EVENING_STAR.getDisplayName(),
                    PatternType.EVENING_STAR,
                    CONFIDENCE,
                    description
                ));
            }
        } catch (Exception e) {
            logger.error("Error detecting Evening Star pattern", e);
        }
        return Optional.empty();
    }
}
