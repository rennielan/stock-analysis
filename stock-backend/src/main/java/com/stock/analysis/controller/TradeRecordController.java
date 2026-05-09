package com.stock.analysis.controller;

import com.stock.analysis.entity.Stock;
import com.stock.analysis.entity.StockBasic;
import com.stock.analysis.entity.StrategyType;
import com.stock.analysis.entity.TradeRecord;
import com.stock.analysis.repository.StockBasicRepository;
import com.stock.analysis.repository.StockRepository;
import com.stock.analysis.repository.TradeRecordRepository;
import com.stock.analysis.service.OcrService;
import com.stock.analysis.service.ExcelTradeService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/trades")
@CrossOrigin(origins = "*") // 允许前端跨域请求
public class TradeRecordController {

    private static final Logger logger = LoggerFactory.getLogger(TradeRecordController.class);

    @Autowired
    private OcrService ocrService;

    @Autowired
    private ExcelTradeService excelTradeService; // Excel 解析服务

    @Autowired
    private com.stock.analysis.service.HoldingExcelService holdingExcelService; // 持仓 Excel 解析服务

    @Autowired
    private TradeRecordRepository tradeRecordRepository;

    @Autowired
    private StockRepository stockRepository; // 注入 StockRepository

    @Autowired
    private StockBasicRepository stockBasicRepository; // 注入 StockBasicRepository

    @Autowired
    private EntityManager entityManager; // 注入 EntityManager 用于清理 Session

