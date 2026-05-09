package com.stock.analysis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.stock.analysis.entity.StrategyType;
import com.stock.analysis.entity.TradeRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 股票详情 DTO
 * 包含持仓视图的衍生字段 + 交易记录列表
 */
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StockDetailDTO {

    private Long id;
    private String code;
    private String symbol;
    private String name;
    private BigDecimal currentPrice;
    private BigDecimal changePercent;
    private StrategyType strategy;
    private Boolean isActive;

    // 持仓信息
    private BigDecimal costPrice;
    private BigDecimal referenceShares;

    // 衍生字段（由视图计算）
    private BigDecimal marketValue;
    private BigDecimal totalCost;
    private BigDecimal profitLoss;
    private BigDecimal profitLossRatio;
    private BigDecimal perShareProfitLoss;

    // 用户可编辑字段
    private BigDecimal buyPrice;
    private BigDecimal targetPrice;
    private BigDecimal stopLoss;
    private Integer confidence;
    private String notes;

    // 时间
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // 交易记录
    private List<TradeRecord> tradeRecords;

    public StockDetailDTO() {}

    public StockDetailDTO(Long id, String code, String symbol, String name,
                          BigDecimal currentPrice, BigDecimal changePercent,
                          StrategyType strategy, Boolean isActive,
                          BigDecimal costPrice, BigDecimal referenceShares,
                          BigDecimal marketValue, BigDecimal totalCost,
                          BigDecimal profitLoss, BigDecimal profitLossRatio,
                          BigDecimal perShareProfitLoss,
                          BigDecimal buyPrice, BigDecimal targetPrice,
                          BigDecimal stopLoss, Integer confidence, String notes,
                          LocalDateTime createdAt, LocalDateTime updatedAt,
                          List<TradeRecord> tradeRecords) {
        this.id = id;
        this.code = code;
        this.symbol = symbol;
        this.name = name;
        this.currentPrice = currentPrice;
        this.changePercent = changePercent;
        this.strategy = strategy;
        this.isActive = isActive;
        this.costPrice = costPrice;
        this.referenceShares = referenceShares;
        this.marketValue = marketValue;
        this.totalCost = totalCost;
        this.profitLoss = profitLoss;
        this.profitLossRatio = profitLossRatio;
        this.perShareProfitLoss = perShareProfitLoss;
        this.buyPrice = buyPrice;
        this.targetPrice = targetPrice;
        this.stopLoss = stopLoss;
        this.confidence = confidence;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.tradeRecords = tradeRecords;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public BigDecimal getChangePercent() { return changePercent; }
    public void setChangePercent(BigDecimal changePercent) { this.changePercent = changePercent; }

    public StrategyType getStrategy() { return strategy; }
    public void setStrategy(StrategyType strategy) { this.strategy = strategy; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public BigDecimal getReferenceShares() { return referenceShares; }
    public void setReferenceShares(BigDecimal referenceShares) { this.referenceShares = referenceShares; }

    public BigDecimal getMarketValue() { return marketValue; }
    public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public BigDecimal getProfitLoss() { return profitLoss; }
    public void setProfitLoss(BigDecimal profitLoss) { this.profitLoss = profitLoss; }

    public BigDecimal getProfitLossRatio() { return profitLossRatio; }
    public void setProfitLossRatio(BigDecimal profitLossRatio) { this.profitLossRatio = profitLossRatio; }

    public BigDecimal getPerShareProfitLoss() { return perShareProfitLoss; }
    public void setPerShareProfitLoss(BigDecimal perShareProfitLoss) { this.perShareProfitLoss = perShareProfitLoss; }

    public BigDecimal getBuyPrice() { return buyPrice; }
    public void setBuyPrice(BigDecimal buyPrice) { this.buyPrice = buyPrice; }

    public BigDecimal getTargetPrice() { return targetPrice; }
    public void setTargetPrice(BigDecimal targetPrice) { this.targetPrice = targetPrice; }

    public BigDecimal getStopLoss() { return stopLoss; }
    public void setStopLoss(BigDecimal stopLoss) { this.stopLoss = stopLoss; }

    public Integer getConfidence() { return confidence; }
    public void setConfidence(Integer confidence) { this.confidence = confidence; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<TradeRecord> getTradeRecords() { return tradeRecords; }
    public void setTradeRecords(List<TradeRecord> tradeRecords) { this.tradeRecords = tradeRecords; }
}
