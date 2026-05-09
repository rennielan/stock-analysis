package com.stock.analysis.repository;

import com.stock.analysis.entity.StockHoldingView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 持仓盈亏视图 Repository
 * 用于查询包含衍生字段的股票数据
 */
@Repository
public interface StockHoldingViewRepository extends JpaRepository<StockHoldingView, Long> {
    
    /**
     * 查询所有活跃的持仓股票（包含衍生字段）
     */
    List<StockHoldingView> findByIsActiveTrue();
}
