package com.marketmind.model.dto;

import com.marketmind.model.MarketData;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Extended Market Data DTO with detected patterns.
 * This is returned by analysis endpoints.
 */
public class MarketDataWithPatternsDto {
    private Long id;
    private String symbol;
    private LocalDateTime timestamp;
    private double openPrice;
    private double highPrice;
    private double lowPrice;
    private double closePrice;
    private long volume;
    private List<PatternDto> detectedPatterns;

    public MarketDataWithPatternsDto() {}

    public MarketDataWithPatternsDto(MarketData marketData, List<PatternDto> detectedPatterns) {
        this.id = marketData.getId();
        this.symbol = marketData.getSymbol();
        this.timestamp = marketData.getTimestamp();
        this.openPrice = marketData.getOpenPrice();
        this.highPrice = marketData.getHighPrice();
        this.lowPrice = marketData.getLowPrice();
        this.closePrice = marketData.getClosePrice();
        this.volume = marketData.getVolume();
        this.detectedPatterns = detectedPatterns;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public double getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(double openPrice) {
        this.openPrice = openPrice;
    }

    public double getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(double highPrice) {
        this.highPrice = highPrice;
    }

    public double getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(double lowPrice) {
        this.lowPrice = lowPrice;
    }

    public double getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(double closePrice) {
        this.closePrice = closePrice;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public List<PatternDto> getDetectedPatterns() {
        return detectedPatterns;
    }

    public void setDetectedPatterns(List<PatternDto> detectedPatterns) {
        this.detectedPatterns = detectedPatterns;
    }
}
