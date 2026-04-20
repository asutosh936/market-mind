package com.marketmind.service;

import com.marketmind.model.MarketData;
import com.marketmind.model.dto.PatternDto;
import com.marketmind.model.dto.MarketDataWithPatternsDto;
import com.marketmind.model.dto.TradingSignalDto;
import com.marketmind.repository.MarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);
    private static final int HISTORY_SIZE = 5; // Last 5 candles for pattern context

    @Autowired
    private MarketDataRepository repository;

    @Autowired
    private PatternAnalysisService patternAnalysisService;

    @Autowired
    private TradingSignalAIService tradingSignalAIService;

    public List<MarketData> findAll() {
        logger.debug("Fetching all market data records");
        List<MarketData> results = repository.findAll();
        logger.info("Retrieved {} market data records", results.size());
        return results;
    }

    public Optional<MarketData> findById(Long id) {
        logger.debug("Fetching market data by id={}", id);
        Optional<MarketData> result = repository.findById(id);
        result.ifPresentOrElse(
                data -> logger.info("Market data found id={}", id),
                () -> logger.warn("Market data not found id={}", id)
        );
        return result;
    }

    public MarketData save(MarketData marketData) {
        logger.info("Saving market data symbol={}", marketData.getSymbol());
        MarketData saved = repository.save(marketData);
        logger.debug("Saved market data id={}", saved.getId());
        return saved;
    }

    public List<MarketData> findBySymbol(String symbol) {
        logger.debug("Searching market data by symbol={}", symbol);
        List<MarketData> results = repository.findBySymbolIgnoreCaseOrderByTimestampDesc(symbol);
        logger.info("Found {} records for symbol={}", results.size(), symbol);
        return results;
    }

    public List<MarketData> findBySymbolAndRange(String symbol, LocalDateTime start, LocalDateTime end) {
        logger.debug("Searching market data for symbol={} between {} and {}", symbol, start, end);
        List<MarketData> results = repository.findBySymbolIgnoreCaseAndTimestampBetweenOrderByTimestampAsc(symbol, start, end);
        logger.info("Found {} records for symbol={} in range", results.size(), symbol);
        return results;
    }

    public void deleteById(Long id) {
        logger.info("Deleting market data id={}", id);
        repository.deleteById(id);
    }

    /**
     * Get market data with detected patterns.
     * Analyzes current candle with historical context (last 5 candles).
     * 
     * @param marketData The current market data to analyze
     * @param historicalData List of historical market data (ordered chronologically)
     * @return MarketDataWithPatternsDto containing patterns detected
     */
    public MarketDataWithPatternsDto getWithPatterns(MarketData marketData, List<MarketData> historicalData) {
        logger.debug("Analyzing patterns for symbol={}, timestamp={}, OHLCV: O={}, H={}, L={}, C={}, V={}", 
                   marketData.getSymbol(), marketData.getTimestamp(),
                   marketData.getOpenPrice(), marketData.getHighPrice(), 
                   marketData.getLowPrice(), marketData.getClosePrice(), marketData.getVolume());
        
        // Get up to last HISTORY_SIZE candles before current one (excluding current)
        List<MarketData> previousCandles = historicalData.stream()
            .filter(data -> data.getTimestamp().isBefore(marketData.getTimestamp()))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp())) // Most recent first
            .limit(HISTORY_SIZE)
            .collect(Collectors.toList());

        // Reverse to get chronological order (oldest first)
        previousCandles = previousCandles.stream()
            .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
            .collect(Collectors.toList());

        logger.debug("Using {} previous candles for pattern analysis", previousCandles.size());

        // Detect patterns
        List<PatternDto> detectedPatterns = patternAnalysisService.detectPatterns(marketData, previousCandles);
        
        logger.debug("Detected {} patterns: {}", detectedPatterns.size(), 
                   detectedPatterns.stream()
                       .map(p -> p.getName() + "(" + String.format("%.2f", p.getConfidence()) + ")")
                       .collect(Collectors.joining(", ")));

        String priceTrend = computeFiveDayTrend(previousCandles);
        logger.debug("Computed 5-day price trend for {}: {}", marketData.getSymbol(), priceTrend);

        // Generate AI-powered trading signal
        TradingSignalAIService.TradingSignalAnalysis aiAnalysis =
            tradingSignalAIService.analyzePatternsForTradingSignal(
                marketData,
                priceTrend,
                detectedPatterns
            );

        TradingSignalDto tradingSignal = new TradingSignalDto(
            aiAnalysis.getSignal(),
            aiAnalysis.getConfidence(),
            aiAnalysis.getReasoning(),
            aiAnalysis.getRiskNote()
        );

        logger.debug("AI Trading Signal: {} ({}%) - {}", tradingSignal.getSignal(),
                   String.format("%.1f", tradingSignal.getConfidence()), tradingSignal.getReasoning());

        return new MarketDataWithPatternsDto(marketData, detectedPatterns, tradingSignal);
    }

    private String computeFiveDayTrend(List<MarketData> previousCandles) {
        if (previousCandles.isEmpty()) {
            return "Insufficient trend data";
        }

        double firstClose = previousCandles.get(0).getClosePrice();
        double lastClose = previousCandles.get(previousCandles.size() - 1).getClosePrice();
        double percentChange = firstClose == 0 ? 0 : ((lastClose - firstClose) / firstClose) * 100;

        if (percentChange > 1.0) {
            return String.format("Uptrend (%.2f%% over 5 bars)", percentChange);
        } else if (percentChange < -1.0) {
            return String.format("Downtrend (%.2f%% over 5 bars)", percentChange);
        } else {
            return String.format("Sideways (%.2f%% over 5 bars)", percentChange);
        }
    }

    /**
     * Get market data by symbol with patterns detected for the latest entry.
     * 
     * @param symbol Stock symbol
     * @return MarketDataWithPatternsDto or empty Optional if symbol not found
     */
    public Optional<MarketDataWithPatternsDto> findBySymbolWithPatterns(String symbol) {
        List<MarketData> allData = findBySymbol(symbol);
        if (allData.isEmpty()) {
            return Optional.empty();
        }

        // Latest entry is first (ordered by timestamp DESC)
        MarketData latest = allData.get(0);
        
        // Get all previous data for pattern analysis
        return Optional.of(getWithPatterns(latest, allData));
    }
}