    /**
     * 上传 Excel 交割单文件
     */
    @PostMapping("/upload")
    @Transactional // 确保整个操作在一个事务中
    public ResponseEntity<?> uploadTradeFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "文件不能为空"));
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "只支持 Excel 文件格式（.xls 或 .xlsx）"));
        }

        try {
            logger.info("开始处理 Excel 文件: {}", fileName);
            
            // 1. 解析 Excel 文件
            List<TradeRecord> records = excelTradeService.parseTradeRecords(file);

            if (records.isEmpty()) {
                return ResponseEntity.ok(Map.of("message", "未从 Excel 文件中解析到任何交易记录", "savedCount", 0));
            }

            logger.info("成功解析 {} 条交易记录，开始保存...", records.size());

            // 2. 处理并保存记录
            List<TradeRecord> savedRecords = new ArrayList<>();
            int duplicateCount = 0;
            int newStockCount = 0;
            int filteredCount = 0; // 过滤掉的记录数
            
            for (TradeRecord record : records) {
                try {
                    // 过滤掉委托编号为0的记录
                    if (record.getOrderNumber() != null && record.getOrderNumber().equals("0")) {
                        logger.info("Filtering out record with order number 0: {} - {}", 
                                record.getStockName(), record.getStockCode());
                        filteredCount++;
                        continue; // 跳过这条记录
                    }
                    
                    // 根据股票代码和名称查找或创建 Stock
                    Stock stock = findOrCreateStock(record.getStockCode(), record.getStockName());
                    if (stock.getId() == null) { // 如果是新创建的股票
                        newStockCount++;
                    }
                    record.setStock(stock); // 关联 TradeRecord 和 Stock

                    // 保存 TradeRecord
                    savedRecords.add(tradeRecordRepository.save(record));
                } catch (DataIntegrityViolationException e) {
                    logger.warn("Skipping duplicate trade record - Order: {}, Stock: {} ({}): {}", 
                            record.getOrderNumber(), record.getStockName(), record.getStockCode(), e.getMessage());
                    duplicateCount++;
                    // 清除 Session 中失败的实体，避免影响后续操作
                    entityManager.detach(record);
                } catch (Exception e) {
                    logger.error("Failed to process or save trade record - Order: {}, Stock: {} ({}): {}", 
                            record.getOrderNumber(), record.getStockName(), record.getStockCode(), e);
                    // 清除 Session 中失败的实体，避免影响后续操作
                    entityManager.detach(record);
                }
            }

            // 3. 返回结果
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Excel 文件处理完成");
            response.put("totalParsed", records.size());
            response.put("savedCount", savedRecords.size());
            response.put("filteredCount", filteredCount); // 返回过滤掉的记录数
            response.put("duplicateCount", duplicateCount);
            response.put("newStockCount", newStockCount);
            response.put("records", savedRecords); // 返回保存的记录列表

            logger.info("Excel 文件处理完成：解析={}, 保存={}, 过滤={}, 重复={}, 新股={}",
                    records.size(), savedRecords.size(), filteredCount, duplicateCount, newStockCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing Excel file", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Excel 文件处理失败: " + e.getMessage()));
        }
    }

    /**
     * 上传持仓 Excel 文件，更新股票的参考持股和成本价
     */
    @PostMapping("/upload-holdings")
    @Transactional
    public ResponseEntity<?> uploadHoldingsFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "文件不能为空"));
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "只支持 Excel 文件格式（.xls 或 .xlsx）"));
        }

        try {
            logger.info("开始处理持仓 Excel 文件: {}", fileName);
            
            // 解析并更新持仓数据
            Map<String, Object> result = holdingExcelService.parseAndUpdateHoldings(file);
            
            logger.info("持仓 Excel 文件处理完成: {}", result);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error processing holdings Excel file", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "持仓 Excel 文件处理失败: " + e.getMessage()));
        }
    }

    /**
     * 根据股票代码和名称查找或创建 Stock 实体
     * @param stockCode 股票代码（可选）
     * @param stockName 股票名称
     * @return 找到或创建的 Stock 实体
     */
    private Stock findOrCreateStock(String stockCode, String stockName) {
        // 1. 如果有股票代码，先尝试通过代码查找
        if (stockCode != null && !stockCode.trim().isEmpty()) {
            Optional<Stock> existingStockByCode = stockRepository.findByCode(stockCode);
            if (existingStockByCode.isPresent()) {
                logger.debug("Found existing stock by code: {}", stockCode);
                return existingStockByCode.get();
            }
        }

        // 2. 尝试从 stock_basic 表中查找获取完整的 code 和 symbol
        String code = stockCode;
        String symbol = stockCode;

        Optional<StockBasic> stockBasic = stockBasicRepository.findBySymbol(stockCode);
        
        // 如果找到了，使用 stock_basic 中的 code 和 symbol
        if (stockBasic.isPresent()) {
            StockBasic basic = stockBasic.get();
            code = basic.getCode() != null ? basic.getCode() : stockCode;
            symbol = basic.getSymbol() != null ? basic.getSymbol() : stockCode;
            
            // 再次检查这个 code 是否已存在（避免重复创建）
            if (!code.equals(stockCode)) {
                Optional<Stock> existingStock = stockRepository.findByCode(code);
                if (existingStock.isPresent()) {
                    logger.debug("Found existing stock by stock_basic code: {}", code);
                    return existingStock.get();
                }
            }
        }

        // 3. 如果 stocks 表中不存在，则创建新的 Stock
        Stock newStock = new Stock();
        newStock.setName(stockName);
        newStock.setCode(code);
        newStock.setSymbol(symbol);
        newStock.setCurrentPrice(BigDecimal.ZERO);
        newStock.setChangePercent(BigDecimal.ZERO);
        newStock.setStrategy(StrategyType.WATCH);
        newStock.setConfidence(3);
        newStock.setIsActive(true);

        logger.info("Creating new stock entry: {} (code={}, symbol={})",
                stockName, code, symbol);
        
        try {
            return stockRepository.save(newStock);
        } catch (DataIntegrityViolationException e) {
            // 如果在保存时发生唯一约束冲突，说明有其他线程已经创建了该股票
            logger.warn("Stock {} already exists (concurrent creation), fetching existing record", code);
            final String finalCode = code; // 创建 final 副本用于 lambda 表达式
            return stockRepository.findByCode(finalCode)
                    .orElseThrow(() -> new RuntimeException("Failed to retrieve stock after constraint violation: " + finalCode));
        }
    }
    
    @GetMapping
    public ResponseEntity<List<TradeRecord>> getAllTrades() {
        // 按交易时间倒序返回（最新的在前）
        return ResponseEntity.ok(tradeRecordRepository.findAllByOrderByTradeTimeDesc());
    }
}