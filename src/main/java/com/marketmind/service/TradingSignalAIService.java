package com.marketmind.service;

import com.marketmind.model.MarketData;
import com.marketmind.model.dto.PatternDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI-powered trading signal analysis service.
 * Uses Spring AI with OpenAI to analyze detected patterns and provide buy/sell confidence.
 */
@Service
public class TradingSignalAIService {

    private static final Logger logger = LoggerFactory.getLogger(TradingSignalAIService.class);

    @Autowired
    private ChatModel chatModel;

    /**
     * Analyzes the current candle and detected patterns, and provides AI-powered trading signals with confidence levels.
     *
     * @param currentCandle The current market data candle used for analysis
     * @param fiveDayTrend Calculated 5-day price trend description
     * @param detectedPatterns List of detected candlestick patterns
     * @return TradingSignalAnalysis containing buy/sell/hold signal, confidence percentage, reasoning, and risk note
     */
    public TradingSignalAnalysis analyzePatternsForTradingSignal(MarketData currentCandle, String fiveDayTrend, List<PatternDto> detectedPatterns) {
        logger.info("Analyzing patterns for trading signal: symbol={}, patterns={}", currentCandle.getSymbol(), detectedPatterns.size());

        try {
            String analysisPrompt = buildAnalysisPrompt(currentCandle, fiveDayTrend, detectedPatterns);
            String aiResponse = chatModel.call(new Prompt(analysisPrompt)).getResult().getOutput().getContent();

            return parseAIResponse(aiResponse);
        } catch (Exception e) {
            logger.warn("AI analysis failed for symbol={}, falling back to rule-based analysis: {}", currentCandle.getSymbol(), e.getMessage());
            // Fallback to rule-based analysis
            return fallbackAnalysis(detectedPatterns);
        }
    }

    private String buildAnalysisPrompt(MarketData currentCandle, String fiveDayTrend, List<PatternDto> detectedPatterns) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a professional technical analyst with 15+ years of experience in equity markets, ")
              .append("specializing in price action analysis, candlestick patterns, and market momentum. ")
              .append("You provide concise, data-driven trading signals for retail traders. ")
              .append("Your analysis is objective, risk-aware, and never speculative beyond what the data supports.\n\n");

        prompt.append("---\n");
        prompt.append("Stock Symbol: ").append(currentCandle.getSymbol()).append("\n");
        prompt.append("Date: ").append(currentCandle.getTimestamp().toLocalDate()).append("\n");
        prompt.append("OHLC Data:\n");
        prompt.append("  - Open:   ").append(String.format("%.2f", currentCandle.getOpenPrice())).append("\n");
        prompt.append("  - High:   ").append(String.format("%.2f", currentCandle.getHighPrice())).append("\n");
        prompt.append("  - Low:    ").append(String.format("%.2f", currentCandle.getLowPrice())).append("\n");
        prompt.append("  - Close:  ").append(String.format("%.2f", currentCandle.getClosePrice())).append("\n");
        prompt.append("  - Volume: ").append(currentCandle.getVolume()).append("\n\n");
        prompt.append("5-Day Price Trend: ").append(fiveDayTrend).append("\n");

        if (detectedPatterns.isEmpty()) {
            prompt.append("Detected Patterns: NONE\n");
            prompt.append("No candlestick patterns were identified from the current data.\n");
            prompt.append("\n");
            prompt.append("Using general technical analysis principles — including price action, momentum, ")
                  .append("volume behaviour, and intraday range analysis — evaluate the above data and provide ")
                  .append("a trading signal strictly based on what the numbers indicate.\n\n");
            prompt.append("Consider the following in your analysis:\n");
            prompt.append("  - Body-to-wick ratio and what it implies about buyer/seller control\n");
            prompt.append("  - Whether the close is in the upper, middle, or lower third of the day's range\n");
            prompt.append("  - Volume relative to the 5-day trend (high volume = conviction, low volume = weak signal)\n");
            prompt.append("  - Overall trend direction from the 5-day context\n\n");
        } else {
            prompt.append("Detected Patterns:\n");
            for (PatternDto pattern : detectedPatterns) {
                prompt.append("- ").append(pattern.getName())
                      .append(" (").append(pattern.getType().getSignalType())
                      .append(", confidence: ").append(String.format("%.1f%%", pattern.getConfidence() * 100))
                      .append("): ").append(pattern.getDescription())
                      .append("\n");
            }
            prompt.append("\nUsing the above pattern context, current OHLC data, and the 5-day trend, provide a trading signal.\n\n");
            prompt.append("Consider the following in your analysis:\n");
            prompt.append("  - Body-to-wick ratio and what it implies about buyer/seller control\n");
            prompt.append("  - Whether the close is in the upper, middle, or lower third of the day's range\n");
            prompt.append("  - Volume relative to the 5-day trend (high volume = conviction, low volume = weak signal)\n");
            prompt.append("  - Overall trend direction from the 5-day context\n\n");
        }

