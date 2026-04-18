package com.marketmind.controller;

import com.marketmind.model.MarketData;
import com.marketmind.service.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/marketdata")
public class MarketDataController {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);

    @Autowired
    private MarketDataService service;

    @GetMapping
    public List<MarketData> getAllMarketData() {
        logger.info("Request received: getAllMarketData");
        List<MarketData> marketDataList = service.findAll();
        logger.debug("Found {} market data records", marketDataList.size());
        return marketDataList;
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