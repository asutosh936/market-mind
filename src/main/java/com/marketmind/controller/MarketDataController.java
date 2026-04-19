package com.marketmind.controller;

import com.marketmind.model.MarketData;
import com.marketmind.model.dto.MarketDataWithPatternsDto;
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
    public ApiResponse<MarketDataWithPatternsDto> getAllMarketData(
            @RequestParam(value = "includePatterns", defaultValue = "true") boolean includePatterns) {
        logger.info("Request received: getAllMarketData includePatterns={}", includePatterns);
        List<MarketData> marketDataList = service.findAll();
        List<MarketDataWithPatternsDto> results = new java.util.ArrayList<>();
        
        if (includePatterns) {
            for (MarketData data : marketDataList) {
                var allData = service.findBySymbol(data.getSymbol());
                results.add(service.getWithPatterns(data, allData));
            }
        } else {
            results = marketDataList.stream()
                .map(data -> new MarketDataWithPatternsDto(data, java.util.List.of()))
                .collect(java.util.stream.Collectors.toList());
        }
        
        logger.debug("Found {} market data records with patterns", results.size());
        return new ApiResponse<>(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarketDataWithPatternsDto> getMarketDataById(
            @PathVariable Long id,
            @RequestParam(value = "includePatterns", defaultValue = "true") boolean includePatterns) {
        logger.info("Request received: getMarketDataById id={} includePatterns={}", id, includePatterns);
        return service.findById(id)
                .map(marketData -> {
                    logger.debug("Found market data record id={}", id);
                    if (includePatterns) {
                        var allData = service.findBySymbol(marketData.getSymbol());
                        return ResponseEntity.ok(service.getWithPatterns(marketData, allData));
                    } else {
                        return ResponseEntity.ok(new MarketDataWithPatternsDto(marketData, java.util.List.of()));
                    }
                })
                .orElseGet(() -> {
                    logger.warn("Market data record not found id={}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Get real-time quote from Alpha Vantage with optional pattern detection.
     * Wrapped endpoint for symbol-based query.
     * First tries to fetch from database (cached data), then falls back to Alpha Vantage API.
     * Pattern detection is included by default.
     */
    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<MarketDataWithPatternsDto> getMarketDataBySymbol(
            @PathVariable String symbol,
            @RequestParam(value = "includePatterns", defaultValue = "true") boolean includePatterns) {
        logger.info("Request received: getMarketDataBySymbol symbol={} includePatterns={}", symbol, includePatterns);
        try {
            // First try to get from cached database
            List<MarketData> cachedData = service.findBySymbol(symbol);
            if (!cachedData.isEmpty()) {
                logger.debug("Retrieved cached data for symbol={} from database, records={}", symbol, cachedData.size());
                MarketData latest = cachedData.get(0);
                if (includePatterns) {
                    return ResponseEntity.ok(service.getWithPatterns(latest, cachedData));
                } else {
                    return ResponseEntity.ok(new MarketDataWithPatternsDto(latest, java.util.List.of()));
                }
            }
            
            // Fall back to real-time Alpha Vantage API
            logger.debug("No cached data found for symbol={}, querying Alpha Vantage API", symbol);
            MarketData marketData = alphaVantageService.getQuote(symbol);
            if (marketData != null) {
                logger.debug("Successfully retrieved real-time data for symbol={}", symbol);
                if (includePatterns) {
                    // For pattern detection, we need historical context. If we don't have cached data,
                    // fetch intraday history to provide context for pattern analysis
                    List<MarketData> history = service.findBySymbol(symbol);
                    logger.debug("Found {} historical records in database for symbol={}", history.size(), symbol);
                    if (history.size() <= 1) {
                        logger.debug("Insufficient historical data for pattern detection, fetching intraday history");
                        history = alphaVantageService.getIntraDayHistory(symbol);
                        logger.debug("Intraday history fetch returned {} records", history.size());
                        // If intraday history fails (e.g., demo API limitations), try daily history as fallback
                        if (history.isEmpty()) {
                            logger.debug("Intraday history unavailable, trying daily history as fallback");
                            history = alphaVantageService.getDailyHistory(symbol);
                            logger.debug("Daily history fetch returned {} records", history.size());
                        }
                    }
                    return ResponseEntity.ok(service.getWithPatterns(marketData, history));
                } else {
                    return ResponseEntity.ok(new MarketDataWithPatternsDto(marketData, java.util.List.of()));
                }
            }
            logger.warn("No real-time data available for symbol={}", symbol);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching data for symbol={}: {}", symbol, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Test endpoint to demonstrate pattern detection with synthetic data.
     * Creates sample market data that forms recognizable candlestick patterns.
     */
    @GetMapping("/test/patterns")
    public ResponseEntity<MarketDataWithPatternsDto> testPatterns() {
        logger.info("Request received: testPatterns - creating synthetic data for pattern demonstration");
        
        // Create synthetic data that forms a Doji pattern
        // Doji: body < 5% of total range (high - low)
        LocalDateTime now = LocalDateTime.now();
        MarketData dojiCandle = new MarketData("TEST_DOJI", now, 100.0, 110.0, 90.0, 100.1, 100000);
        // Open: 100.0, Close: 100.1, High: 110.0, Low: 90.0
        // Body size: |100.1 - 100.0| = 0.1
        // Total range: 110.0 - 90.0 = 20.0
        // Body ratio: 0.1 / 20.0 = 0.005 (0.5%) < 5% threshold
        
        // Create some previous candles for context
        List<MarketData> historicalData = List.of(
            new MarketData("TEST_DOJI", now.minusMinutes(5), 99.0, 101.0, 98.5, 100.5, 95000),
            new MarketData("TEST_DOJI", now.minusMinutes(4), 100.5, 102.5, 99.5, 101.5, 105000),
            new MarketData("TEST_DOJI", now.minusMinutes(3), 101.5, 103.0, 100.0, 102.0, 110000),
            new MarketData("TEST_DOJI", now.minusMinutes(2), 102.0, 104.0, 101.0, 103.0, 115000),
            new MarketData("TEST_DOJI", now.minusMinutes(1), 103.0, 105.0, 102.0, 104.0, 120000)
        );
        
        // Save to database for consistency
        service.save(dojiCandle);
        historicalData.forEach(service::save);
        
        // Detect patterns
        MarketDataWithPatternsDto result = service.getWithPatterns(dojiCandle, historicalData);
        
        logger.info("Test patterns endpoint: detected {} patterns", result.getDetectedPatterns().size());
        return ResponseEntity.ok(result);
    }
    @GetMapping("/intraday/{symbol}")
    public ResponseEntity<MarketDataWithPatternsDto> getMarketDataIntraDay(
            @PathVariable String symbol,
            @RequestParam(value = "includePatterns", defaultValue = "true") boolean includePatterns) {
        logger.info("Request received: getMarketDataIntraDay symbol={} includePatterns={}", symbol, includePatterns);
        try {
            MarketData marketData = alphaVantageService.getIntraDayLatest(symbol);
            if (marketData != null) {
                logger.debug("Successfully retrieved intraday data for symbol={}", symbol);
                if (includePatterns) {
                    List<MarketData> history = alphaVantageService.getIntraDayHistory(symbol);
                    return ResponseEntity.ok(service.getWithPatterns(marketData, history));
                } else {
                    return ResponseEntity.ok(new MarketDataWithPatternsDto(marketData, java.util.List.of()));
                }
            }
            logger.warn("No intraday data available for symbol={}", symbol);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching intraday data for symbol={}: {}", symbol, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get intraday history from Alpha Vantage with optional pattern detection.
     * Returns historical intraday data for a symbol.
     * Pattern detection is included by default.
     */
    @GetMapping("/intraday/{symbol}/history")
    public ResponseEntity<ApiResponse<MarketDataWithPatternsDto>> getMarketDataIntraDayHistory(
            @PathVariable String symbol,
            @RequestParam(value = "includePatterns", defaultValue = "true") boolean includePatterns) {
        logger.info("Request received: getMarketDataIntraDayHistory symbol={} includePatterns={}", symbol, includePatterns);
        try {
            List<MarketData> marketDataList = alphaVantageService.getIntraDayHistory(symbol);
            if (!marketDataList.isEmpty()) {
                logger.debug("Successfully retrieved intraday history for symbol={}, records={}", symbol, marketDataList.size());
                List<MarketDataWithPatternsDto> results;
                if (includePatterns) {
                    results = marketDataList.stream()
                        .map(data -> service.getWithPatterns(data, marketDataList))
                        .collect(java.util.stream.Collectors.toList());
                } else {
                    results = marketDataList.stream()
                        .map(data -> new MarketDataWithPatternsDto(data, java.util.List.of()))
                        .collect(java.util.stream.Collectors.toList());
                }
                return ResponseEntity.ok(new ApiResponse<>(results));
            }
            logger.warn("No intraday history available for symbol={}", symbol);
            return ResponseEntity.ok(new ApiResponse<>(false, "No intraday history data available for symbol: " + symbol, java.util.List.of()));
        } catch (Exception e) {
            logger.error("Error fetching intraday history for symbol={}: {}", symbol, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get daily data (latest close) from Alpha Vantage with optional pattern detection.
     * Pattern detection is included by default.
     */
    @GetMapping("/daily/{symbol}")
    public ResponseEntity<MarketDataWithPatternsDto> getMarketDataDaily(
            @PathVariable String symbol,
            @RequestParam(value = "includePatterns", defaultValue = "true") boolean includePatterns) {
        logger.info("Request received: getMarketDataDaily symbol={} includePatterns={}", symbol, includePatterns);
        try {
            MarketData marketData = alphaVantageService.getDailyLatest(symbol);
            if (marketData != null) {
                logger.debug("Successfully retrieved daily data for symbol={}", symbol);
                if (includePatterns) {
                    List<MarketData> history = alphaVantageService.getDailyHistory(symbol);
                    return ResponseEntity.ok(service.getWithPatterns(marketData, history));
                } else {
                    return ResponseEntity.ok(new MarketDataWithPatternsDto(marketData, java.util.List.of()));
                }
            }
            logger.warn("No daily data available for symbol={}", symbol);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching daily data for symbol={}: {}", symbol, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get daily history from Alpha Vantage with optional pattern detection.
     * Returns full historical daily data for a symbol.
     * Pattern detection is included by default.
     */
    @GetMapping("/daily/{symbol}/history")
    public ResponseEntity<ApiResponse<MarketDataWithPatternsDto>> getMarketDataDailyHistory(
            @PathVariable String symbol,
            @RequestParam(value = "includePatterns", defaultValue = "true") boolean includePatterns) {
        logger.info("Request received: getMarketDataDailyHistory symbol={} includePatterns={}", symbol, includePatterns);
        try {
            List<MarketData> marketDataList = alphaVantageService.getDailyHistory(symbol);
            if (!marketDataList.isEmpty()) {
                logger.debug("Successfully retrieved daily history for symbol={}, records={}", symbol, marketDataList.size());
                List<MarketDataWithPatternsDto> results;
                if (includePatterns) {
                    results = marketDataList.stream()
                        .map(data -> service.getWithPatterns(data, marketDataList))
                        .collect(java.util.stream.Collectors.toList());
                } else {
                    results = marketDataList.stream()
                        .map(data -> new MarketDataWithPatternsDto(data, java.util.List.of()))
                        .collect(java.util.stream.Collectors.toList());
                }
                return ResponseEntity.ok(new ApiResponse<>(results));
            }
            logger.warn("No daily history available for symbol={}", symbol);
            return ResponseEntity.ok(new ApiResponse<>(false, "No daily history data available for symbol: " + symbol, java.util.List.of()));
        } catch (Exception e) {
            logger.error("Error fetching daily history for symbol={}: {}", symbol, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get range-based data from database (local queries only) with optional pattern detection.
     * Pattern detection is included by default.
     */
    @GetMapping("/symbol/{symbol}/range")
    public ResponseEntity<ApiResponse<MarketDataWithPatternsDto>> getMarketDataBySymbolAndRange(
            @PathVariable String symbol,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(value = "includePatterns", defaultValue = "true") boolean includePatterns) {
        logger.info("Request received: getMarketDataBySymbolAndRange symbol={} start={} end={} includePatterns={}", symbol, start, end, includePatterns);
        if (end.isBefore(start)) {
            logger.warn("Invalid date range start={} end={} for symbol={}", start, end, symbol);
            return ResponseEntity.badRequest().build();
        }
        List<MarketData> marketDataList = service.findBySymbolAndRange(symbol, start, end);
        logger.debug("Found {} records for symbol={} in range {}-{}", marketDataList.size(), symbol, start, end);
        
        List<MarketDataWithPatternsDto> results;
        if (includePatterns) {
            results = marketDataList.stream()
                .map(data -> service.getWithPatterns(data, marketDataList))
                .collect(java.util.stream.Collectors.toList());
        } else {
            results = marketDataList.stream()
                .map(data -> new MarketDataWithPatternsDto(data, java.util.List.of()))
                .collect(java.util.stream.Collectors.toList());
        }
        return ResponseEntity.ok(new ApiResponse<>(results));
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
