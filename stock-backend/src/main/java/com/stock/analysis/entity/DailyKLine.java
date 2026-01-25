package com.stock.analysis.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_k_lines", indexes = {
    @Index(name = "idx_code_date", columnList = "code, trade_date", unique = true)
})
@Data
public class DailyKLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 20)
    private String code; // 统一使用 code (sh.600000)

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "open_price", precision = 10, scale = 4)
    private BigDecimal openPrice;

    @Column(name = "high_price", precision = 10, scale = 4)
    private BigDecimal highPrice;

    @Column(name = "low_price", precision = 10, scale = 4)
    private BigDecimal lowPrice;

    @Column(name = "close_price", precision = 10, scale = 4)
    private BigDecimal closePrice;

    @Column(name = "pre_close_price", precision = 10, scale = 4)
    private BigDecimal preClosePrice;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "amount", precision = 20, scale = 4)
    private BigDecimal amount;

    @Column(name = "turnover_rate", precision = 10, scale = 4)
    private BigDecimal turnoverRate; // 换手率

    @Column(name = "pct_chg", precision = 10, scale = 4)
    private BigDecimal pctChg; // 涨跌幅

    @Column(name = "pe_ttm", precision = 10, scale = 4)
    private BigDecimal peTTM; // 滚动市盈率

    @Column(name = "pb_mrq", precision = 10, scale = 4)
    private BigDecimal pbMRQ; // 市净率
}