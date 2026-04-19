package com.marketmind.integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketmind.model.MarketData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * DTO for Alpha Vantage Daily API response.
 * Maps to TIME_SERIES_DAILY function for daily OHLCV data.
 */
public class AlphaVantageDailyResponse {

    @JsonProperty("Meta Data")
    private MetaData metaData;

    @JsonProperty("Time Series (Daily)")
    private Map<String, TimeSeriesData> timeSeries;

    public MetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }

    public Map<String, TimeSeriesData> getTimeSeries() {
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
        String latestDate = series.keySet().stream().findFirst().orElse(null);
        if (latestDate == null) {
            return null;
        }

        TimeSeriesData data = series.get(latestDate);
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

        // Convert date to datetime (use end of trading day)
        try {
            LocalDate date = LocalDate.parse(latestDate);
            marketData.setTimestamp(date.atTime(16, 0, 0)); // 4 PM close
        } catch (Exception e) {
            marketData.setTimestamp(LocalDateTime.now());
        }

        return marketData;
    }

    /**
     * Convert all time series entries to MarketData list.
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
                LocalDate date = LocalDate.parse(entry.getKey());
                marketData.setTimestamp(date.atTime(16, 0, 0));
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

        @JsonProperty("4. Output Size")
        private String outputSize;

        @JsonProperty("5. Time Zone")
        private String timeZone;

        // Getters and setters
        public String getInformation() { return information; }
        public void setInformation(String information) { this.information = information; }

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public String getLastRefreshed() { return lastRefreshed; }
        public void setLastRefreshed(String lastRefreshed) { this.lastRefreshed = lastRefreshed; }

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
