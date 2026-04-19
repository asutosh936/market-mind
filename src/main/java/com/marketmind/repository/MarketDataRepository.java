package com.marketmind.repository;

import com.marketmind.model.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {
    List<MarketData> findBySymbolIgnoreCaseOrderByTimestampDesc(String symbol);
    List<MarketData> findBySymbolIgnoreCaseAndTimestampBetweenOrderByTimestampAsc(String symbol, LocalDateTime start, LocalDateTime end);
}