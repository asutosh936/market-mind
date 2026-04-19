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
 * Detector for Doji pattern - neutral reversal signal.
 * A Doji has a body < 5% of the total range (open and close are very close).
 */
@Component
public class DojiDetector implements PatternDetector {
    private static final Logger logger = LoggerFactory.getLogger(DojiDetector.class);
    private static final double BODY_THRESHOLD = 0.05; // 5% of range
    private static final double CONFIDENCE = 0.85;

    @Override
    public Optional<PatternDto> detect(MarketData currentCandle, List<MarketData> previousCandles) {
        try {
            double totalRange = currentCandle.getHighPrice() - currentCandle.getLowPrice();
            if (totalRange == 0) return Optional.empty();

            double bodySize = Math.abs(currentCandle.getClosePrice() - currentCandle.getOpenPrice());
            double bodyRatio = bodySize / totalRange;

            if (bodyRatio < BODY_THRESHOLD) {
                String description = String.format(
                    "Doji detected: Body is %.2f%% of range (threshold: %.0f%%)",
                    bodyRatio * 100, BODY_THRESHOLD * 100
                );
                logger.debug("Pattern detected: {}", description);
                return Optional.of(new PatternDto(
                    PatternType.DOJI.getDisplayName(),
                    PatternType.DOJI,
                    CONFIDENCE,
                    description
                ));
            }
        } catch (Exception e) {
            logger.error("Error detecting Doji pattern", e);
        }
        return Optional.empty();
    }
}
