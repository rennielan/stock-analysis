package com.stock.analysis.repository;

import com.stock.analysis.entity.StockBasic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockBasicRepository extends JpaRepository<StockBasic, Long> {
    
    Optional<StockBasic> findByCode(String code);
    
    /**
     * 模糊搜索：匹配 code, symbol 或 name
     */
    @Query("SELECT s FROM StockBasic s WHERE s.code LIKE :keyword% OR s.symbol LIKE :keyword% OR s.name LIKE %:keyword%")
    List<StockBasic> searchStocks(@Param("keyword") String keyword);
}