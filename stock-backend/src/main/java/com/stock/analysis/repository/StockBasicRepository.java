package com.stock.analysis.repository;

import com.stock.analysis.entity.StockBasic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockBasicRepository extends JpaRepository<StockBasic, Long> {
    Optional<StockBasic> findBySymbol(String symbol);
}