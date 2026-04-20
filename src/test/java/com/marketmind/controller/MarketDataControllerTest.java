package com.marketmind.controller;

import com.marketmind.model.MarketData;
import com.marketmind.repository.MarketDataRepository;
import com.marketmind.integration.service.AlphaVantageService;
import com.marketmind.service.MarketDataService;
import com.marketmind.service.PatternAnalysisService;
import com.marketmind.service.TradingSignalAIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
public class MarketDataControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MarketDataRepository repository;

    @MockBean
    private TradingSignalAIService tradingSignalAIService;

    @MockBean
    private PatternAnalysisService patternAnalysisService;

    @MockBean
    private AlphaVantageService alphaVantageService;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        repository.deleteAll(); // Clean up before each test
    }

    @Test
    public void testGetAllMarketData_EmptyList() throws Exception {
        mockMvc.perform(get("/api/marketdata"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("No data found")))
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.count", is(0)));
    }

    @Test
    public void testGetAllMarketData_ExcludePatterns() throws Exception {
        MarketData data = new MarketData("NFLX", LocalDateTime.parse("2026-04-18T09:00:00"), 500.00, 510.00, 495.00, 505.00, 700000);
        repository.save(data);

        mockMvc.perform(get("/api/marketdata").param("includePatterns", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.count", is(1)))
                .andExpect(jsonPath("$.data[0].symbol", is("NFLX")))
                .andExpect(jsonPath("$.data[0].detectedPatterns", hasSize(0)));
    }

    @Test
    public void testCreateMarketData() throws Exception {
        String marketDataJson = """
            {
                "symbol": "AAPL",
                "timestamp": "2026-04-18T10:00:00",
                "openPrice": 150.00,
                "highPrice": 155.00,
                "lowPrice": 149.00,
                "closePrice": 154.50,
                "volume": 1000000
            }
            """;

        mockMvc.perform(post("/api/marketdata")
                .contentType(MediaType.APPLICATION_JSON)
                .content(marketDataJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.symbol", is("AAPL")))
                .andExpect(jsonPath("$.openPrice", is(150.00)))
                .andExpect(jsonPath("$.closePrice", is(154.50)));
    }

    @Test
    public void testGetMarketDataById() throws Exception {
        // First create a market data entry
        MarketData marketData = new MarketData("GOOGL", LocalDateTime.now(), 2800.00, 2850.00, 2790.00, 2840.00, 500000);
        MarketData saved = repository.save(marketData);

        // Mock the AI service for pattern analysis
        when(tradingSignalAIService.analyzePatternsForTradingSignal(any(MarketData.class), anyString(), anyList()))
                .thenReturn(new TradingSignalAIService.TradingSignalAnalysis("HOLD", 50.0, "Test analysis.", "Test risk."));

        mockMvc.perform(get("/api/marketdata/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.symbol", is("GOOGL")))
                .andExpect(jsonPath("$.closePrice", is(2840.00)));
    }

    @Test
    public void testGetMarketDataById_NotFound() throws Exception {
        mockMvc.perform(get("/api/marketdata/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateMarketData() throws Exception {
        // First create a market data entry
        MarketData marketData = new MarketData("MSFT", LocalDateTime.now(), 300.00, 310.00, 295.00, 305.00, 2000000);
        MarketData saved = repository.save(marketData);

        String updatedDataJson = """
            {
                "symbol": "MSFT",
                "timestamp": "2026-04-18T11:00:00",
                "openPrice": 300.00,
                "highPrice": 315.00,
                "lowPrice": 295.00,
                "closePrice": 310.00,
                "volume": 2500000
            }
            """;

        mockMvc.perform(put("/api/marketdata/" + saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedDataJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.symbol", is("MSFT")))
                .andExpect(jsonPath("$.closePrice", is(310.00)))
                .andExpect(jsonPath("$.volume", is(2500000)));
    }

    @Test
    public void testUpdateMarketData_NotFound() throws Exception {
        String updatedDataJson = """
            {
                "symbol": "TSLA",
                "timestamp": "2026-04-18T12:00:00",
                "openPrice": 200.00,
                "highPrice": 210.00,
                "lowPrice": 195.00,
                "closePrice": 205.00,
                "volume": 1500000
            }
            """;

        mockMvc.perform(put("/api/marketdata/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedDataJson))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteMarketData() throws Exception {
        // First create a market data entry
        MarketData marketData = new MarketData("AMZN", LocalDateTime.now(), 3300.00, 3350.00, 3280.00, 3330.00, 3000000);
        MarketData saved = repository.save(marketData);

        mockMvc.perform(delete("/api/marketdata/" + saved.getId()))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/marketdata/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteMarketData_NotFound() throws Exception {
        mockMvc.perform(delete("/api/marketdata/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetMarketDataBySymbol() throws Exception {
        repository.save(new MarketData("NFLX", LocalDateTime.parse("2026-04-18T09:00:00"), 500.00, 510.00, 495.00, 505.00, 700000));
        repository.save(new MarketData("NFLX", LocalDateTime.parse("2026-04-18T10:00:00"), 505.00, 515.00, 500.00, 510.00, 750000));
        repository.save(new MarketData("AAPL", LocalDateTime.parse("2026-04-18T10:00:00"), 150.00, 155.00, 149.00, 154.50, 1000000));

        // Mock the AI service for pattern analysis
        when(tradingSignalAIService.analyzePatternsForTradingSignal(any(MarketData.class), anyString(), anyList()))
                .thenReturn(new TradingSignalAIService.TradingSignalAnalysis("HOLD", 50.0, "Test analysis.", "Test risk."));

        mockMvc.perform(get("/api/marketdata/symbol/NFLX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("NFLX")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    public void testGetAllMarketData_IncludePatterns() throws Exception {
        MarketData data = new MarketData("IBM", LocalDateTime.parse("2026-04-18T09:00:00"), 120.00, 125.00, 118.00, 123.00, 450000);
        repository.save(data);

        when(patternAnalysisService.detectPatterns(any(MarketData.class), anyList())).thenReturn(List.of());
        when(tradingSignalAIService.analyzePatternsForTradingSignal(any(MarketData.class), anyString(), anyList()))
                .thenReturn(new TradingSignalAIService.TradingSignalAnalysis("HOLD", 50.0, "Test analysis.", "Test risk."));

        mockMvc.perform(get("/api/marketdata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(1)))
                .andExpect(jsonPath("$.data[0].symbol", is("IBM")))
                .andExpect(jsonPath("$.data[0].detectedPatterns", hasSize(0)));
    }

    @Test
    public void testGetMarketDataById_ExcludePatterns() throws Exception {
        MarketData marketData = new MarketData("NFLX", LocalDateTime.parse("2026-04-18T09:00:00"), 500.00, 510.00, 495.00, 505.00, 700000);
        MarketData saved = repository.save(marketData);

        mockMvc.perform(get("/api/marketdata/" + saved.getId()).param("includePatterns", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("NFLX")))
                .andExpect(jsonPath("$.detectedPatterns", hasSize(0)));
    }

    @Test
    public void testGetMarketDataBySymbol_NoCachedData_FallsBackToAlphaVantage() throws Exception {
        when(alphaVantageService.getQuote("TSLA")).thenReturn(new MarketData("TSLA", LocalDateTime.parse("2026-04-18T10:00:00"), 200.00, 210.00, 195.00, 205.00, 800000));
        when(alphaVantageService.getIntraDayHistory("TSLA")).thenReturn(List.of(new MarketData("TSLA", LocalDateTime.parse("2026-04-18T09:00:00"), 198.00, 205.00, 196.00, 200.00, 700000)));
        when(patternAnalysisService.detectPatterns(any(MarketData.class), anyList())).thenReturn(List.of());
        when(tradingSignalAIService.analyzePatternsForTradingSignal(any(MarketData.class), anyString(), anyList()))
                .thenReturn(new TradingSignalAIService.TradingSignalAnalysis("HOLD", 50.0, "Test analysis.", "Test risk."));

        mockMvc.perform(get("/api/marketdata/symbol/TSLA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("TSLA")))
                .andExpect(jsonPath("$.detectedPatterns", hasSize(0)));
    }

    @Test
    public void testGetMarketDataBySymbol_NoDataReturnsNotFound() throws Exception {
        when(alphaVantageService.getQuote("UNKNOWN")).thenReturn(null);

        mockMvc.perform(get("/api/marketdata/symbol/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetMarketDataBySymbol_ExcludePatterns() throws Exception {
        when(alphaVantageService.getQuote("TSLA")).thenReturn(new MarketData("TSLA", LocalDateTime.parse("2026-04-18T10:00:00"), 200.00, 210.00, 195.00, 205.00, 800000));
        when(alphaVantageService.getIntraDayHistory("TSLA")).thenReturn(List.of(new MarketData("TSLA", LocalDateTime.parse("2026-04-18T09:00:00"), 198.00, 205.00, 196.00, 200.00, 700000)));

        mockMvc.perform(get("/api/marketdata/symbol/TSLA").param("includePatterns", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("TSLA")))
                .andExpect(jsonPath("$.detectedPatterns", hasSize(0)));
    }

    @Test
    public void testGetMarketDataBySymbol_CachedDataExcludePatterns() throws Exception {
        repository.save(new MarketData("IBM", LocalDateTime.parse("2026-04-18T09:00:00"), 120.00, 125.00, 118.00, 123.00, 450000));
        repository.save(new MarketData("IBM", LocalDateTime.parse("2026-04-18T10:00:00"), 123.00, 127.00, 121.00, 126.00, 480000));

        mockMvc.perform(get("/api/marketdata/symbol/IBM").param("includePatterns", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("IBM")))
                .andExpect(jsonPath("$.detectedPatterns", hasSize(0)));
    }

    @Test
    public void testGetMarketDataIntraDay_ReturnsData() throws Exception {
        when(alphaVantageService.getIntraDayLatest("TSLA")).thenReturn(new MarketData("TSLA", LocalDateTime.parse("2026-04-18T10:00:00"), 200.00, 210.00, 195.00, 205.00, 800000));
        when(alphaVantageService.getIntraDayHistory("TSLA")).thenReturn(List.of(new MarketData("TSLA", LocalDateTime.parse("2026-04-18T09:00:00"), 198.00, 205.00, 196.00, 200.00, 700000)));
        when(patternAnalysisService.detectPatterns(any(MarketData.class), anyList())).thenReturn(List.of());
        when(tradingSignalAIService.analyzePatternsForTradingSignal(any(MarketData.class), anyString(), anyList()))
                .thenReturn(new TradingSignalAIService.TradingSignalAnalysis("BUY", 60.0, "Intraday momentum rising.", "Weak volume on the latest candle."));

        mockMvc.perform(get("/api/marketdata/intraday/TSLA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("TSLA")))
                .andExpect(jsonPath("$.detectedPatterns", hasSize(0)));
    }

    @Test
    public void testGetMarketDataDaily_ExcludePatterns() throws Exception {
        when(alphaVantageService.getDailyLatest("IBM")).thenReturn(new MarketData("IBM", LocalDateTime.parse("2026-04-18T10:00:00"), 120.00, 125.00, 118.00, 123.00, 450000));

        mockMvc.perform(get("/api/marketdata/daily/IBM").param("includePatterns", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("IBM")))
                .andExpect(jsonPath("$.detectedPatterns", hasSize(0)));
    }

    @Test
    public void testGetMarketDataBySymbol_NoCachedData_IntraDayHistoryEmptyFallsBackToDailyHistory() throws Exception {
        when(alphaVantageService.getQuote("TSLA")).thenReturn(new MarketData("TSLA", LocalDateTime.parse("2026-04-18T10:00:00"), 200.00, 210.00, 195.00, 205.00, 800000));
        when(alphaVantageService.getIntraDayHistory("TSLA")).thenReturn(List.of());
        when(alphaVantageService.getDailyHistory("TSLA")).thenReturn(List.of(new MarketData("TSLA", LocalDateTime.parse("2026-04-18T09:00:00"), 198.00, 205.00, 196.00, 200.00, 700000)));
        when(patternAnalysisService.detectPatterns(any(MarketData.class), anyList())).thenReturn(List.of());
        when(tradingSignalAIService.analyzePatternsForTradingSignal(any(MarketData.class), anyString(), anyList()))
                .thenReturn(new TradingSignalAIService.TradingSignalAnalysis("HOLD", 50.0, "Fallback daily history used.", "Intraday data was unavailable."));

        mockMvc.perform(get("/api/marketdata/symbol/TSLA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("TSLA")))
                .andExpect(jsonPath("$.detectedPatterns", hasSize(0)));
    }

    @Test
    public void testGetMarketDataIntraDay_NotFound() throws Exception {
        when(alphaVantageService.getIntraDayLatest("NFLX")).thenReturn(null);

        mockMvc.perform(get("/api/marketdata/intraday/NFLX"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetMarketDataDailyHistory_Empty() throws Exception {
        when(alphaVantageService.getDailyHistory("IBM")).thenReturn(List.of());

        mockMvc.perform(get("/api/marketdata/daily/IBM/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("No daily history data available")));
    }

    @Test
    public void testGetMarketDataDailyHistory_ReturnsData() throws Exception {
        when(alphaVantageService.getDailyHistory("IBM")).thenReturn(List.of(
                new MarketData("IBM", LocalDateTime.parse("2026-04-18T09:00:00"), 120.00, 125.00, 118.00, 123.00, 450000)
        ));
        when(patternAnalysisService.detectPatterns(any(MarketData.class), anyList())).thenReturn(List.of());
        when(tradingSignalAIService.analyzePatternsForTradingSignal(any(MarketData.class), anyString(), anyList()))
                .thenReturn(new TradingSignalAIService.TradingSignalAnalysis("HOLD", 50.0, "Test analysis.", "Test risk."));

        mockMvc.perform(get("/api/marketdata/daily/IBM/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    public void testGetMarketDataBySymbolAndRange() throws Exception {
        repository.save(new MarketData("NFLX", LocalDateTime.parse("2026-04-18T09:00:00"), 500.00, 510.00, 495.00, 505.00, 700000));
        repository.save(new MarketData("NFLX", LocalDateTime.parse("2026-04-18T10:00:00"), 505.00, 515.00, 500.00, 510.00, 750000));
        repository.save(new MarketData("NFLX", LocalDateTime.parse("2026-04-18T11:00:00"), 510.00, 520.00, 505.00, 515.00, 780000));

        // Mock the AI service for pattern analysis
        when(tradingSignalAIService.analyzePatternsForTradingSignal(any(MarketData.class), anyString(), anyList()))
                .thenReturn(new TradingSignalAIService.TradingSignalAnalysis("HOLD", 50.0, "Test analysis.", "Test risk."));

        mockMvc.perform(get("/api/marketdata/symbol/NFLX/range")
                .param("start", "2026-04-18T09:30:00")
                .param("end", "2026-04-18T10:30:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Data retrieved successfully")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.count", is(1)))
                .andExpect(jsonPath("$.data[0].timestamp", is("2026-04-18T10:00:00")));
    }

    @Test
    public void testGetMarketDataBySymbolAndRange_ExcludePatterns() throws Exception {
        repository.save(new MarketData("NFLX", LocalDateTime.parse("2026-04-18T09:00:00"), 500.00, 510.00, 495.00, 505.00, 700000));
        repository.save(new MarketData("NFLX", LocalDateTime.parse("2026-04-18T10:00:00"), 505.00, 515.00, 500.00, 510.00, 750000));

        mockMvc.perform(get("/api/marketdata/symbol/NFLX/range")
                .param("start", "2026-04-18T09:30:00")
                .param("end", "2026-04-18T10:30:00")
                .param("includePatterns", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].detectedPatterns", hasSize(0)));
    }

    @Test
    public void testGetMarketDataDaily_ReturnsData() throws Exception {
        when(alphaVantageService.getDailyLatest("IBM")).thenReturn(new MarketData("IBM", LocalDateTime.parse("2026-04-18T10:00:00"), 120.00, 125.00, 118.00, 123.00, 450000));
        when(alphaVantageService.getDailyHistory("IBM")).thenReturn(List.of(new MarketData("IBM", LocalDateTime.parse("2026-04-18T09:00:00"), 118.00, 123.00, 117.00, 120.00, 430000)));
        when(patternAnalysisService.detectPatterns(any(MarketData.class), anyList())).thenReturn(List.of());
        when(tradingSignalAIService.analyzePatternsForTradingSignal(any(MarketData.class), anyString(), anyList()))
                .thenReturn(new TradingSignalAIService.TradingSignalAnalysis("HOLD", 55.0, "Daily trend neutral.", "Volume is average."));

        mockMvc.perform(get("/api/marketdata/daily/IBM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("IBM")))
                .andExpect(jsonPath("$.detectedPatterns", hasSize(0)));
    }

    @Test
    public void testGetMarketDataDailyHistory_ExcludePatterns() throws Exception {
        when(alphaVantageService.getDailyHistory("IBM")).thenReturn(List.of(
                new MarketData("IBM", LocalDateTime.parse("2026-04-18T09:00:00"), 120.00, 125.00, 118.00, 123.00, 450000)
        ));

        mockMvc.perform(get("/api/marketdata/daily/IBM/history").param("includePatterns", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].detectedPatterns", hasSize(0)));
    }

    @Test
    public void testGetMarketDataIntraDay_ExcludePatterns() throws Exception {
        when(alphaVantageService.getIntraDayLatest("TSLA")).thenReturn(new MarketData("TSLA", LocalDateTime.parse("2026-04-18T10:00:00"), 200.00, 210.00, 195.00, 205.00, 800000));
        when(alphaVantageService.getIntraDayHistory("TSLA")).thenReturn(List.of(new MarketData("TSLA", LocalDateTime.parse("2026-04-18T09:00:00"), 198.00, 205.00, 196.00, 200.00, 700000)));

        mockMvc.perform(get("/api/marketdata/intraday/TSLA").param("includePatterns", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("TSLA")))
                .andExpect(jsonPath("$.detectedPatterns", hasSize(0)));
    }

    @Test
    public void testPatternsEndpoint() throws Exception {
        when(patternAnalysisService.detectPatterns(any(MarketData.class), anyList())).thenReturn(List.of());
        when(tradingSignalAIService.analyzePatternsForTradingSignal(any(MarketData.class), anyString(), anyList()))
                .thenReturn(new TradingSignalAIService.TradingSignalAnalysis("HOLD", 50.0, "Synthetic Doji analysis.", "Low risk due to synthetic data."));

        mockMvc.perform(get("/api/marketdata/test/patterns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", containsString("TEST_DOJI")))
                .andExpect(jsonPath("$.detectedPatterns", hasSize(greaterThanOrEqualTo(0))));
    }

    @Test
    public void testGetMarketDataBySymbolAndRange_InvalidRange() throws Exception {
        mockMvc.perform(get("/api/marketdata/symbol/NFLX/range")
                .param("start", "2026-04-18T11:00:00")
                .param("end", "2026-04-18T10:00:00"))
                .andExpect(status().isBadRequest());
    }
}