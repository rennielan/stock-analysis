package com.stock.analysis.service;

import com.stock.analysis.entity.Stock;
import com.stock.analysis.entity.StockBasic;
import com.stock.analysis.repository.StockRepository;
import com.stock.analysis.repository.StockBasicRepository;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
public class HoldingExcelService {

    private static final Logger logger = LoggerFactory.getLogger(HoldingExcelService.class);

    @Autowired
    private StockRepository stockRepository;
    
    @Autowired
    private StockBasicRepository stockBasicRepository;

    /**
     * 解析持仓 Excel 文件并更新股票的参考持股和成本价
     */
    @Transactional
    public Map<String, Object> parseAndUpdateHoldings(MultipartFile file) throws IOException {
        List<Map<String, Object>> updatedStocks = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        int notFoundCount = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            logger.info("开始处理持仓 Excel 文件，工作表: {}", sheet.getSheetName());

            // 构建列名映射
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("Excel 文件格式错误：未找到标题行");
            }

            Map<String, Integer> columnMap = buildColumnMapping(headerRow);
            logger.info("列映射: {}", columnMap);

            // 遍历数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                try {
                    Map<String, Object> result = processRow(row, columnMap, i + 1);
                    if (result != null) {
                        updatedStocks.add(result);
                        successCount++;
                    }
                } catch (Exception e) {
                    logger.error("处理第 {} 行失败: {}", i + 1, e.getMessage(), e);
                    failCount++;
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "持仓数据导入完成");
        response.put("successCount", successCount);
        response.put("failCount", failCount);
        response.put("notFoundCount", notFoundCount);
        response.put("updatedStocks", updatedStocks);

        logger.info("持仓数据导入完成：成功={}, 失败={}, 未找到={}", successCount, failCount, notFoundCount);
        return response;
    }

    /**
     * 构建列名到索引的映射
     */
    private Map<String, Integer> buildColumnMapping(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String columnName = getCellValueAsString(cell);
                if (columnName != null && !columnName.trim().isEmpty()) {
                    // 去除 BOM 字符、空格和特殊字符
                    String normalized = columnName.trim()
                        .replace("\ufeff", "")  // 去除 BOM
                        .replaceAll("\\s+", "");
                    columnMap.put(normalized, i);
                    logger.debug("列映射: {} -> {}", columnName, i);
                }
            }
        }

        return columnMap;
    }

    /**
     * 处理单行数据
     */
    private Map<String, Object> processRow(Row row, Map<String, Integer> columnMap, int rowNum) {
        try {
            // 获取股票代码
            String stockCode = getCellValueByNames(row, columnMap, 
                new String[]{"证券代码", "股票代码", "代码"});
            
            if (stockCode == null || stockCode.trim().isEmpty()) {
                logger.warn("第 {} 行：股票代码为空，跳过", rowNum);
                return null;
            }
            
            stockCode = stockCode.trim();
            
            // 获取股票名称
            String stockName = getCellValueByNames(row, columnMap,
                new String[]{"证券名称", "股票名称", "名称"});
            
            // 获取参考持股数量
            String sharesStr = getCellValueByNames(row, columnMap,
                new String[]{"参考持股", "持股数量", "持仓数量", "数量"});
            
            BigDecimal referenceShares = null;
            if (sharesStr != null && !sharesStr.trim().isEmpty()) {
                try {
                    referenceShares = new BigDecimal(sharesStr.trim().replaceAll(",", ""));
                } catch (NumberFormatException e) {
                    logger.warn("第 {} 行：参考持股格式错误: {}", rowNum, sharesStr);
                }
            }
            
            // 获取成本价
            String costPriceStr = getCellValueByNames(row, columnMap,
                new String[]{"成本价", "持仓成本", "成本价格", "买入成本"});
            
            BigDecimal costPrice = null;
            if (costPriceStr != null && !costPriceStr.trim().isEmpty()) {
                try {
                    costPrice = new BigDecimal(costPriceStr.trim().replaceAll(",", ""));
                } catch (NumberFormatException e) {
                    logger.warn("第 {} 行：成本价格式错误: {}", rowNum, costPriceStr);
                }
            }
            
            // 查找或创建股票
            Stock stock = findOrCreateStock(stockCode, stockName);
            
            // 更新参考持股和成本价
            boolean updated = false;
            if (referenceShares != null) {
                stock.setReferenceShares(referenceShares);
                updated = true;
            }
            if (costPrice != null) {
                stock.setCostPrice(costPrice);
                updated = true;
            }
            
            if (updated) {
                stockRepository.save(stock);
                logger.info("第 {} 行：成功更新股票 {} ({}) - 参考持股={}, 成本价={}", 
                    rowNum, stock.getName(), stock.getCode(), referenceShares, costPrice);
                
                Map<String, Object> result = new HashMap<>();
                result.put("id", stock.getId());
                result.put("code", stock.getCode());
                result.put("name", stock.getName());
                result.put("referenceShares", referenceShares);
                result.put("costPrice", costPrice);
                return result;
            } else {
                logger.warn("第 {} 行：没有可更新的数据", rowNum);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("处理第 {} 行时发生错误: {}", rowNum, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 根据股票代码查找或创建 Stock 实体
     * 优先使用 symbol 字段进行匹配
     */
    private Stock findOrCreateStock(String stockCode, String stockName) {
        // 1. 清理股票代码，提取纯数字部分作为 symbol
        String symbol = extractSymbol(stockCode);
        
        // 2. 尝试通过 symbol 查找
        Optional<Stock> existingStockBySymbol = stockRepository.findBySymbol(symbol);
        if (existingStockBySymbol.isPresent()) {
            logger.debug("通过 symbol 找到已存在的股票: {}", symbol);
            return existingStockBySymbol.get();
        }
        
        // 3. 尝试通过 code 查找（兼容旧数据）
        Optional<Stock> existingStockByCode = stockRepository.findByCode(stockCode);
        if (existingStockByCode.isPresent()) {
            logger.debug("通过 code 找到已存在的股票: {}", stockCode);
            return existingStockByCode.get();
        }
        
        // 4. 如果都不存在，从 stock_basic 表中查找获取完整信息
        Optional<StockBasic> stockBasicOpt = stockBasicRepository.findBySymbol(symbol);
        
        String code = stockCode;
        String name = stockName;
        
        if (stockBasicOpt.isPresent()) {
            StockBasic basic = stockBasicOpt.get();
            code = basic.getCode() != null ? basic.getCode() : stockCode;
            symbol = basic.getSymbol() != null ? basic.getSymbol() : symbol;
            name = basic.getName() != null ? basic.getName() : (stockName != null ? stockName : symbol);
            
            // 再次检查是否已存在
            Optional<Stock> existingStock = stockRepository.findByCode(code);
            if (existingStock.isPresent()) {
                logger.debug("通过 stock_basic 的 code 找到已存在的股票: {}", code);
                return existingStock.get();
            }
        }
        
        // 5. 创建新的 Stock
        Stock newStock = new Stock();
        newStock.setCode(code);
        newStock.setSymbol(symbol);
        newStock.setName(name != null ? name : symbol);
        newStock.setCurrentPrice(BigDecimal.ZERO);
        newStock.setChangePercent(BigDecimal.ZERO);
        newStock.setIsActive(true);
        
        logger.info("创建新股票: {} (code={}, symbol={})", 
            name != null ? name : symbol, code, symbol);
        return stockRepository.save(newStock);
    }
    
    /**
     * 从股票代码中提取纯数字 symbol
     * 例如: sh.600000 -> 600000, sz.000001 -> 000001
     */
    private String extractSymbol(String stockCode) {
        if (stockCode == null || stockCode.trim().isEmpty()) {
            return stockCode;
        }
        
        String code = stockCode.trim();
        
        // 如果包含点号，提取点号后面的部分
        if (code.contains(".")) {
            String[] parts = code.split("\\.");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        
        // 如果已经是纯数字，直接返回
        if (code.matches("\\d+")) {
            return code;
        }
        
        // 否则返回原值
        return code;
    }

    /**
     * 判断行是否为空
     */
    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 通过多个可能的列名获取单元格值
     */
    private String getCellValueByNames(Row row, Map<String, Integer> columnMap, String[] possibleNames) {
        for (String name : possibleNames) {
            // 尝试原始名称（去除 BOM）
            String normalizedName = name.replace("\ufeff", "");
            Integer colIndex = columnMap.get(normalizedName);
            if (colIndex != null) {
                Cell cell = row.getCell(colIndex);
                return getCellValueAsString(cell);
            }
            
            // 尝试去除空格后的名称
            String noSpaceName = normalizedName.replaceAll("\\s+", "");
            colIndex = columnMap.get(noSpaceName);
            if (colIndex != null) {
                Cell cell = row.getCell(colIndex);
                return getCellValueAsString(cell);
            }
        }
        return null;
    }

    /**
     * 将单元格值转换为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double value = cell.getNumericCellValue();
                    // 如果是整数，不显示小数点
                    if (value == Math.floor(value)) {
                        return String.valueOf((long) value);
                    }
                    return String.valueOf(value);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }
}
