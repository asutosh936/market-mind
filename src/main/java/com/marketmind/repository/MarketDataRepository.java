package com.marketmind.repository;

import com.marketmind.model.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {
    // Custom query methods can be added here if needed
}