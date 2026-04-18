package com.marketmind.repository;

import com.marketmind.model.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class MarketDataRepositoryTest {

    @Autowired
    private MarketDataRepository repository;

    @BeforeEach
    public void setup() {
        repository.deleteAll();
    }

    @Test
    public void testFindAll_Empty() {
        List<MarketData> result = repository.findAll();
        assertThat(result).isEmpty();
    }

    @Test
    public void testFindAll_WithData() {
        MarketData data1 = new MarketData("AAPL", LocalDateTime.now(), 150.00, 155.00, 149.00, 154.50, 1000000);
        MarketData data2 = new MarketData("GOOGL", LocalDateTime.now(), 2800.00, 2850.00, 2790.00, 2840.00, 500000);
        repository.save(data1);
        repository.save(data2);

        List<MarketData> result = repository.findAll();
        assertThat(result).hasSize(2);
    }

    @Test
    public void testFindById_Existing() {
        MarketData data = new MarketData("MSFT", LocalDateTime.now(), 300.00, 310.00, 295.00, 305.00, 2000000);
        MarketData saved = repository.save(data);

        Optional<MarketData> result = repository.findById(saved.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo("MSFT");
    }

    @Test
    public void testFindById_NonExisting() {
        Optional<MarketData> result = repository.findById(999L);
        assertThat(result).isNotPresent();
    }

    @Test
    public void testSave_NewEntity() {
        MarketData data = new MarketData("TSLA", LocalDateTime.now(), 200.00, 210.00, 195.00, 205.00, 1500000);
        MarketData saved = repository.save(data);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSymbol()).isEqualTo("TSLA");
        assertThat(saved.getOpenPrice()).isEqualTo(200.00);
    }

    @Test
    public void testSave_UpdateExisting() {
        MarketData data = new MarketData("AMZN", LocalDateTime.now(), 3300.00, 3350.00, 3280.00, 3330.00, 3000000);
        MarketData saved = repository.save(data);

        saved.setClosePrice(3400.00);
        MarketData updated = repository.save(saved);

        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getClosePrice()).isEqualTo(3400.00);
    }

    @Test
    public void testDeleteById() {
        MarketData data = new MarketData("NVDA", LocalDateTime.now(), 400.00, 420.00, 395.00, 415.00, 800000);
        MarketData saved = repository.save(data);

        repository.deleteById(saved.getId());

        Optional<MarketData> result = repository.findById(saved.getId());
        assertThat(result).isNotPresent();
    }

    @Test
    public void testDeleteAll() {
        repository.save(new MarketData("TEST1", LocalDateTime.now(), 100.00, 110.00, 90.00, 105.00, 100000));
        repository.save(new MarketData("TEST2", LocalDateTime.now(), 200.00, 210.00, 190.00, 205.00, 200000));

        assertThat(repository.findAll()).hasSize(2);

        repository.deleteAll();

        assertThat(repository.findAll()).isEmpty();
    }
}