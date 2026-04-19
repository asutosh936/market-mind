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
 * Detector for Shooting Star pattern - bearish reversal signal.
 * Upper wick ≥ 2× body; lower wick minimal; body in bottom 30%.
 */
@Component
public class ShootingStarDetector implements PatternDetector {
    private static final Logger logger = LoggerFactory.getLogger(ShootingStarDetector.class);
    private static final double UPPER_WICK_MULTIPLIER = 2.0;
    private static final double LOWER_WICK_THRESHOLD = 0.10; // 10% of range
    private static final double BODY_POSITION_THRESHOLD = 0.30; // Bottom 30%
    private static final double CONFIDENCE = 0.82;

    @Override
    public Optional<PatternDto> detect(MarketData currentCandle, List<MarketData> previousCandles) {
        try {
            double totalRange = currentCandle.getHighPrice() - currentCandle.getLowPrice();
            if (totalRange == 0) return Optional.empty();

            double bodySize = Math.abs(currentCandle.getClosePrice() - currentCandle.getOpenPrice());
            double bodyOpen = Math.max(currentCandle.getOpenPrice(), currentCandle.getClosePrice());
            double bodyLow = Math.min(currentCandle.getOpenPrice(), currentCandle.getClosePrice());

            // Upper wick (distance from high to body)
            double upperWick = currentCandle.getHighPrice() - bodyOpen;
            
            // Lower wick (distance from body to low)
            double lowerWick = bodyLow - currentCandle.getLowPrice();
            
            // Body position from bottom (lower = body at bottom)
            double bodyPositionFromBottom = (bodyLow - currentCandle.getLowPrice()) / totalRange;
            
            // Check conditions
            boolean hasLongUpperWick = upperWick >= (UPPER_WICK_MULTIPLIER * bodySize);
            boolean hasSmallLowerWick = lowerWick < (LOWER_WICK_THRESHOLD * totalRange);
            boolean bodyInBottomPosition = bodyPositionFromBottom < BODY_POSITION_THRESHOLD;

            if (hasLongUpperWick && hasSmallLowerWick && bodyInBottomPosition) {
                String description = String.format(
                    "Shooting Star detected: Upper wick=%.2f (%.1fx body), Lower wick=%.2f%% of range, Body at %.0f%% from bottom",
                    upperWick, upperWick / Math.max(bodySize, 0.01), (lowerWick / totalRange) * 100, bodyPositionFromBottom * 100
                );
                logger.debug("Pattern detected: {}", description);
                return Optional.of(new PatternDto(
                    PatternType.SHOOTING_STAR.getDisplayName(),
                    PatternType.SHOOTING_STAR,
                    CONFIDENCE,
                    description
                ));
            }
        } catch (Exception e) {
            logger.error("Error detecting Shooting Star pattern", e);
        }
        return Optional.empty();
    }
}