        prompt.append("Provide your response in EXACTLY this format with no additional text:\n\n");
        prompt.append("SIGNAL: [BUY/SELL/HOLD]\n");
        prompt.append("CONFIDENCE: [0-100]%\n");
        prompt.append("REASONING: [Max 50 words. Be specific — reference the actual price levels and volume.]\n");
        prompt.append("RISK_NOTE: [One sentence flagging the biggest uncertainty in this signal.]\n");

        logger.info("Constructed AI analysis prompt for symbol={}: {}", currentCandle.getSymbol(), prompt.toString());
        return prompt.toString();
    }

    private TradingSignalAnalysis parseAIResponse(String aiResponse) {
        try {
            String[] lines = aiResponse.trim().split("\n");
            String signal = "HOLD";
            double confidence = 50.0;
            String reasoning = "AI analysis inconclusive";
            String riskNote = "No risk note provided.";

            for (String line : lines) {
                if (line.startsWith("SIGNAL:")) {
                    signal = line.substring(7).trim().toUpperCase();
                } else if (line.startsWith("CONFIDENCE:")) {
                    String confStr = line.substring(11).replace("%", "").trim();
                    confidence = Double.parseDouble(confStr);
                } else if (line.startsWith("REASONING:")) {
                    reasoning = line.substring(10).trim();
                } else if (line.startsWith("RISK_NOTE:")) {
                    riskNote = line.substring(10).trim();
                }
            }

            // Validate signal
            if (!signal.equals("BUY") && !signal.equals("SELL") && !signal.equals("HOLD")) {
                signal = "HOLD";
            }

            // Validate confidence
            confidence = Math.max(0.0, Math.min(100.0, confidence));

            return new TradingSignalAnalysis(signal, confidence, reasoning, riskNote);

        } catch (Exception e) {
            logger.warn("Failed to parse AI response: {}", aiResponse);
            return new TradingSignalAnalysis("HOLD", 50.0, "Failed to parse AI analysis", "Unable to extract the risk note from the model output.");
        }
    }

    private TradingSignalAnalysis fallbackAnalysis(List<PatternDto> detectedPatterns) {
        logger.debug("Using fallback analysis for {} patterns", detectedPatterns.size());

        // Simple rule-based analysis based on pattern types
        int bullishCount = 0;
        int bearishCount = 0;
        double totalConfidence = 0.0;

        for (PatternDto pattern : detectedPatterns) {
            totalConfidence += pattern.getConfidence();
            String signalType = pattern.getType().getSignalType();

            if ("Bullish Reversal".equals(signalType)) {
                bullishCount++;
            } else if ("Bearish Reversal".equals(signalType)) {
                bearishCount++;
            }
        }

        double avgConfidence = detectedPatterns.isEmpty() ? 0.0 : totalConfidence / detectedPatterns.size();
        String signal;
        double confidence;

        if (bullishCount > bearishCount) {
            signal = "BUY";
            confidence = Math.min(85.0, 50.0 + (avgConfidence * 35.0));
        } else if (bearishCount > bullishCount) {
            signal = "SELL";
            confidence = Math.min(85.0, 50.0 + (avgConfidence * 35.0));
        } else {
            signal = "HOLD";
            confidence = 50.0 + (avgConfidence * 20.0);
        }

        String reasoning = String.format("Rule-based analysis: %d bullish, %d bearish patterns (avg confidence: %.1f%%)",
                                       bullishCount, bearishCount, avgConfidence * 100);
        String riskNote = detectedPatterns.isEmpty()
                ? "Signal is limited by absence of identified candlestick patterns."
                : "Signal may be less reliable without additional trend confirmation.";

        return new TradingSignalAnalysis(signal, confidence, reasoning, riskNote);
    }

    /**
     * Data class for trading signal analysis results.
     */
    public static class TradingSignalAnalysis {
        private final String signal;
        private final double confidence;
        private final String reasoning;
        private final String riskNote;

        public TradingSignalAnalysis(String signal, double confidence, String reasoning, String riskNote) {
            this.signal = signal;
            this.confidence = confidence;
            this.reasoning = reasoning;
            this.riskNote = riskNote;
        }

        public String getSignal() { return signal; }
        public double getConfidence() { return confidence; }
        public String getReasoning() { return reasoning; }
        public String getRiskNote() { return riskNote; }

        @Override
        public String toString() {
            return String.format("TradingSignal{signal='%s', confidence=%.1f%%, reasoning='%s', riskNote='%s'}",
                               signal, confidence, reasoning, riskNote);
        }
    }
}