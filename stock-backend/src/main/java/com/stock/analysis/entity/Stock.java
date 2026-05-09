package com.stock.analysis.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stocks", indexes = {
    @Index(name = "idx_stock_code", columnList = "code", unique = true),
    @Index(name = "idx_stock_symbol", columnList = "symbol")
})
@Data
@ToString(exclude = "tradeRecords")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Stock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "code", nullable = false, length = 20)
    private String code; // 唯一标识，如 sh.600000

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol; // 纯数字代码，如 600000

    @Column(name = "name", length = 100)
    private String name;
    
    @Column(name = "current_price", nullable = false, precision = 10, scale = 4)
    private BigDecimal currentPrice;
    
    @Column(name = "change_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal changePercent;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", nullable = false)
    private StrategyType strategy = StrategyType.WATCH;
    
    @Column(name = "buy_price", precision = 10, scale = 4)
    private BigDecimal buyPrice; // 买入价

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

    @Column(name = "reference_shares", precision = 16, scale = 2)
    private BigDecimal referenceShares; // 参考持股数量

    @Column(name = "cost_price", precision = 10, scale = 4)
    private BigDecimal costPrice; // 成本价

    @OneToMany(mappedBy = "stock", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("tradeTime DESC")
    private List<TradeRecord> tradeRecords = new ArrayList<>();
    
    // 构造函数
    public Stock() {}
    
    public Stock(String code, String symbol, BigDecimal currentPrice, BigDecimal changePercent) {
        this.code = code;
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.changePercent = changePercent;
    }
}