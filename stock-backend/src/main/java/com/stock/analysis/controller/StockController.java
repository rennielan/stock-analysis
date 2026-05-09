package com.stock.analysis.controller;

import com.stock.analysis.dto.StockDetailDTO;
import com.stock.analysis.entity.Stock;
import com.stock.analysis.entity.StockHoldingView;
import com.stock.analysis.entity.StrategyType;
import com.stock.analysis.repository.StockHoldingViewRepository;
import com.stock.analysis.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/stocks")
@CrossOrigin(origins = "*")
public class StockController {

    private static final Logger logger = LoggerFactory.getLogger(StockController.class);

    @Autowired
    private StockRepository stockRepository;
    
    @Autowired
    private StockHoldingViewRepository stockHoldingViewRepository;

    @GetMapping
    public List<StockDetailDTO> getAllStocks() {
        // 1. 获取带有交易记录的股票数据（使用 @EntityGraph 预加载 tradeRecords）
        List<Stock> stocks = stockRepository.findByIsActiveTrue();
        
        // 2. 获取视图数据（包含市场价值、盈亏等衍生字段）
        List<StockHoldingView> views = stockHoldingViewRepository.findByIsActiveTrue();
        
        // 3. 建立视图 id -> StockHoldingView 映射，用于合并
        Map<Long, StockHoldingView> viewMap = views.stream()
                .collect(Collectors.toMap(StockHoldingView::getId, v -> v));
        
        // 4. 合并数据
        return stocks.stream().map(stock -> {
            StockHoldingView view = viewMap.get(stock.getId());
            StockDetailDTO dto = new StockDetailDTO();
            
            // 基础字段
            dto.setId(stock.getId());
            dto.setCode(stock.getCode());
            dto.setSymbol(stock.getSymbol());
            dto.setName(stock.getName());
            dto.setCurrentPrice(stock.getCurrentPrice());
            dto.setChangePercent(stock.getChangePercent());
            dto.setStrategy(stock.getStrategy());
            dto.setIsActive(stock.getIsActive());
            
            // 持仓信息（优先从视图取，如果视图不存在则从 Stock 实体取）
            dto.setCostPrice(view != null ? view.getCostPrice() : stock.getCostPrice());
            dto.setReferenceShares(view != null ? view.getReferenceShares() : stock.getReferenceShares());
            
            // 衍生字段（仅视图拥有）
            if (view != null) {
                dto.setMarketValue(view.getMarketValue());
                dto.setTotalCost(view.getTotalCost());
                dto.setProfitLoss(view.getProfitLoss());
                dto.setProfitLossRatio(view.getProfitLossRatio());
                dto.setPerShareProfitLoss(view.getPerShareProfitLoss());
            }
            
            // 用户可编辑字段
            dto.setBuyPrice(stock.getBuyPrice());
            dto.setTargetPrice(stock.getTargetPrice());
            dto.setStopLoss(stock.getStopLoss());
            dto.setConfidence(stock.getConfidence());
            dto.setNotes(stock.getNotes());
            
            // 时间
            dto.setCreatedAt(stock.getCreatedAt());
            dto.setUpdatedAt(stock.getUpdatedAt());
            
            // 交易记录
            dto.setTradeRecords(stock.getTradeRecords());
            
            return dto;
        }).collect(Collectors.toList());
    }

    @PostMapping
    public Stock createStock(@RequestBody Stock stock) {
        // 检查是否已存在相同 code 的股票
        if (stockRepository.findByCode(stock.getCode()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "股票代码已存在: " + stock.getCode());
        }
        stock.setIsActive(true); // 确保新创建的股票是活跃的
        stock.setCreatedAt(null); // 确保由数据库自动生成
        stock.setUpdatedAt(null); // 确保由数据库自动生成
        return stockRepository.save(stock);
    }

    @PutMapping("/{id}")
    public Stock updateStock(@PathVariable Long id, @RequestBody Stock updatedStock) {
        return stockRepository.findById(id)
                .map(stock -> {
                    stock.setBuyPrice(updatedStock.getBuyPrice());
                    stock.setTargetPrice(updatedStock.getTargetPrice());
                    stock.setStopLoss(updatedStock.getStopLoss());
                    stock.setConfidence(updatedStock.getConfidence());
                    stock.setNotes(updatedStock.getNotes());
                    stock.setStrategy(updatedStock.getStrategy());
                    // currentPrice 和 changePercent 由 ETL 更新，不通过此接口修改
                    // stock.setCurrentPrice(updatedStock.getCurrentPrice());
                    // stock.setChangePercent(updatedStock.getChangePercent());
                    return stockRepository.save(stock);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found with id " + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStock(@PathVariable Long id) {
        return stockRepository.findById(id)
                .map(stock -> {
                    stockRepository.softDeleteById(id); // 软删除
                    return ResponseEntity.ok().build();
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found with id " + id));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivateStock(@PathVariable Long id) {
        return stockRepository.findById(id)
                .map(stock -> {
                    stockRepository.reactivateStock(id);
                    return ResponseEntity.ok().build();
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found with id " + id));
    }

    // 搜索股票 (根据 code, symbol, name)
    @GetMapping("/search")
    public List<Stock> searchStocks(@RequestParam String keyword) {
        // 这里可以根据实际需求调整搜索逻辑，例如模糊匹配 code, symbol, name
        // 简单示例：直接使用 StockRepository 的 findAll 方法，然后过滤
        // 更好的做法是使用 Specification 或者自定义查询
        String lowerCaseKeyword = keyword.toLowerCase();
        return stockRepository.findAll().stream()
                .filter(stock -> stock.getCode().toLowerCase().contains(lowerCaseKeyword) ||
                                 stock.getSymbol().toLowerCase().contains(lowerCaseKeyword) ||
                                 stock.getName().toLowerCase().contains(lowerCaseKeyword))
                .toList();
    }
}