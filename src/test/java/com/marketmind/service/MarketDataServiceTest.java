package com.marketmind.service;

import com.marketmind.model.MarketData;
import com.marketmind.model.dto.MarketDataWithPatternsDto;
import com.marketmind.model.dto.PatternDto;
import com.marketmind.model.dto.TradingSignalDto;
import com.marketmind.model.enums.PatternType;
import com.marketmind.repository.MarketDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
public class MarketDataServiceTest {

    @Autowired
    private MarketDataService service;

    @Autowired
    private MarketDataRepository repository;

    @MockBean
    private PatternAnalysisService patternAnalysisService;

    @MockBean
    private TradingSignalAIService tradingSignalAIService;

    @BeforeEach
    public void setup() {
        repository.deleteAll(); // Clean up before each test
    }

    @Test
    public void testFindAll_EmptyList() {
        List<MarketData> result = service.findAll();
        assertThat(result).isEmpty();
    }

    @Test
    public void testFindAll_WithData() {
        // Create test data
        MarketData data1 = new MarketData("AAPL", LocalDateTime.now(), 150.00, 155.00, 149.00, 154.50, 1000000);
        MarketData data2 = new MarketData("GOOGL", LocalDateTime.now(), 2800.00, 2850.00, 2790.00, 2840.00, 500000);
        repository.save(data1);
        repository.save(data2);

        List<MarketData> result = service.findAll();
        assertThat(result).hasSize(2);
        assertThat(result).extracting("symbol").contains("AAPL", "GOOGL");
    }

