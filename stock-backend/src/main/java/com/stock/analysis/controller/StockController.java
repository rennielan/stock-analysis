package com.stock.analysis.controller;

import com.stock.analysis.entity.DailyKLine;
import com.stock.analysis.entity.Stock;
import com.stock.analysis.entity.StockBasic;
import com.stock.analysis.repository.DailyKLineRepository;
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

    @Autowired
    private DailyKLineRepository dailyKLineRepository;

    @GetMapping
    public List<Stock> getAllStocks() {
        List<Stock> stocks = stockRepository.findByIsActiveTrue();
        for (Stock stock : stocks) {
            Optional<StockBasic> basic = stockBasicRepository.findByCode(stock.getCode());
            basic.ifPresent(stockBasic -> stock.setName(stockBasic.getName()));
        }
        return stocks;
    }

    @PostMapping
    public Stock createStock(@RequestBody Stock stock) {
        Optional<Stock> existingStock = stockRepository.findByCode(stock.getCode());
        
        if (stock.getSymbol() == null && stock.getCode() != null && stock.getCode().contains(".")) {
            stock.setSymbol(stock.getCode().split("\\.")[1]);
        }

        Optional<DailyKLine> latestKLine = dailyKLineRepository.findLatestByCode(stock.getCode());
        if (latestKLine.isPresent()) {
            DailyKLine kLine = latestKLine.get();
            stock.setCurrentPrice(kLine.getClosePrice());
            stock.setChangePercent(kLine.getPctChg());
        }
        
        if (existingStock.isPresent()) {
            Stock existing = existingStock.get();
            
            if (latestKLine.isPresent()) {
                DailyKLine kLine = latestKLine.get();
                existing.setCurrentPrice(kLine.getClosePrice());
                existing.setChangePercent(kLine.getPctChg());
            }

            if (Boolean.TRUE.equals(existing.getIsActive())) {
                stockRepository.save(existing);
                Optional<StockBasic> basic = stockBasicRepository.findByCode(existing.getCode());
                basic.ifPresent(stockBasic -> existing.setName(stockBasic.getName()));
                return existing;
            } else {
                existing.setIsActive(true);
                existing.setUpdatedAt(LocalDateTime.now());
                stockRepository.save(existing);
                Optional<StockBasic> basic = stockBasicRepository.findByCode(existing.getCode());
                basic.ifPresent(stockBasic -> existing.setName(stockBasic.getName()));
                return existing;
            }
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
            
            // 关键修改：只更新用户可编辑的字段，忽略价格等由ETL维护的字段
            // 这样可以防止前端旧数据覆盖ETL的新数据
            
            // 策略相关
            if (stockDetails.getStrategy() != null) stock.setStrategy(stockDetails.getStrategy());
            
            // 价格计划相关 (允许更新为 null，如果前端传了 null)
            // 注意：这里假设前端传的是全量用户编辑字段。如果前端传 null，意味着用户清空了该值。
            // 如果前端没传（即为 null），也会被清空吗？
            // 由于我们现在是全量提交（StockCard 提交完整的 localData），所以 stockDetails 中的字段就是用户当前看到的。
            // 如果用户清空了输入框，前端传 null，这里就应该设为 null。
            
            stock.setBuyPrice(stockDetails.getBuyPrice());
            stock.setTargetPrice(stockDetails.getTargetPrice());
            stock.setStopLoss(stockDetails.getStopLoss());
            stock.setNotes(stockDetails.getNotes());
            
            if (stockDetails.getConfidence() != null) stock.setConfidence(stockDetails.getConfidence());
            
            // 忽略 currentPrice, changePercent, code, symbol 等字段的更新
            
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
    
    @GetMapping("/search")
    public List<StockBasic> searchStocks(@RequestParam String keyword) {
        List<StockBasic> results = stockBasicRepository.searchStocks(keyword);
        return results.stream().limit(10).toList();
    }

    @GetMapping("/test")
    public String testConnection() {
        return "后端API连接测试成功！当前时间: " + new java.util.Date();
    }
}