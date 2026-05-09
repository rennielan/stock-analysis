package com.stock.analysis.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持仓盈亏视图实体
 * 映射到 v_stock_holdings 视图（只读）
 * 用于查询包含衍生字段的股票数据
 */
@Entity
@Table(name = "v_stock_holdings")
@Data
@Immutable
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StockHoldingView {
    
    @Id
    private Long id;
    
    @Column(name = "code")
    private String code;
    
    @Column(name = "symbol")
    private String symbol;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "current_price")
    private BigDecimal currentPrice;
    
    @Column(name = "change_percent")
    private BigDecimal changePercent;
    
    @Column(name = "cost_price")
    private BigDecimal costPrice;
    
    @Column(name = "reference_shares")
    private BigDecimal referenceShares;
    
    @Column(name = "strategy")
    @Enumerated(EnumType.STRING)
    private StrategyType strategy;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    // 衍生字段（由视图计算）
    @Column(name = "market_value")
    private BigDecimal marketValue;
    
    @Column(name = "total_cost")
    private BigDecimal totalCost;
    
    @Column(name = "profit_loss")
    private BigDecimal profitLoss;
    
    @Column(name = "profit_loss_ratio")
    private BigDecimal profitLossRatio;
    
    @Column(name = "per_share_profit_loss")
    private BigDecimal perShareProfitLoss;
    
    @Column(name = "buy_price")
    private BigDecimal buyPrice;
    
    @Column(name = "target_price")
    private BigDecimal targetPrice;
    
    @Column(name = "stop_loss")
    private BigDecimal stopLoss;
    
    @Column(name = "confidence")
    private Integer confidence;
    
    @Column(name = "notes")
    private String notes;
    
    @Column(name = "created_at")
    @JsonIgnore
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @JsonIgnore
    private LocalDateTime updatedAt;
    
    // 注意：视图不支持关联关系，tradeRecords 需要通过其他方式获取
    // 如果需要交易记录，可以在前端单独调用接口获取
}