    @Test
    public void testFindById_Existing() {
        MarketData data = new MarketData("MSFT", LocalDateTime.now(), 300.00, 310.00, 295.00, 305.00, 2000000);
        MarketData saved = repository.save(data);

        Optional<MarketData> result = service.findById(saved.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo("MSFT");
        assertThat(result.get().getClosePrice()).isEqualTo(305.00);
    }

    @Test
    public void testFindById_NonExisting() {
        Optional<MarketData> result = service.findById(999L);
        assertThat(result).isNotPresent();
    }

    @Test
    public void testSave_NewEntity() {
        MarketData data = new MarketData("TSLA", LocalDateTime.now(), 200.00, 210.00, 195.00, 205.00, 1500000);
        MarketData saved = service.save(data);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSymbol()).isEqualTo("TSLA");
        assertThat(saved.getOpenPrice()).isEqualTo(200.00);

        // Verify it's in the database
        Optional<MarketData> retrieved = repository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getSymbol()).isEqualTo("TSLA");
    }

    @Test
    public void testSave_UpdateExisting() {
        MarketData data = new MarketData("AMZN", LocalDateTime.now(), 3300.00, 3350.00, 3280.00, 3330.00, 3000000);
        MarketData saved = repository.save(data);

        // Update the data
        saved.setClosePrice(3400.00);
        saved.setVolume(3500000);
        MarketData updated = service.save(saved);

        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getClosePrice()).isEqualTo(3400.00);
        assertThat(updated.getVolume()).isEqualTo(3500000);
    }

    @Test
    public void testDeleteById_Existing() {
        MarketData data = new MarketData("NVDA", LocalDateTime.now(), 400.00, 420.00, 395.00, 415.00, 800000);
        MarketData saved = repository.save(data);

        service.deleteById(saved.getId());

        Optional<MarketData> result = repository.findById(saved.getId());
        assertThat(result).isNotPresent();
    }

    @Test
    public void testDeleteById_NonExisting() {
        // Should not throw exception
        service.deleteById(999L);

        // Verify no data was affected
        List<MarketData> all = service.findAll();
        assertThat(all).isEmpty();
    }

    @Test
    public void testFindBySymbol() {
        repository.save(new MarketData("CRM", LocalDateTime.parse("2026-04-18T09:00:00"), 250.00, 255.00, 248.00, 252.00, 400000));
        repository.save(new MarketData("CRM", LocalDateTime.parse("2026-04-18T10:00:00"), 252.00, 258.00, 251.00, 256.00, 420000));
        repository.save(new MarketData("ORCL", LocalDateTime.parse("2026-04-18T10:00:00"), 80.00, 83.00, 79.00, 82.00, 300000));

        List<MarketData> results = service.findBySymbol("CRM");
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTimestamp()).isAfterOrEqualTo(results.get(1).getTimestamp());
    }

    @Test
    public void testFindBySymbolAndRange() {
        repository.save(new MarketData("CRM", LocalDateTime.parse("2026-04-18T09:00:00"), 250.00, 255.00, 248.00, 252.00, 400000));
        repository.save(new MarketData("CRM", LocalDateTime.parse("2026-04-18T10:00:00"), 252.00, 258.00, 251.00, 256.00, 420000));
        repository.save(new MarketData("CRM", LocalDateTime.parse("2026-04-18T11:00:00"), 256.00, 260.00, 255.00, 259.00, 430000));

        List<MarketData> results = service.findBySymbolAndRange(
                "CRM",
                LocalDateTime.parse("2026-04-18T09:30:00"),
                LocalDateTime.parse("2026-04-18T10:30:00")
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTimestamp()).isEqualTo(LocalDateTime.parse("2026-04-18T10:00:00"));
    }

    @Test
    public void testGetWithPatterns_UsesAiTradingSignalAndTrendComputation() {
        MarketData previous1 = new MarketData("AAPL", LocalDateTime.parse("2026-04-18T09:00:00"), 145.00, 147.00, 144.00, 146.00, 900000);
        MarketData previous2 = new MarketData("AAPL", LocalDateTime.parse("2026-04-18T09:30:00"), 146.00, 148.00, 145.00, 147.00, 910000);
        MarketData current = new MarketData("AAPL", LocalDateTime.parse("2026-04-18T10:00:00"), 147.00, 149.00, 146.00, 148.50, 950000);

        repository.save(previous1);
        repository.save(previous2);
        repository.save(current);

        PatternDto detectedPattern = new PatternDto("Hammer", PatternType.HAMMER, 0.92, "Bullish reversal after pullback.");
        when(patternAnalysisService.detectPatterns(eq(current), anyList())).thenReturn(List.of(detectedPattern));
        when(tradingSignalAIService.analyzePatternsForTradingSignal(eq(current), org.mockito.ArgumentMatchers.anyString(), anyList()))
                .thenReturn(new TradingSignalAIService.TradingSignalAnalysis("BUY", 85.0, "AI confirms bullish setup.", "Watch strong resistance at the next high."));

        MarketDataWithPatternsDto result = service.getWithPatterns(current, List.of(previous1, previous2, current));

        assertThat(result.getDetectedPatterns()).hasSize(1);
        assertThat(result.getDetectedPatterns().get(0).getName()).isEqualTo("Hammer");
        assertThat(result.getTradingSignal()).isNotNull();
        assertThat(result.getTradingSignal().getSignal()).isEqualTo("BUY");
        assertThat(result.getTradingSignal().getRiskNote()).contains("resistance");
    }

    @Test
    public void testGetWithPatterns_InsufficientHistoryUsesInsufficientTrendData() {
        MarketData current = new MarketData("TSLA", LocalDateTime.parse("2026-04-18T10:00:00"), 195.00, 205.00, 190.00, 200.00, 1200000);

        when(patternAnalysisService.detectPatterns(eq(current), anyList())).thenReturn(List.of());
        when(tradingSignalAIService.analyzePatternsForTradingSignal(any(MarketData.class), eq("Insufficient trend data"), anyList()))
                .thenReturn(new TradingSignalAIService.TradingSignalAnalysis("HOLD", 50.0, "No clear pattern detected.", "Insufficient history limits signal interpretation."));

        MarketDataWithPatternsDto result = service.getWithPatterns(current, List.of(current));

        assertThat(result.getDetectedPatterns()).isEmpty();
        assertThat(result.getTradingSignal().getSignal()).isEqualTo("HOLD");
        assertThat(result.getTradingSignal().getReasoning()).contains("No clear pattern");
    }

    @Test
    public void testGetWithPatterns_DowntrendTrend() {
        MarketData previous1 = new MarketData("AAPL", LocalDateTime.parse("2026-04-18T09:00:00"), 155.00, 157.00, 154.00, 155.00, 900000);
        MarketData previous2 = new MarketData("AAPL", LocalDateTime.parse("2026-04-18T09:30:00"), 152.00, 154.00, 151.00, 152.00, 910000);
        MarketData current = new MarketData("AAPL", LocalDateTime.parse("2026-04-18T10:00:00"), 152.00, 154.00, 151.00, 149.00, 950000);

        when(patternAnalysisService.detectPatterns(eq(current), anyList())).thenReturn(List.of());
        when(tradingSignalAIService.analyzePatternsForTradingSignal(any(MarketData.class), anyString(), anyList()))
                .thenReturn(new TradingSignalAIService.TradingSignalAnalysis("SELL", 70.0, "Downward pressure confirmed.", "Trend may reverse on strong support."));

        MarketDataWithPatternsDto result = service.getWithPatterns(current, List.of(previous1, previous2, current));

        assertThat(result.getTradingSignal().getSignal()).isEqualTo("SELL");
        assertThat(result.getTradingSignal().getReasoning()).contains("Downward pressure");
    }

    @Test
    public void testGetWithPatterns_SidewaysTrend() {
        MarketData previous1 = new MarketData("AAPL", LocalDateTime.parse("2026-04-18T09:00:00"), 150.00, 152.00, 149.00, 150.50, 900000);
        MarketData previous2 = new MarketData("AAPL", LocalDateTime.parse("2026-04-18T09:30:00"), 150.50, 152.50, 150.00, 151.00, 910000);
        MarketData current = new MarketData("AAPL", LocalDateTime.parse("2026-04-18T10:00:00"), 151.00, 153.00, 150.50, 151.25, 950000);

        when(patternAnalysisService.detectPatterns(eq(current), anyList())).thenReturn(List.of());
        when(tradingSignalAIService.analyzePatternsForTradingSignal(any(MarketData.class), contains("Sideways"), anyList()))
                .thenReturn(new TradingSignalAIService.TradingSignalAnalysis("HOLD", 55.0, "Range-bound action continues.", "Watch for breakout or breakdown."));

        MarketDataWithPatternsDto result = service.getWithPatterns(current, List.of(previous1, previous2, current));

        assertThat(result.getTradingSignal().getSignal()).isEqualTo("HOLD");
        assertThat(result.getTradingSignal().getReasoning()).contains("Range-bound");
    }

    @Test
    public void testFindBySymbolWithPatterns_ReturnsEmptyWhenNoData() {
        Optional<MarketDataWithPatternsDto> result = service.findBySymbolWithPatterns("UNKNOWN");

        assertThat(result).isNotPresent();
    }

    @Test
    public void testFindBySymbolWithPatterns_ReturnsLatestEntry() {
        MarketData older = new MarketData("AMZN", LocalDateTime.parse("2026-04-18T09:00:00"), 3300.00, 3350.00, 3280.00, 3330.00, 3000000);
        MarketData latest = new MarketData("AMZN", LocalDateTime.parse("2026-04-18T10:00:00"), 3330.00, 3380.00, 3320.00, 3375.00, 3100000);

        repository.save(older);
        repository.save(latest);

        // Mock pattern analysis and AI service
        when(patternAnalysisService.detectPatterns(any(MarketData.class), anyList())).thenReturn(List.of());
        when(tradingSignalAIService.analyzePatternsForTradingSignal(any(MarketData.class), anyString(), anyList()))
                .thenReturn(new TradingSignalAIService.TradingSignalAnalysis("HOLD", 50.0, "No patterns detected.", "Market requires more context."));

        Optional<MarketDataWithPatternsDto> result = service.findBySymbolWithPatterns("AMZN");

        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo("AMZN");
        assertThat(result.get().getTimestamp()).isEqualTo(latest.getTimestamp());
        assertThat(result.get().getTradingSignal()).isNotNull();
    }
}
