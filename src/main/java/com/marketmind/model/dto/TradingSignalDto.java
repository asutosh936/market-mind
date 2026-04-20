package com.marketmind.model.dto;

/**
 * DTO representing AI-generated trading signal analysis.
 * Contains buy/sell/hold recommendation with confidence percentage.
 */
public class TradingSignalDto {
    private String signal; // BUY, SELL, HOLD
    private double confidence; // 0.0 to 100.0
    private String reasoning;
    private String riskNote;

    public TradingSignalDto() {}

    public TradingSignalDto(String signal, double confidence, String reasoning) {
        this.signal = signal;
        this.confidence = Math.max(0.0, Math.min(100.0, confidence)); // Clamp between 0 and 100
        this.reasoning = reasoning;
        this.riskNote = "";
    }

    public TradingSignalDto(String signal, double confidence, String reasoning, String riskNote) {
        this.signal = signal;
        this.confidence = Math.max(0.0, Math.min(100.0, confidence)); // Clamp between 0 and 100
        this.reasoning = reasoning;
        this.riskNote = riskNote;
    }

    public String getSignal() {
        return signal;
    }

    public void setSignal(String signal) {
        this.signal = signal;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = Math.max(0.0, Math.min(100.0, confidence));
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public String getRiskNote() {
        return riskNote;
    }

    public void setRiskNote(String riskNote) {
        this.riskNote = riskNote;
    }

    @Override
    public String toString() {
        return "TradingSignalDto{" +
                "signal='" + signal + '\'' +
                ", confidence=" + String.format("%.1f%%", confidence) +
                ", reasoning='" + reasoning + '\'' +
                ", riskNote='" + riskNote + '\'' +
                '}';
    }
}