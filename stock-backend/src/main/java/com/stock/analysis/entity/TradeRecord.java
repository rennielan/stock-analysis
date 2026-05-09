package com.stock.analysis.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_records")
@Data
@ToString(exclude = "stock")
public class TradeRecord {

    @Id
    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber; // 委托编号（主键）

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode; // 证券代码

    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName; // 证券名称

    @Column(name = "settlement_date", length = 20)
    private String settlementDate; // 交收日期

    @Column(name = "business_name", length = 50)
    private String businessName; // 业务名称（证券买入、证券卖出等）

    @Column(name = "trade_price", nullable = false, precision = 10, scale = 4)
    private BigDecimal tradePrice; // 成交价格

    @Column(name = "quantity", nullable = false)
    private Integer quantity; // 成交数量

    @Column(name = "trade_amount", precision = 16, scale = 2)
    private BigDecimal tradeAmount; // 成交金额

    @Column(name = "commission", precision = 10, scale = 2)
    private BigDecimal commission; // 手续费

    @Column(name = "stamp_tax", precision = 10, scale = 2)
    private BigDecimal stampTax; // 印花税

    @Column(name = "transfer_fee", precision = 10, scale = 2)
    private BigDecimal transferFee; // 过户费

    @Column(name = "additional_fee", precision = 10, scale = 2)
    private BigDecimal additionalFee; // 附加费

    @Column(name = "exchange_clearing_fee", precision = 10, scale = 2)
    private BigDecimal exchangeClearingFee; // 交易所清算费

    @Column(name = "fund_commission", precision = 10, scale = 2)
    private BigDecimal fundCommission; // 基金手续费

    @Column(name = "regulatory_fee", precision = 10, scale = 2)
    private BigDecimal regulatoryFee; // 规费

    @Column(name = "exchange_difference", precision = 10, scale = 2)
    private BigDecimal exchangeDifference; // 换汇尾差

    @Column(name = "clearing_amount", precision = 16, scale = 2)
    private BigDecimal clearingAmount; // 清算金额

    @Column(name = "fund_balance", precision = 16, scale = 2)
    private BigDecimal fundBalance; // 资金本次余额

    @Column(name = "settlement_flag", length = 20)
    private String settlementFlag; // 交收标志

    @Column(name = "trade_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime tradeTime; // 成交时间

    @Column(name = "shareholder_code", length = 50)
    private String shareholderCode; // 股东代码

    @Column(name = "fund_account", length = 50)
    private String fundAccount; // 资金账号

    @Column(name = "customer_code", length = 50)
    private String customerCode; // 客户代码

    @Column(name = "currency", length = 20)
    private String currency; // 币种

    @Column(name = "exchange_name", length = 50)
    private String exchangeName; // 交易所名称

    @Column(name = "created_at", updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @JsonIgnore
    private LocalDateTime createdAt;

    @JsonIgnore // 防止 JSON 序列化时发生循环引用
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id")
    private Stock stock;
}