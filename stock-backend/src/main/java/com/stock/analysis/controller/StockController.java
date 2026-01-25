package com.stock.analysis.controller;

import com.stock.analysis.entity.Stock;
import com.stock.analysis.entity.StockBasic;
import com.stock.analysis.repository.StockBasicRepository;
import com.stock.analysis.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/stocks")
public class StockController {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockBasicRepository stockBasicRepository;

    @GetMapping
    public List<Stock> getAllStocks() {
        List<Stock> stocks = stockRepository.findByIsActiveTrue();
        for (Stock stock : stocks) {
            // 使用 code 关联
            Optional<StockBasic> basic = stockBasicRepository.findByCode(stock.getCode());
            basic.ifPresent(stockBasic -> stock.setName(stockBasic.getName()));
        }
        return stocks;
    }

    @PostMapping
    public Stock createStock(@RequestBody Stock stock) {
        // 1. 检查是否已存在该股票记录 (使用 code)
        Optional<Stock> existingStock = stockRepository.findByCode(stock.getCode());
        
        if (existingStock.isPresent()) {
            Stock existing = existingStock.get();
            if (Boolean.TRUE.equals(existing.getIsActive())) {
                Optional<StockBasic> basic = stockBasicRepository.findByCode(existing.getCode());
                basic.ifPresent(stockBasic -> existing.setName(stockBasic.getName()));
                return existing;
            } else {
                stockRepository.reactivateStock(existing.getId());
                existing.setIsActive(true);
                existing.setUpdatedAt(LocalDateTime.now());
                Optional<StockBasic> basic = stockBasicRepository.findByCode(existing.getCode());
                basic.ifPresent(stockBasic -> existing.setName(stockBasic.getName()));
                return existing;
            }
        }

        // 2. 创建新记录
        // 确保 symbol 存在 (如果前端只传了 code)
        if (stock.getSymbol() == null && stock.getCode() != null && stock.getCode().contains(".")) {
            stock.setSymbol(stock.getCode().split("\\.")[1]);
        }
        
        stock.setCreatedAt(LocalDateTime.now());
        stock.setUpdatedAt(LocalDateTime.now());
        stock.setIsActive(true);
        Stock savedStock = stockRepository.save(stock);
        
        Optional<StockBasic> basic = stockBasicRepository.findByCode(savedStock.getCode());
        basic.ifPresent(stockBasic -> savedStock.setName(stockBasic.getName()));
        
        return savedStock;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Stock> getStockById(@PathVariable Long id) {
        Optional<Stock> stockOptional = stockRepository.findById(id);
        if (stockOptional.isPresent()) {
            Stock stock = stockOptional.get();
            if (!Boolean.TRUE.equals(stock.getIsActive())) {
                return ResponseEntity.notFound().build();
            }
            Optional<StockBasic> basic = stockBasicRepository.findByCode(stock.getCode());
            basic.ifPresent(stockBasic -> stock.setName(stockBasic.getName()));
            return ResponseEntity.ok(stock);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Stock> updateStock(@PathVariable Long id, @RequestBody Stock stockDetails) {
        Optional<Stock> stockOptional = stockRepository.findById(id);
        if (stockOptional.isPresent()) {
            Stock stock = stockOptional.get();
            if (!Boolean.TRUE.equals(stock.getIsActive())) {
                return ResponseEntity.notFound().build();
            }
            
            // 更新字段
            if (stockDetails.getCode() != null) stock.setCode(stockDetails.getCode());
            if (stockDetails.getSymbol() != null) stock.setSymbol(stockDetails.getSymbol());
            if (stockDetails.getCurrentPrice() != null) stock.setCurrentPrice(stockDetails.getCurrentPrice());
            if (stockDetails.getChangePercent() != null) stock.setChangePercent(stockDetails.getChangePercent());
            if (stockDetails.getStrategy() != null) stock.setStrategy(stockDetails.getStrategy());
            
            stock.setTargetPrice(stockDetails.getTargetPrice());
            stock.setStopLoss(stockDetails.getStopLoss());
            stock.setNotes(stockDetails.getNotes());
            
            if (stockDetails.getConfidence() != null) stock.setConfidence(stockDetails.getConfidence());
            
            stock.setUpdatedAt(LocalDateTime.now());

            Stock updatedStock = stockRepository.save(stock);
            Optional<StockBasic> basic = stockBasicRepository.findByCode(updatedStock.getCode());
            basic.ifPresent(stockBasic -> updatedStock.setName(stockBasic.getName()));

            return ResponseEntity.ok(updatedStock);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStock(@PathVariable Long id) {
        if (stockRepository.existsById(id)) {
            stockRepository.softDeleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * 搜索股票接口
     */
    @GetMapping("/search")
    public List<StockBasic> searchStocks(@RequestParam String keyword) {
        // 限制返回数量，防止数据量过大
        List<StockBasic> results = stockBasicRepository.searchStocks(keyword);
        return results.stream().limit(10).toList();
    }

    @GetMapping("/test")
    public String testConnection() {
        return "后端API连接测试成功！当前时间: " + new java.util.Date();
    }
}