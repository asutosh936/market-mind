package com.marketmind.model.enums;

/**
 * Enum for candlestick pattern types.
 */
public enum PatternType {
    DOJI("Doji", "Neutral Reversal"),
    HAMMER("Hammer", "Bullish Reversal"),
    SHOOTING_STAR("Shooting Star", "Bearish Reversal"),
    BULLISH_ENGULFING("Bullish Engulfing", "Bullish Reversal"),
    BEARISH_ENGULFING("Bearish Engulfing", "Bearish Reversal"),
    MORNING_STAR("Morning Star", "Bullish Reversal"),
    EVENING_STAR("Evening Star", "Bearish Reversal");

    private final String displayName;
    private final String signalType;

    PatternType(String displayName, String signalType) {
        this.displayName = displayName;
        this.signalType = signalType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSignalType() {
        return signalType;
    }
}
