package com.marketmind.integration.service;

import com.marketmind.integration.dto.AlphaVantageQuoteResponse;
import com.marketmind.integration.dto.AlphaVantageIntraDayResponse;
import com.marketmind.integration.dto.AlphaVantageDailyResponse;
import com.marketmind.model.MarketData;
import com.marketmind.repository.MarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Service for integrating with Alpha Vantage API.
 * Fetches real-time market data and caches responses to handle rate limiting.
 */
@Service
public class AlphaVantageService {

    private static final Logger logger = LoggerFactory.getLogger(AlphaVantageService.class);

    private static final String BASE_URL = "https://www.alphavantage.co/query";
    private static final String QUOTE_FUNCTION = "GLOBAL_QUOTE";
    private static final String INTRADAY_FUNCTION = "TIME_SERIES_INTRADAY";
    private static final String DAILY_FUNCTION = "TIME_SERIES_DAILY";

    @Value("${alpha-vantage.api-key:demo}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final MarketDataRepository marketDataRepository;
    private final AlphaVantageRateLimiter rateLimiter;

    public AlphaVantageService(RestTemplate restTemplate, MarketDataRepository marketDataRepository) {
        this.restTemplate = restTemplate;
        this.marketDataRepository = marketDataRepository;
    }

    /**
     * Fetch real-time stock quote via QUOTE_ENDPOINT function.
     * Results are cached for 1 minute to handle rate limiting.
     */
    @Cacheable(value = "alphaVantageQuote", key = "#symbol", unless = "#result == null")
    public MarketData getQuote(String symbol) {
        logger.info("Fetching QUOTE for symbol={}", symbol);
        try {
            String url = String.format("%s?function=%s&symbol=%s&apikey=%s",
                    BASE_URL, QUOTE_FUNCTION, symbol.toUpperCase(), apiKey);

            AlphaVantageQuoteResponse response = restTemplate.getForObject(url, AlphaVantageQuoteResponse.class);
            if (response != null) {
                MarketData marketData = response.toMarketData(symbol);
                if (marketData != null) {
                    logger.info("Successfully fetched quote for symbol={}, price={}", symbol, marketData.getClosePrice());
                    // Save to database for reference
                    return marketDataRepository.save(marketData);
                }
            }
            logger.warn("No quote data received for symbol={}", symbol);
        } catch (RestClientException e) {
            logger.error("Failed to fetch quote for symbol={}: {}", symbol, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error fetching quote for symbol={}", symbol, e);
        }
        return null;
    }

    /**
     * Fetch intraday time series data (1-minute interval).
     * Results are cached for 1 minute to handle rate limiting.
     */
    @Cacheable(value = "alphaVantageIntraDay", key = "#symbol", unless = "#result == null")
    public MarketData getIntraDayLatest(String symbol) {
        logger.info("Fetching INTRADAY latest for symbol={}", symbol);
        try {
            String url = String.format("%s?function=%s&symbol=%s&interval=1min&apikey=%s",
                    BASE_URL, INTRADAY_FUNCTION, symbol.toUpperCase(), apiKey);

            AlphaVantageIntraDayResponse response = restTemplate.getForObject(url, AlphaVantageIntraDayResponse.class);
            if (response != null) {
                MarketData marketData = response.toMarketData(symbol);
                if (marketData != null) {
                    logger.info("Successfully fetched intraday latest for symbol={}, price={}", symbol, marketData.getClosePrice());
                    return marketDataRepository.save(marketData);
                }
            }
            logger.warn("No intraday data received for symbol={}", symbol);
        } catch (RestClientException e) {
            logger.error("Failed to fetch intraday for symbol={}: {}", symbol, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error fetching intraday for symbol={}", symbol, e);
        }
        return null;
    }

    /**
     * Fetch intraday time series data as a list (for historical intraday data).
     */
    public List<MarketData> getIntraDayHistory(String symbol) {
        logger.info("Fetching INTRADAY history for symbol={}", symbol);
        try {
            String url = String.format("%s?function=%s&symbol=%s&interval=1min&apikey=%s",
                    BASE_URL, INTRADAY_FUNCTION, symbol.toUpperCase(), apiKey);

            AlphaVantageIntraDayResponse response = restTemplate.getForObject(url, AlphaVantageIntraDayResponse.class);
            if (response != null) {
                List<MarketData> marketDataList = response.toMarketDataList(symbol);
                if (!marketDataList.isEmpty()) {
                    logger.info("Successfully fetched intraday history for symbol={}, records={}", symbol, marketDataList.size());
                    return marketDataRepository.saveAll(marketDataList);
                }
            }
            logger.warn("No intraday history received for symbol={}", symbol);
        } catch (RestClientException e) {
            logger.error("Failed to fetch intraday history for symbol={}: {}", symbol, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error fetching intraday history for symbol={}", symbol, e);
        }
        return List.of();
    }

    /**
     * Fetch daily time series data (cached for 1 minute).
     */
    @Cacheable(value = "alphaVantageDaily", key = "#symbol", unless = "#result == null")
    public MarketData getDailyLatest(String symbol) {
        logger.info("Fetching DAILY latest for symbol={}", symbol);
        try {
            String url = String.format("%s?function=%s&symbol=%s&apikey=%s",
                    BASE_URL, DAILY_FUNCTION, symbol.toUpperCase(), apiKey);

            AlphaVantageDailyResponse response = restTemplate.getForObject(url, AlphaVantageDailyResponse.class);
            if (response != null) {
                MarketData marketData = response.toMarketData(symbol);
                if (marketData != null) {
                    logger.info("Successfully fetched daily latest for symbol={}, price={}", symbol, marketData.getClosePrice());
                    return marketDataRepository.save(marketData);
                }
            }
            logger.warn("No daily data received for symbol={}", symbol);
        } catch (RestClientException e) {
            logger.error("Failed to fetch daily for symbol={}: {}", symbol, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error fetching daily for symbol={}", symbol, e);
        }
        return null;
    }

    /**
     * Fetch daily time series data as a list (for historical daily data).
     */
    public List<MarketData> getDailyHistory(String symbol) {
        logger.info("Fetching DAILY history for symbol={}", symbol);
        try {
            String url = String.format("%s?function=%s&symbol=%s&outputsize=full&apikey=%s",
                    BASE_URL, DAILY_FUNCTION, symbol.toUpperCase(), apiKey);

            AlphaVantageDailyResponse response = restTemplate.getForObject(url, AlphaVantageDailyResponse.class);
            if (response != null) {
                List<MarketData> marketDataList = response.toMarketDataList(symbol);
                if (!marketDataList.isEmpty()) {
                    logger.info("Successfully fetched daily history for symbol={}, records={}", symbol, marketDataList.size());
                    return marketDataRepository.saveAll(marketDataList);
                }
            }
            logger.warn("No daily history received for symbol={}", symbol);
        } catch (RestClientException e) {
            logger.error("Failed to fetch daily history for symbol={}: {}", symbol, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error fetching daily history for symbol={}", symbol, e);
        }
        return List.of();
    }
}
