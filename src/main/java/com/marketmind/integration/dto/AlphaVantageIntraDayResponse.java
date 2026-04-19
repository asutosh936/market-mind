package com.marketmind.integration.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketmind.model.MarketData;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * DTO for Alpha Vantage Intraday API response.
 * Maps to TIME_SERIES_INTRADAY function for minute-level data.
 */
public class AlphaVantageIntraDayResponse {

    @JsonProperty("Meta Data")
    private MetaData metaData;

    @JsonProperty("Time Series (1min)")
    private Map<String, TimeSeriesData> timeSeries;

    @JsonAnySetter
    private Map<String, Object> otherTimeSeriesFormats = new HashMap<>();

    public MetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }

    public Map<String, TimeSeriesData> getTimeSeries() {
        if (timeSeries == null || timeSeries.isEmpty()) {
            // Try other interval formats
            for (Map.Entry<String, Object> entry : otherTimeSeriesFormats.entrySet()) {
                if (entry.getKey().startsWith("Time Series")) {
                    timeSeries = (Map<String, TimeSeriesData>) entry.getValue();
                    break;
                }
            }
        }
        return timeSeries != null ? timeSeries : new HashMap<>();
    }

    public void setTimeSeries(Map<String, TimeSeriesData> timeSeries) {
        this.timeSeries = timeSeries;
    }

    /**
     * Convert the most recent time series entry to MarketData.
     */
    public MarketData toMarketData(String symbol) {
        Map<String, TimeSeriesData> series = getTimeSeries();
        if (series.isEmpty()) {
            return null;
        }
        
        // Get the first (most recent) entry
        String latestTimestamp = series.keySet().stream().findFirst().orElse(null);
        if (latestTimestamp == null) {
            return null;
        }

        TimeSeriesData data = series.get(latestTimestamp);
        if (data == null) {
            return null;
        }

        MarketData marketData = new MarketData();
        marketData.setSymbol(symbol);
        marketData.setOpenPrice(data.getOpenPrice());
        marketData.setHighPrice(data.getHighPrice());
        marketData.setLowPrice(data.getLowPrice());
        marketData.setClosePrice(data.getClosePrice());
        marketData.setVolume(data.getVolume());

        // Parse timestamp (format: "2026-04-18 10:30:00")
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            marketData.setTimestamp(LocalDateTime.parse(latestTimestamp, formatter));
        } catch (Exception e) {
            marketData.setTimestamp(LocalDateTime.now());
        }

        return marketData;
    }

    /**
     * Convert all time series entries to MarketData list (for historical data).
     */
    public List<MarketData> toMarketDataList(String symbol) {
        Map<String, TimeSeriesData> series = getTimeSeries();
        List<MarketData> result = new ArrayList<>();

        for (Map.Entry<String, TimeSeriesData> entry : series.entrySet()) {
            TimeSeriesData data = entry.getValue();
            MarketData marketData = new MarketData();
            marketData.setSymbol(symbol);
            marketData.setOpenPrice(data.getOpenPrice());
            marketData.setHighPrice(data.getHighPrice());
            marketData.setLowPrice(data.getLowPrice());
            marketData.setClosePrice(data.getClosePrice());
            marketData.setVolume(data.getVolume());

            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                marketData.setTimestamp(LocalDateTime.parse(entry.getKey(), formatter));
            } catch (Exception e) {
                marketData.setTimestamp(LocalDateTime.now());
            }

            result.add(marketData);
        }

        return result;
    }

    public static class MetaData {
        @JsonProperty("1. Information")
        private String information;

        @JsonProperty("2. Symbol")
        private String symbol;

        @JsonProperty("3. Last Refreshed")
        private String lastRefreshed;

        @JsonProperty("4. Interval")
        private String interval;

        @JsonProperty("5. Output Size")
        private String outputSize;

        @JsonProperty("6. Time Zone")
        private String timeZone;

        // Getters and setters
        public String getInformation() { return information; }
        public void setInformation(String information) { this.information = information; }

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public String getLastRefreshed() { return lastRefreshed; }
        public void setLastRefreshed(String lastRefreshed) { this.lastRefreshed = lastRefreshed; }

        public String getInterval() { return interval; }
        public void setInterval(String interval) { this.interval = interval; }

        public String getOutputSize() { return outputSize; }
        public void setOutputSize(String outputSize) { this.outputSize = outputSize; }

        public String getTimeZone() { return timeZone; }
        public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    }

    public static class TimeSeriesData {
        @JsonProperty("1. open")
        private double openPrice;

        @JsonProperty("2. high")
        private double highPrice;

        @JsonProperty("3. low")
        private double lowPrice;

        @JsonProperty("4. close")
        private double closePrice;

        @JsonProperty("5. volume")
        private long volume;

        public double getOpenPrice() { return openPrice; }
        public void setOpenPrice(double openPrice) { this.openPrice = openPrice; }

        public double getHighPrice() { return highPrice; }
        public void setHighPrice(double highPrice) { this.highPrice = highPrice; }

        public double getLowPrice() { return lowPrice; }
        public void setLowPrice(double lowPrice) { this.lowPrice = lowPrice; }

        public double getClosePrice() { return closePrice; }
        public void setClosePrice(double closePrice) { this.closePrice = closePrice; }

        public long getVolume() { return volume; }
        public void setVolume(long volume) { this.volume = volume; }
    }
}
