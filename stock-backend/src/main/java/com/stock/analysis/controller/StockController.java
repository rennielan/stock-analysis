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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/stocks")
public class StockController {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockBasicRepository stockBasicRepository;

    @GetMapping
    public List<Stock> getAllStocks() {
        // 修改为只查询活跃的股票
        List<Stock> stocks = stockRepository.findByIsActiveTrue();
        
        // 批量获取股票名称
        for (Stock stock : stocks) {
            Optional<StockBasic> basic = stockBasicRepository.findBySymbol(stock.getSymbol());
            basic.ifPresent(stockBasic -> stock.setName(stockBasic.getName()));
        }
        
        return stocks;
    }

    @PostMapping
    public Stock createStock(@RequestBody Stock stock) {
        stock.setCreatedAt(LocalDateTime.now());
        stock.setUpdatedAt(LocalDateTime.now());
        stock.setIsActive(true);
        Stock savedStock = stockRepository.save(stock);
        
        // 填充名称返回
        Optional<StockBasic> basic = stockBasicRepository.findBySymbol(savedStock.getSymbol());
        basic.ifPresent(stockBasic -> savedStock.setName(stockBasic.getName()));
        
        return savedStock;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Stock> getStockById(@PathVariable Long id) {
        Optional<Stock> stockOptional = stockRepository.findById(id);
        if (stockOptional.isPresent()) {
            Stock stock = stockOptional.get();
            Optional<StockBasic> basic = stockBasicRepository.findBySymbol(stock.getSymbol());
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
            
            // 填充名称
            Optional<StockBasic> basic = stockBasicRepository.findBySymbol(updatedStock.getSymbol());
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

    @GetMapping("/test")
    public String testConnection() {
        return "后端API连接测试成功！当前时间: " + new java.util.Date();
    }
}