package com.marketmind.service;

import com.marketmind.model.MarketData;
import com.marketmind.repository.MarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);

    @Autowired
    private MarketDataRepository repository;

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
}