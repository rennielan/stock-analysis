package com.stock.analysis.controller;

import com.stock.analysis.entity.Stock;
import com.stock.analysis.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stocks")
public class StockController {
    
    @Autowired
    private StockRepository stockRepository;
    
    @GetMapping
    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }
    
    @PostMapping
    public Stock createStock(@RequestBody Stock stock) {
        return stockRepository.save(stock);
    }
    
    @GetMapping("/{id}")
    public Stock getStockById(@PathVariable Long id) {
        return stockRepository.findById(id).orElse(null);
    }
    
    @GetMapping("/test")
    public String testConnection() {
        return "数据库连接测试成功！当前时间: " + new java.util.Date();
    }
}