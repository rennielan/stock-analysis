package com.stock.analysis.repository;

import com.stock.analysis.entity.Stock;
import com.stock.analysis.entity.StrategyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long>, JpaSpecificationExecutor<Stock> {
    
    /**
     * 根据股票代码查找股票 (使用 code)
     */
    Optional<Stock> findByCode(String code);
    
    /**
     * 查找活跃的股票
     */
    List<Stock> findByIsActiveTrue();
    
    /**
     * 更新股票价格 (使用 code)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Stock s SET s.currentPrice = :price, s.changePercent = :changePercent, s.updatedAt = CURRENT_TIMESTAMP WHERE s.code = :code AND s.isActive = true")
    int updateStockPrice(@Param("code") String code, @Param("price") BigDecimal price, @Param("changePercent") BigDecimal changePercent);
    
    /**
     * 软删除股票
     */
    @Modifying
    @Transactional
    @Query("UPDATE Stock s SET s.isActive = false, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :id")
    void softDeleteById(@Param("id") Long id);
    
    /**
     * 重新激活股票
     */
    @Modifying
    @Transactional
    @Query("UPDATE Stock s SET s.isActive = true, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :id")
    void reactivateStock(@Param("id") Long id);
}