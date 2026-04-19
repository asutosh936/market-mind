package com.marketmind.controller;

import com.marketmind.model.MarketData;
import com.marketmind.service.MarketDataService;
import com.marketmind.integration.service.AlphaVantageService;
import com.marketmind.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/marketdata")
public class MarketDataController {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);

    @Autowired
    private MarketDataService service;

    @Autowired
    private AlphaVantageService alphaVantageService;

    @GetMapping
    public ApiResponse<MarketData> getAllMarketData() {
        logger.info("Request received: getAllMarketData");
        List<MarketData> marketDataList = service.findAll();
        logger.debug("Found {} market data records", marketDataList.size());
        return new ApiResponse<>(marketDataList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarketData> getMarketDataById(@PathVariable Long id) {
        logger.info("Request received: getMarketDataById id={}", id);
        return service.findById(id)
                .map(marketData -> {
                    logger.debug("Found market data record id={}", id);
                    return ResponseEntity.ok(marketData);
                })
                .orElseGet(() -> {
                    logger.warn("Market data record not found id={}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Get real-time quote from Alpha Vantage.
     * Wrapped endpoint for symbol-based query.
     * First tries to fetch from database (cached data), then falls back to Alpha Vantage API.
     */
    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<MarketData> getMarketDataBySymbol(@PathVariable String symbol) {
        logger.info("Request received: getMarketDataBySymbol symbol={}", symbol);
        try {
            // First try to get from cached database
            List<MarketData> cachedData = service.findBySymbol(symbol);
            if (!cachedData.isEmpty()) {
                logger.debug("Retrieved cached data for symbol={} from database, records={}", symbol, cachedData.size());
                return ResponseEntity.ok().body(cachedData.get(0)); // Return most recent
            }
            
            // Fall back to real-time Alpha Vantage API
            logger.debug("No cached data found for symbol={}, querying Alpha Vantage API", symbol);
            MarketData marketData = alphaVantageService.getQuote(symbol);
            if (marketData != null) {
                logger.debug("Successfully retrieved real-time data for symbol={}", symbol);
                return ResponseEntity.ok(marketData);
            }
            logger.warn("No real-time data available for symbol={}", symbol);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching data for symbol={}: {}", symbol, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get intraday data (latest 1-minute bar) from Alpha Vantage.
     * New endpoint for real-time intraday data.
     */
    @GetMapping("/intraday/{symbol}")
    public ResponseEntity<MarketData> getMarketDataIntraDay(@PathVariable String symbol) {
        logger.info("Request received: getMarketDataIntraDay symbol={}", symbol);
        try {
            MarketData marketData = alphaVantageService.getIntraDayLatest(symbol);
            if (marketData != null) {
                logger.debug("Successfully retrieved intraday data for symbol={}", symbol);
                return ResponseEntity.ok(marketData);
            }
            logger.warn("No intraday data available for symbol={}", symbol);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching intraday data for symbol={}: {}", symbol, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get intraday history from Alpha Vantage.
     * Returns historical intraday data for a symbol.
     */
    @GetMapping("/intraday/{symbol}/history")
    public ResponseEntity<ApiResponse<MarketData>> getMarketDataIntraDayHistory(@PathVariable String symbol) {
        logger.info("Request received: getMarketDataIntraDayHistory symbol={}", symbol);
        try {
            List<MarketData> marketDataList = alphaVantageService.getIntraDayHistory(symbol);
            if (!marketDataList.isEmpty()) {
                logger.debug("Successfully retrieved intraday history for symbol={}, records={}", symbol, marketDataList.size());
                return ResponseEntity.ok(new ApiResponse<>(marketDataList));
            }
            logger.warn("No intraday history available for symbol={}", symbol);
            return ResponseEntity.ok(new ApiResponse<>(false, "No intraday history data available for symbol: " + symbol, marketDataList));
        } catch (Exception e) {
            logger.error("Error fetching intraday history for symbol={}: {}", symbol, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get daily data (latest close) from Alpha Vantage.
     * New endpoint for daily OHLCV data.
     */
    @GetMapping("/daily/{symbol}")
    public ResponseEntity<MarketData> getMarketDataDaily(@PathVariable String symbol) {
        logger.info("Request received: getMarketDataDaily symbol={}", symbol);
        try {
            MarketData marketData = alphaVantageService.getDailyLatest(symbol);
            if (marketData != null) {
                logger.debug("Successfully retrieved daily data for symbol={}", symbol);
                return ResponseEntity.ok(marketData);
            }
            logger.warn("No daily data available for symbol={}", symbol);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching daily data for symbol={}: {}", symbol, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get daily history from Alpha Vantage.
     * Returns full historical daily data for a symbol.
     */
    @GetMapping("/daily/{symbol}/history")
    public ResponseEntity<ApiResponse<MarketData>> getMarketDataDailyHistory(@PathVariable String symbol) {
        logger.info("Request received: getMarketDataDailyHistory symbol={}", symbol);
        try {
            List<MarketData> marketDataList = alphaVantageService.getDailyHistory(symbol);
            if (!marketDataList.isEmpty()) {
                logger.debug("Successfully retrieved daily history for symbol={}, records={}", symbol, marketDataList.size());
                return ResponseEntity.ok(new ApiResponse<>(marketDataList));
            }
            logger.warn("No daily history available for symbol={}", symbol);
            return ResponseEntity.ok(new ApiResponse<>(false, "No daily history data available for symbol: " + symbol, marketDataList));
        } catch (Exception e) {
            logger.error("Error fetching daily history for symbol={}: {}", symbol, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get range-based data from database (local queries only).
     * Original endpoint kept for backward compatibility.
     */
    @GetMapping("/symbol/{symbol}/range")
    public ResponseEntity<ApiResponse<MarketData>> getMarketDataBySymbolAndRange(
            @PathVariable String symbol,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        logger.info("Request received: getMarketDataBySymbolAndRange (from database) symbol={} start={} end={}", symbol, start, end);
        if (end.isBefore(start)) {
            logger.warn("Invalid date range start={} end={} for symbol={}", start, end, symbol);
            return ResponseEntity.badRequest().build();
        }
        List<MarketData> marketDataList = service.findBySymbolAndRange(symbol, start, end);
        logger.debug("Found {} records for symbol={} in range {}-{}", marketDataList.size(), symbol, start, end);
        return ResponseEntity.ok(new ApiResponse<>(marketDataList));
    }

    @PostMapping
    public MarketData createMarketData(@RequestBody MarketData marketData) {
        logger.info("Request received: createMarketData symbol={}", marketData.getSymbol());
        MarketData created = service.save(marketData);
        logger.debug("Created market data id={}", created.getId());
        return created;
    }

    @PutMapping("/{id}")
    public ResponseEntity<MarketData> updateMarketData(@PathVariable Long id, @RequestBody MarketData marketDataDetails) {
        logger.info("Request received: updateMarketData id={} symbol={}", id, marketDataDetails.getSymbol());
        return service.findById(id)
                .map(existingMarketData -> {
                    existingMarketData.setSymbol(marketDataDetails.getSymbol());
                    existingMarketData.setTimestamp(marketDataDetails.getTimestamp());
                    existingMarketData.setOpenPrice(marketDataDetails.getOpenPrice());
                    existingMarketData.setHighPrice(marketDataDetails.getHighPrice());
                    existingMarketData.setLowPrice(marketDataDetails.getLowPrice());
                    existingMarketData.setClosePrice(marketDataDetails.getClosePrice());
                    existingMarketData.setVolume(marketDataDetails.getVolume());
                    MarketData updatedMarketData = service.save(existingMarketData);
                    logger.debug("Updated market data id={}", id);
                    return ResponseEntity.ok(updatedMarketData);
                })
                .orElseGet(() -> {
                    logger.warn("Update failed; market data not found id={}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMarketData(@PathVariable Long id) {
        logger.info("Request received: deleteMarketData id={}", id);
        if (service.findById(id).isPresent()) {
            service.deleteById(id);
            logger.debug("Deleted market data id={}", id);
            return ResponseEntity.noContent().build();
        } else {
            logger.warn("Delete failed; market data not found id={}", id);
            return ResponseEntity.notFound().build();
        }
    }
}
