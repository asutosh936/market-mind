package com.marketmind.integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketmind.model.MarketData;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO for Alpha Vantage Global Quote API response.
 * Maps to QUOTE_ENDPOINT function for real-time price data.
 */
public class AlphaVantageQuoteResponse {

    @JsonProperty("Global Quote")
    private GlobalQuote globalQuote;

    public GlobalQuote getGlobalQuote() {
        return globalQuote;
    }

    public void setGlobalQuote(GlobalQuote globalQuote) {
        this.globalQuote = globalQuote;
    }

    public MarketData toMarketData(String symbol) {
        if (globalQuote == null || globalQuote.getPrice() == null || globalQuote.getPrice().isEmpty()) {
            return null;
        }
        MarketData marketData = new MarketData();
        marketData.setSymbol(symbol);
        marketData.setOpenPrice(parseDouble(globalQuote.getOpen()));
        marketData.setHighPrice(parseDouble(globalQuote.getHigh()));
        marketData.setLowPrice(parseDouble(globalQuote.getLow()));
        marketData.setClosePrice(parseDouble(globalQuote.getPrice()));
        marketData.setVolume(parseLong(globalQuote.getVolume()));
        marketData.setTimestamp(LocalDateTime.now());
        return marketData;
    }

    private static double parseDouble(String value) {
        return value != null && !value.isEmpty() ? Double.parseDouble(value) : 0.0;
    }

    private static long parseLong(String value) {
        return value != null && !value.isEmpty() ? Long.parseLong(value) : 0L;
    }

    public static class GlobalQuote {
        @JsonProperty("01. symbol")
        private String symbol;

        @JsonProperty("02. open")
        private String open;

        @JsonProperty("03. high")
        private String high;

        @JsonProperty("04. low")
        private String low;

        @JsonProperty("05. price")
        private String price;

        @JsonProperty("06. volume")
        private String volume;

        @JsonProperty("07. latest trading day")
        private String latestTradingDay;

        @JsonProperty("08. previous close")
        private String previousClose;

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public String getOpen() { return open; }
        public void setOpen(String open) { this.open = open; }

        public String getHigh() { return high; }
        public void setHigh(String high) { this.high = high; }

        public String getLow() { return low; }
        public void setLow(String low) { this.low = low; }

        public String getPrice() { return price; }
        public void setPrice(String price) { this.price = price; }

        public String getVolume() { return volume; }
        public void setVolume(String volume) { this.volume = volume; }

        public String getLatestTradingDay() { return latestTradingDay; }
        public void setLatestTradingDay(String latestTradingDay) { this.latestTradingDay = latestTradingDay; }

        public String getPreviousClose() { return previousClose; }
        public void setPreviousClose(String previousClose) { this.previousClose = previousClose; }
    }
}
