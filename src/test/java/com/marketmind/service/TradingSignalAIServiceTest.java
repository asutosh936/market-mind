package com.marketmind.service;

import com.marketmind.model.MarketData;
import com.marketmind.model.dto.PatternDto;
import com.marketmind.model.enums.PatternType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TradingSignalAIServiceTest {

    @Mock
    private ChatModel chatModel;

    @Captor
    private ArgumentCaptor<Prompt> promptCaptor;

    private TradingSignalAIService service;

    @BeforeEach
    void setUp() {
        service = new TradingSignalAIService();
        ReflectionTestUtils.setField(service, "chatModel", chatModel);
    }

    @Test
    void analyzePatternsWithEmptyPatterns_callsAiAndParsesResponse() {
        MarketData current = new MarketData("AAPL", LocalDateTime.parse("2026-04-18T10:00:00"), 150.00, 155.00, 149.00, 154.50, 1000000);
        ChatResponse aiResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage("SIGNAL: BUY\nCONFIDENCE: 87.5%\nREASONING: Strong close above prior range on higher volume supports upside continuation.\nRISK_NOTE: Watch for near-term overbought extensions due to stretched momentum."))));

        when(chatModel.call(any(Prompt.class))).thenReturn(aiResponse);

        TradingSignalAIService.TradingSignalAnalysis analysis = service.analyzePatternsForTradingSignal(current, "Uptrend (2.33% over 5 bars)", List.of());

        assertThat(analysis.getSignal()).isEqualTo("BUY");
        assertThat(analysis.getConfidence()).isEqualTo(87.5);
        assertThat(analysis.getRiskNote()).contains("near-term overbought extensions");

        verify(chatModel).call(promptCaptor.capture());
        assertThat(promptCaptor.getValue().getContents()).contains("Detected Patterns: NONE");
        assertThat(promptCaptor.getValue().getContents()).contains("SIGNAL: [BUY/SELL/HOLD]");
        assertThat(promptCaptor.getValue().getContents()).contains("REASONING: [Max 50 words");
    }

    @Test
    void analyzePatternsWithDetectedPatterns_includesPatternDetailsInPrompt() {
        MarketData current = new MarketData("AAPL", LocalDateTime.parse("2026-04-18T10:00:00"), 150.00, 155.00, 149.00, 154.50, 1000000);
        PatternDto pattern = new PatternDto("Shooting Star", PatternType.SHOOTING_STAR, 0.82, "Bearish reversal with long upper wick.");
        ChatResponse aiResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage("SIGNAL: SELL\nCONFIDENCE: 73.0%\nREASONING: Long upper wick and downtrend bias favour a pullback.\nRISK_NOTE: A sudden market reversal would invalidate the pattern."))));

        when(chatModel.call(any(Prompt.class))).thenReturn(aiResponse);

        TradingSignalAIService.TradingSignalAnalysis analysis = service.analyzePatternsForTradingSignal(current, "Downtrend (1.75% over 5 bars)", List.of(pattern));

        assertThat(analysis.getSignal()).isEqualTo("SELL");
        assertThat(analysis.getConfidence()).isEqualTo(73.0);
        assertThat(analysis.getReasoning()).contains("downtrend");
        assertThat(analysis.getRiskNote()).contains("market reversal");

        verify(chatModel).call(promptCaptor.capture());
        assertThat(promptCaptor.getValue().getContents()).contains("Shooting Star");
        assertThat(promptCaptor.getValue().getContents()).contains("Bearish Reversal");
    }

    @Test
    void analyzePatterns_fallsBackWhenChatModelThrows() {
        MarketData current = new MarketData("AAPL", LocalDateTime.parse("2026-04-18T10:00:00"), 150.00, 155.00, 149.00, 154.50, 1000000);
        PatternDto pattern = new PatternDto("Hammer", PatternType.HAMMER, 0.75, "Bullish reversal after consolidation.");

        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API unavailable"));

        TradingSignalAIService.TradingSignalAnalysis analysis = service.analyzePatternsForTradingSignal(current, "Sideways (0.00% over 5 bars)", List.of(pattern));

        assertThat(analysis.getSignal()).isEqualTo("BUY");
        assertThat(analysis.getRiskNote()).contains("less reliable");
    }

    @Test
    void analyzePatterns_returnsHoldWhenAiResponseCannotBeParsed() {
        MarketData current = new MarketData("AAPL", LocalDateTime.parse("2026-04-18T10:00:00"), 150.00, 155.00, 149.00, 154.50, 1000000);
        ChatResponse aiResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage("SIGNAL: BUY\nCONFIDENCE: ninety percent\nREASONING: Improper confidence format.\nRISK_NOTE: Force fallback parsing."))));

        when(chatModel.call(any(Prompt.class))).thenReturn(aiResponse);

        TradingSignalAIService.TradingSignalAnalysis analysis = service.analyzePatternsForTradingSignal(current, "Sideways (0.00% over 5 bars)", List.of());

        assertThat(analysis.getSignal()).isEqualTo("HOLD");
        assertThat(analysis.getReasoning()).contains("Failed to parse AI analysis");
        assertThat(analysis.getRiskNote()).contains("Unable to extract");
    }

    @Test
    void analyzePatterns_clampsConfidenceAndInvalidSignalToHold() {
        MarketData current = new MarketData("AAPL", LocalDateTime.parse("2026-04-18T10:00:00"), 150.00, 155.00, 149.00, 154.50, 1000000);
        ChatResponse aiResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage("SIGNAL: STRONG BUY\nCONFIDENCE: 150%\nREASONING: Excessive optimism detected.\n"))));

        when(chatModel.call(any(Prompt.class))).thenReturn(aiResponse);

        TradingSignalAIService.TradingSignalAnalysis analysis = service.analyzePatternsForTradingSignal(current, "Uptrend (2.33% over 5 bars)", List.of());

        assertThat(analysis.getSignal()).isEqualTo("HOLD");
        assertThat(analysis.getConfidence()).isEqualTo(100.0);
        assertThat(analysis.getRiskNote()).isEqualTo("No risk note provided.");
    }
}
