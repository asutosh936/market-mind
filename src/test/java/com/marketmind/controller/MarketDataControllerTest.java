package com.marketmind.controller;

import com.marketmind.model.MarketData;
import com.marketmind.repository.MarketDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
public class MarketDataControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MarketDataRepository repository;

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

        mockMvc.perform(get("/api/marketdata/symbol/NFLX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", is("NFLX")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    public void testGetMarketDataBySymbolAndRange() throws Exception {
        repository.save(new MarketData("NFLX", LocalDateTime.parse("2026-04-18T09:00:00"), 500.00, 510.00, 495.00, 505.00, 700000));
        repository.save(new MarketData("NFLX", LocalDateTime.parse("2026-04-18T10:00:00"), 505.00, 515.00, 500.00, 510.00, 750000));
        repository.save(new MarketData("NFLX", LocalDateTime.parse("2026-04-18T11:00:00"), 510.00, 520.00, 505.00, 515.00, 780000));

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
    public void testGetMarketDataBySymbolAndRange_InvalidRange() throws Exception {
        mockMvc.perform(get("/api/marketdata/symbol/NFLX/range")
                .param("start", "2026-04-18T11:00:00")
                .param("end", "2026-04-18T10:00:00"))
                .andExpect(status().isBadRequest());
    }
}