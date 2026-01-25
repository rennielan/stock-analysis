package com.stock.analysis.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stocks", indexes = {
    @Index(name = "idx_stock_code", columnList = "code", unique = true)
})
@Data
public class Stock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "code", nullable = false, length = 20)
    private String code; // 唯一标识，如 sh.600000

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol; // 纯数字代码，如 600000

    @Transient // 不持久化到数据库，仅用于展示
    private String name;
    
    @Column(name = "current_price", nullable = false, precision = 10, scale = 4)
    private BigDecimal currentPrice;
    
    @Column(name = "change_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal changePercent;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", nullable = false)
    private StrategyType strategy = StrategyType.WATCH;
    
    @Column(name = "target_price", precision = 10, scale = 4)
    private BigDecimal targetPrice;
    
    @Column(name = "stop_loss", precision = 10, scale = 4)
    private BigDecimal stopLoss;
    
    @Column(name = "confidence", nullable = false)
    private Integer confidence = 3;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // 构造函数
    public Stock() {}
    
    public Stock(String code, String symbol, BigDecimal currentPrice, BigDecimal changePercent) {
        this.code = code;
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.changePercent = changePercent;
    }

    @Override
    public String toString() {
        return "Stock{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", currentPrice=" + currentPrice +
                ", changePercent=" + changePercent +
                ", strategy=" + strategy +
                ", targetPrice=" + targetPrice +
                ", stopLoss=" + stopLoss +
                ", confidence=" + confidence +
                ", isActive=" + isActive +
                '}';
    }
}