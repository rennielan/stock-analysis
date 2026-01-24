package com.stock.analysis.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "stock_basics", indexes = {
    @Index(name = "idx_code", columnList = "code", unique = true)
})
@Data
public class StockBasic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 20, unique = true)
    private String code;

    @Column(name = "symbol", length = 20)
    private String symbol;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "ipo_date")
    private LocalDate ipoDate;

    @Column(name = "out_date")
    private LocalDate outDate;

    @Column(name = "type", length = 10)
    private String type;

    @Column(name = "status", length = 10)
    private String status;
}