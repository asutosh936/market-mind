package com.marketmind.service;

import com.marketmind.model.MarketData;
import com.marketmind.repository.MarketDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class MarketDataServiceTest {

    @Autowired
    private MarketDataService service;

    @Autowired
    private MarketDataRepository repository;

    @BeforeEach
    public void setup() {
        repository.deleteAll(); // Clean up before each test
    }

    @Test
    public void testFindAll_EmptyList() {
        List<MarketData> result = service.findAll();
        assertThat(result).isEmpty();
    }

    @Test
    public void testFindAll_WithData() {
        // Create test data
        MarketData data1 = new MarketData("AAPL", LocalDateTime.now(), 150.00, 155.00, 149.00, 154.50, 1000000);
        MarketData data2 = new MarketData("GOOGL", LocalDateTime.now(), 2800.00, 2850.00, 2790.00, 2840.00, 500000);
        repository.save(data1);
        repository.save(data2);

        List<MarketData> result = service.findAll();
        assertThat(result).hasSize(2);
        assertThat(result).extracting("symbol").contains("AAPL", "GOOGL");
    }

    @Test
    public void testFindById_Existing() {
        MarketData data = new MarketData("MSFT", LocalDateTime.now(), 300.00, 310.00, 295.00, 305.00, 2000000);
        MarketData saved = repository.save(data);

        Optional<MarketData> result = service.findById(saved.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo("MSFT");
        assertThat(result.get().getClosePrice()).isEqualTo(305.00);
    }

    @Test
    public void testFindById_NonExisting() {
        Optional<MarketData> result = service.findById(999L);
        assertThat(result).isNotPresent();
    }

    @Test
    public void testSave_NewEntity() {
        MarketData data = new MarketData("TSLA", LocalDateTime.now(), 200.00, 210.00, 195.00, 205.00, 1500000);
        MarketData saved = service.save(data);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSymbol()).isEqualTo("TSLA");
        assertThat(saved.getOpenPrice()).isEqualTo(200.00);

        // Verify it's in the database
        Optional<MarketData> retrieved = repository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getSymbol()).isEqualTo("TSLA");
    }

    @Test
    public void testSave_UpdateExisting() {
        MarketData data = new MarketData("AMZN", LocalDateTime.now(), 3300.00, 3350.00, 3280.00, 3330.00, 3000000);
        MarketData saved = repository.save(data);

        // Update the data
        saved.setClosePrice(3400.00);
        saved.setVolume(3500000);
        MarketData updated = service.save(saved);

        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getClosePrice()).isEqualTo(3400.00);
        assertThat(updated.getVolume()).isEqualTo(3500000);
    }

    @Test
    public void testDeleteById_Existing() {
        MarketData data = new MarketData("NVDA", LocalDateTime.now(), 400.00, 420.00, 395.00, 415.00, 800000);
        MarketData saved = repository.save(data);

        service.deleteById(saved.getId());

        Optional<MarketData> result = repository.findById(saved.getId());
        assertThat(result).isNotPresent();
    }

    @Test
    public void testDeleteById_NonExisting() {
        // Should not throw exception
        service.deleteById(999L);

        // Verify no data was affected
        List<MarketData> all = service.findAll();
        assertThat(all).isEmpty();
    }

    @Test
    public void testFindBySymbol() {
        repository.save(new MarketData("CRM", LocalDateTime.parse("2026-04-18T09:00:00"), 250.00, 255.00, 248.00, 252.00, 400000));
        repository.save(new MarketData("CRM", LocalDateTime.parse("2026-04-18T10:00:00"), 252.00, 258.00, 251.00, 256.00, 420000));
        repository.save(new MarketData("ORCL", LocalDateTime.parse("2026-04-18T10:00:00"), 80.00, 83.00, 79.00, 82.00, 300000));

        List<MarketData> results = service.findBySymbol("CRM");
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTimestamp()).isAfterOrEqualTo(results.get(1).getTimestamp());
    }

    @Test
    public void testFindBySymbolAndRange() {
        repository.save(new MarketData("CRM", LocalDateTime.parse("2026-04-18T09:00:00"), 250.00, 255.00, 248.00, 252.00, 400000));
        repository.save(new MarketData("CRM", LocalDateTime.parse("2026-04-18T10:00:00"), 252.00, 258.00, 251.00, 256.00, 420000));
        repository.save(new MarketData("CRM", LocalDateTime.parse("2026-04-18T11:00:00"), 256.00, 260.00, 255.00, 259.00, 430000));

        List<MarketData> results = service.findBySymbolAndRange(
                "CRM",
                LocalDateTime.parse("2026-04-18T09:30:00"),
                LocalDateTime.parse("2026-04-18T10:30:00")
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTimestamp()).isEqualTo(LocalDateTime.parse("2026-04-18T10:00:00"));
    }
}