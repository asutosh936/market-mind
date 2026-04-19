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
 * Detector for Hammer pattern - bullish reversal signal.
 * Lower wick ≥ 2× body; upper wick < 10% range; body in top 30%.
 */
@Component
public class HammerDetector implements PatternDetector {
    private static final Logger logger = LoggerFactory.getLogger(HammerDetector.class);
    private static final double LOWER_WICK_MULTIPLIER = 2.0;
    private static final double UPPER_WICK_THRESHOLD = 0.10; // 10% of range
    private static final double BODY_POSITION_THRESHOLD = 0.30; // Top 30%
    private static final double CONFIDENCE = 0.82;

    @Override
    public Optional<PatternDto> detect(MarketData currentCandle, List<MarketData> previousCandles) {
        try {
            double totalRange = currentCandle.getHighPrice() - currentCandle.getLowPrice();
            if (totalRange == 0) return Optional.empty();

            double bodySize = Math.abs(currentCandle.getClosePrice() - currentCandle.getOpenPrice());
            double bodyOpen = Math.max(currentCandle.getOpenPrice(), currentCandle.getClosePrice());
            double bodyLow = Math.min(currentCandle.getOpenPrice(), currentCandle.getClosePrice());

            // Lower wick (distance from body to low)
            double lowerWick = bodyLow - currentCandle.getLowPrice();
            
            // Upper wick (distance from high to body)
            double upperWick = currentCandle.getHighPrice() - bodyOpen;
            
            // Body position from bottom (higher = body at top)
            double bodyPositionFromBottom = (bodyLow - currentCandle.getLowPrice()) / totalRange;
            
            // Check conditions
            boolean hasLongLowerWick = lowerWick >= (LOWER_WICK_MULTIPLIER * bodySize);
            boolean hasSmallUpperWick = upperWick < (UPPER_WICK_THRESHOLD * totalRange);
            boolean bodyInTopPosition = bodyPositionFromBottom > (1.0 - BODY_POSITION_THRESHOLD);

            if (hasLongLowerWick && hasSmallUpperWick && bodyInTopPosition) {
                String description = String.format(
                    "Hammer detected: Lower wick=%.2f (%.1fx body), Upper wick=%.2f%% of range, Body at %.0f%% from bottom",
                    lowerWick, lowerWick / Math.max(bodySize, 0.01), (upperWick / totalRange) * 100, bodyPositionFromBottom * 100
                );
                logger.debug("Pattern detected: {}", description);
                return Optional.of(new PatternDto(
                    PatternType.HAMMER.getDisplayName(),
                    PatternType.HAMMER,
                    CONFIDENCE,
                    description
                ));
            }
        } catch (Exception e) {
            logger.error("Error detecting Hammer pattern", e);
        }
        return Optional.empty();
    }
}
