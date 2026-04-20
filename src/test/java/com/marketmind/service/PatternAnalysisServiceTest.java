package com.marketmind.service;

import com.marketmind.model.MarketData;
import com.marketmind.model.dto.PatternDto;
import com.marketmind.model.enums.PatternType;
import com.marketmind.service.detector.DojiDetector;
import com.marketmind.service.detector.HammerDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PatternAnalysisServiceTest {

    @Mock
    private DojiDetector dojiDetector;

    @Mock
    private HammerDetector hammerDetector;

    private PatternAnalysisService patternAnalysisService;
    private MarketData currentMarketData;
    private List<MarketData> historicalData;

    @BeforeEach
    public void setUp() {
        patternAnalysisService = new PatternAnalysisService(Arrays.asList(dojiDetector, hammerDetector));
        currentMarketData = new MarketData("AAPL", LocalDateTime.now(), 100.0, 105.0, 95.0, 103.0, 1000000);
        historicalData = Arrays.asList(
            new MarketData("AAPL", LocalDateTime.now().minusDays(2), 98.0, 102.0, 97.0, 100.0, 900000),
            new MarketData("AAPL", LocalDateTime.now().minusDays(1), 100.0, 104.0, 99.0, 102.0, 950000)
        );
    }

    @Test
    public void testDetectPatterns_ReturnsEmptyWhenNoDetectionOccurs() {
        when(dojiDetector.detect(any(), any())).thenReturn(Optional.empty());
        when(hammerDetector.detect(any(), any())).thenReturn(Optional.empty());

        List<PatternDto> patterns = patternAnalysisService.detectPatterns(currentMarketData, historicalData);

        assertNotNull(patterns);
        assertTrue(patterns.isEmpty());
    }

    @Test
    public void testDetectPatterns_ReturnsDetectedPatterns() {
        PatternDto dojiPattern = new PatternDto("Doji", PatternType.DOJI, 0.85, "Neutral reversal");
        PatternDto hammerPattern = new PatternDto("Hammer", PatternType.HAMMER, 0.75, "Bullish reversal");

        when(dojiDetector.detect(any(), any())).thenReturn(Optional.of(dojiPattern));
        when(hammerDetector.detect(any(), any())).thenReturn(Optional.of(hammerPattern));

        List<PatternDto> patterns = patternAnalysisService.detectPatterns(currentMarketData, historicalData);

        assertEquals(2, patterns.size());
        assertEquals(0.85, patterns.get(0).getConfidence());
        assertEquals(0.75, patterns.get(1).getConfidence());
    }

    @Test
    public void testDetectPatterns_CallsAllDetectors() {
        when(dojiDetector.detect(any(), any())).thenReturn(Optional.empty());
        when(hammerDetector.detect(any(), any())).thenReturn(Optional.empty());

        patternAnalysisService.detectPatterns(currentMarketData, historicalData);

        verify(dojiDetector, times(1)).detect(any(), any());
        verify(hammerDetector, times(1)).detect(any(), any());
    }

    @Test
    public void testDetectPatterns_WithNullHistoricalData() {
        when(dojiDetector.detect(any(), isNull())).thenReturn(Optional.empty());
        when(hammerDetector.detect(any(), isNull())).thenReturn(Optional.empty());

        List<PatternDto> patterns = patternAnalysisService.detectPatterns(currentMarketData, null);

        assertNotNull(patterns);
        assertTrue(patterns.isEmpty());
    }
}
