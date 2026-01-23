package com.stock.analysis.controller;

import com.stock.analysis.entity.Stock;
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

    @GetMapping
    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    @PostMapping
    public Stock createStock(@RequestBody Stock stock) {
        stock.setCreatedAt(LocalDateTime.now());
        stock.setUpdatedAt(LocalDateTime.now());
        stock.setIsActive(true);
        return stockRepository.save(stock);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Stock> getStockById(@PathVariable Long id) {
        Optional<Stock> stock = stockRepository.findById(id);
        return stock.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Stock> updateStock(@PathVariable Long id, @RequestBody Stock stockDetails) {
        Optional<Stock> stockOptional = stockRepository.findById(id);
        if (stockOptional.isPresent()) {
            Stock stock = stockOptional.get();
            
            // 更新非空字段或全部字段 (根据业务需求，这里假设前端发送完整对象，但做防空处理更安全)
            if (stockDetails.getSymbol() != null) stock.setSymbol(stockDetails.getSymbol());
            if (stockDetails.getCurrentPrice() != null) stock.setCurrentPrice(stockDetails.getCurrentPrice());
            if (stockDetails.getChangePercent() != null) stock.setChangePercent(stockDetails.getChangePercent());
            if (stockDetails.getStrategy() != null) stock.setStrategy(stockDetails.getStrategy());
            
            // 允许为空的字段
            stock.setTargetPrice(stockDetails.getTargetPrice());
            stock.setStopLoss(stockDetails.getStopLoss());
            stock.setNotes(stockDetails.getNotes());
            
            if (stockDetails.getConfidence() != null) stock.setConfidence(stockDetails.getConfidence());
            
            stock.setUpdatedAt(LocalDateTime.now());

            Stock updatedStock = stockRepository.save(stock);
            return ResponseEntity.ok(updatedStock);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStock(@PathVariable Long id) {
        if (stockRepository.existsById(id)) {
            stockRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/test")
    public String testConnection() {
        return "后端API连接测试成功！当前时间: " + new java.util.Date();
    }
}