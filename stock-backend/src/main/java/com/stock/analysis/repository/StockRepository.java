package com.stock.analysis.repository;

import com.stock.analysis.entity.Stock;
import com.stock.analysis.entity.StrategyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long>, JpaSpecificationExecutor<Stock> {
    
    // 1. 基础查询方法
    
    /**
     * 根据股票代码查找股票
     */
    Optional<Stock> findBySymbol(String symbol);
    
    /**
     * 根据股票代码列表查找股票
     */
    List<Stock> findBySymbolIn(List<String> symbols);
    
    /**
     * 根据策略类型查找股票
     */
    List<Stock> findByStrategy(StrategyType strategy);
    
    /**
     * 查找活跃的股票
     */
    List<Stock> findByIsActiveTrue();
    
    /**
     * 根据股票代码和活跃状态查找
     */
    Optional<Stock> findBySymbolAndIsActiveTrue(String symbol);
    
    // 2. 价格相关查询
    
    /**
     * 查找价格大于指定值的股票
     */
    List<Stock> findByCurrentPriceGreaterThan(BigDecimal price);
    
    /**
     * 查找价格小于指定值的股票
     */
    List<Stock> findByCurrentPriceLessThan(BigDecimal price);
    
    /**
     * 查找价格在指定范围内的股票
     */
    List<Stock> findByCurrentPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    /**
     * 查找涨跌幅大于指定值的股票
     */
    List<Stock> findByChangePercentGreaterThan(BigDecimal percent);
    
    /**
     * 查找涨跌幅小于指定值的股票
     */
    List<Stock> findByChangePercentLessThan(BigDecimal percent);
    
    // 3. 复杂查询（使用@Query注解）
    
    /**
     * 根据信心指数查找股票（信心指数大于等于指定值）
     */
    @Query("SELECT s FROM Stock s WHERE s.confidence >= :confidence AND s.isActive = true")
    List<Stock> findStocksWithHighConfidence(@Param("confidence") Integer confidence);
    
    /**
     * 查找有目标价格的股票
     */
    @Query("SELECT s FROM Stock s WHERE s.targetPrice IS NOT NULL AND s.isActive = true")
    List<Stock> findStocksWithTargetPrice();
    
    /**
     * 查找有止损价格的股票
     */
    @Query("SELECT s FROM Stock s WHERE s.stopLoss IS NOT NULL AND s.isActive = true")
    List<Stock> findStocksWithStopLoss();
    
    /**
     * 根据股票代码前缀查找（模糊查询）
     */
    @Query("SELECT s FROM Stock s WHERE s.symbol LIKE :symbolPrefix% AND s.isActive = true")
    List<Stock> findBySymbolStartingWith(@Param("symbolPrefix") String symbolPrefix);
    
    /**
     * 统计不同策略类型的股票数量
     */
    @Query("SELECT s.strategy, COUNT(s) FROM Stock s WHERE s.isActive = true GROUP BY s.strategy")
    List<Object[]> countStocksByStrategy();
    
    /**
     * 查找最近创建的股票
     */
    @Query("SELECT s FROM Stock s WHERE s.isActive = true ORDER BY s.createdAt DESC")
    List<Stock> findRecentStocks();
    
    /**
     * 查找需要更新的股票（最近未更新的活跃股票）
     * 使用参数传递时间阈值，而不是直接在JPQL中进行时间运算
     */
    @Query("SELECT s FROM Stock s WHERE s.isActive = true AND s.updatedAt < :thresholdTime")
    List<Stock> findStocksNeedingUpdate(@Param("thresholdTime") LocalDateTime thresholdTime);
    
    // 4. 更新操作
    
    /**
     * 更新股票价格
     */
    @Query("UPDATE Stock s SET s.currentPrice = :price, s.changePercent = :changePercent, s.updatedAt = CURRENT_TIMESTAMP WHERE s.symbol = :symbol AND s.isActive = true")
    int updateStockPrice(@Param("symbol") String symbol, @Param("price") BigDecimal price, @Param("changePercent") BigDecimal changePercent);
    
    /**
     * 软删除股票（设置isActive为false）
     */
    @Query("UPDATE Stock s SET s.isActive = false, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :id")
    int softDeleteById(@Param("id") Long id);
    
    /**
     * 批量软删除股票
     */
    @Query("UPDATE Stock s SET s.isActive = false, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id IN :ids")
    int softDeleteByIds(@Param("ids") List<Long> ids);
    
    // 5. 统计查询
    
    /**
     * 统计活跃股票数量
     */
    long countByIsActiveTrue();
    
    /**
     * 统计特定策略的活跃股票数量
     */
    long countByStrategyAndIsActiveTrue(StrategyType strategy);
    
    /**
     * 检查股票代码是否存在
     */
    boolean existsBySymbolAndIsActiveTrue(String symbol);
}