package com.stock.analysis.service;

import com.stock.analysis.entity.TradeRecord;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExcelTradeService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelTradeService.class);

    /**
     * 从 Excel 文件中解析交易记录（动态识别列名）
     * @param file Excel 文件（.xls 或 .xlsx）
     * @return 交易记录列表
     */
    public List<TradeRecord> parseTradeRecords(MultipartFile file) throws IOException {
        List<TradeRecord> records = new ArrayList<>();
        
        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0); // 读取第一个工作表
            
            logger.info("开始解析 Excel 文件，总行数: {}", sheet.getPhysicalNumberOfRows());
            
            // 读取标题行并建立列名到索引的映射
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("Excel 文件格式错误：未找到标题行");
            }
            
            Map<String, Integer> columnMap = buildColumnMapping(headerRow);
            logger.info("识别到的列映射: {}", columnMap.keySet());
            logger.info("详细列映射: {}", columnMap);
            
            // 跳过表头行，从第二行开始读取数据
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                
                try {
                    TradeRecord record = parseRow(row, columnMap, i + 1);
                    if (record != null) {
                        records.add(record);
                    }
                } catch (Exception e) {
                    logger.error("解析第 {} 行失败: {}", i + 1, e.getMessage(), e);
                }
            }
            
            logger.info("成功解析 {} 条交易记录", records.size());
        }
        
        return records;
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
     * 解析单行数据（使用列名映射）
     */
    private TradeRecord parseRow(Row row, Map<String, Integer> columnMap, int rowNum) {
        TradeRecord record = new TradeRecord();
        
        try {
            // 委托编号（主键）- 尝试多个可能的列名
            String orderNumber = getCellValueByNames(row, columnMap, 
                new String[]{"委托编号", "订单编号", "合同编号", "序号", "委托号"});
            logger.debug("第 {} 行 - 委托编号原始值: '{}'", rowNum, orderNumber);
            if (orderNumber == null || orderNumber.trim().isEmpty() || "0".equals(orderNumber)) {
                logger.warn("跳过第 {} 行：委托编号为空或为0 (实际值: '{}')", rowNum, orderNumber);
                return null;
            }
            record.setOrderNumber(orderNumber.trim());
            
            // 证券代码
            String stockCode = getCellValueByNames(row, columnMap,
                new String[]{"证券代码", "股票代码", "代码"});
            record.setStockCode(stockCode != null ? stockCode.trim() : "");
            
            // 证券名称
            String stockName = getCellValueByNames(row, columnMap,
                new String[]{"证券名称", "股票名称", "名称"});
            record.setStockName(stockName != null ? stockName.trim() : "");
            
            // 交收日期
            String settlementDate = getCellValueByNames(row, columnMap,
                new String[]{"交收日期", "交收日", "结算日期"});
            record.setSettlementDate(settlementDate != null ? settlementDate.trim() : "");
            
            // 业务名称
            String businessName = getCellValueByNames(row, columnMap,
                new String[]{"业务名称", "交易类型", "操作", "买卖方向"});
            record.setBusinessName(businessName != null ? businessName.trim() : "");
            
            // 成交价格
            Double tradePrice = getCellValueByName(row, columnMap,
                new String[]{"成交价格", "成交均价", "价格"});
            record.setTradePrice(tradePrice != null ? BigDecimal.valueOf(tradePrice) : BigDecimal.ZERO);
            
            // 成交数量
            Integer quantity = getCellValueAsIntegerByName(row, columnMap,
                new String[]{"成交数量", "数量", "成交量"});
            record.setQuantity(quantity != null ? quantity : 0);
            
            // 成交金额
            Double tradeAmount = getCellValueByName(row, columnMap,
                new String[]{"成交金额", "金额", "交易额"});
            record.setTradeAmount(tradeAmount != null ? BigDecimal.valueOf(tradeAmount) : BigDecimal.ZERO);
            
            // 手续费
            Double commission = getCellValueByName(row, columnMap,
                new String[]{"手续费", "佣金", "交易佣金"});
            record.setCommission(commission != null ? BigDecimal.valueOf(commission) : BigDecimal.ZERO);
            
            // 印花税
            Double stampTax = getCellValueByName(row, columnMap,
                new String[]{"印花税", "印花"});
            record.setStampTax(stampTax != null ? BigDecimal.valueOf(stampTax) : BigDecimal.ZERO);
            
            // 过户费
            Double transferFee = getCellValueByName(row, columnMap,
                new String[]{"过户费", "过路费"});
            record.setTransferFee(transferFee != null ? BigDecimal.valueOf(transferFee) : BigDecimal.ZERO);
            
            // 附加费
            Double additionalFee = getCellValueByName(row, columnMap,
                new String[]{"附加费", "其他费用"});
            record.setAdditionalFee(additionalFee != null ? BigDecimal.valueOf(additionalFee) : BigDecimal.ZERO);
            
            // 交易所清算费
            Double exchangeClearingFee = getCellValueByName(row, columnMap,
                new String[]{"交易所清算费", "清算费", "经手费"});
            record.setExchangeClearingFee(exchangeClearingFee != null ? BigDecimal.valueOf(exchangeClearingFee) : BigDecimal.ZERO);
            
            // 基金手续费
            Double fundCommission = getCellValueByName(row, columnMap,
                new String[]{"基金手续费", "基金佣金"});
            record.setFundCommission(fundCommission != null ? BigDecimal.valueOf(fundCommission) : BigDecimal.ZERO);
            
            // 规费
            Double regulatoryFee = getCellValueByName(row, columnMap,
                new String[]{"规费", "证管费"});
            record.setRegulatoryFee(regulatoryFee != null ? BigDecimal.valueOf(regulatoryFee) : BigDecimal.ZERO);
            
            // 换汇尾差
            Double exchangeDifference = getCellValueByName(row, columnMap,
                new String[]{"换汇尾差", "汇率差"});
            record.setExchangeDifference(exchangeDifference != null ? BigDecimal.valueOf(exchangeDifference) : BigDecimal.ZERO);
            
            // 清算金额
            Double clearingAmount = getCellValueByName(row, columnMap,
                new String[]{"清算金额", "发生金额", "资金发生额", "净额"});
            record.setClearingAmount(clearingAmount != null ? BigDecimal.valueOf(clearingAmount) : BigDecimal.ZERO);
            
            // 资金本次余额
            Double fundBalance = getCellValueByName(row, columnMap,
                new String[]{"资金本次余额", "资金余额", "账户余额", "余额"});
            record.setFundBalance(fundBalance != null ? BigDecimal.valueOf(fundBalance) : BigDecimal.ZERO);
            
            // 交收标志
            String settlementFlag = getCellValueByNames(row, columnMap,
                new String[]{"交收标志", "交收状态"});
            record.setSettlementFlag(settlementFlag != null ? settlementFlag.trim() : "");
            
            // 成交时间 - 需要结合交收日期和成交时间
            LocalDateTime tradeTime = parseTradeTime(settlementDate, row, columnMap);
            record.setTradeTime(tradeTime);
            
            // 股东代码
            String shareholderCode = getCellValueByNames(row, columnMap,
                new String[]{"股东代码", "股东账号"});
            record.setShareholderCode(shareholderCode != null ? shareholderCode.trim() : "");
            
            // 资金账号
            String fundAccount = getCellValueByNames(row, columnMap,
                new String[]{"资金账号", "资金帐户"});
            record.setFundAccount(fundAccount != null ? fundAccount.trim() : "");
            
            // 客户代码
            String customerCode = getCellValueByNames(row, columnMap,
                new String[]{"客户代码", "客户号"});
            record.setCustomerCode(customerCode != null ? customerCode.trim() : "");
            
            // 币种
            String currency = getCellValueByNames(row, columnMap,
                new String[]{"币种", "货币"});
            record.setCurrency(currency != null ? currency.trim() : "");
            
            // 交易所名称
            String exchangeName = getCellValueByNames(row, columnMap,
                new String[]{"交易所名称", "交易市场", "市场"});
            record.setExchangeName(exchangeName != null ? exchangeName.trim() : "");
            
            logger.debug("成功解析第 {} 行：委托编号={}, 股票={}", rowNum, record.getOrderNumber(), record.getStockName());
            
        } catch (Exception e) {
            logger.error("解析第 {} 行时发生错误: {}", rowNum, e.getMessage(), e);
            throw e;
        }
        
        return record;
    }

    /**
     * 获取单元格值为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                String val = cell.getStringCellValue();
                return val != null ? val.replace("\ufeff", "") : null;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
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

    /**
     * 获取单元格值为 Double
     */
    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    return null;
                }
            default:
                return null;
        }
    }

    /**
     * 获取单元格值为 Integer
     */
    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            case FORMULA:
                try {
                    return (int) cell.getNumericCellValue();
                } catch (Exception e) {
                    return null;
                }
            default:
                return null;
        }
    }

    /**
     * 解析成交时间（交收日期 + 成交时间）
     */
    private LocalDateTime parseTradeTime(String settlementDate, Row row, Map<String, Integer> columnMap) {
        try {
            String timeStr = getCellValueByNames(row, columnMap,
                new String[]{"成交时间", "交易时间", "时间"});
            
            if (timeStr == null || timeStr.trim().isEmpty()) {
                return LocalDateTime.now();
            }
            
            timeStr = timeStr.trim();
            
            // 如果有交收日期，拼接日期和时间
            if (settlementDate != null && !settlementDate.trim().isEmpty()) {
                String dateTimeStr = settlementDate.trim() + " " + timeStr;
                
                List<DateTimeFormatter> formatters = List.of(
                    DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                );
                
                for (DateTimeFormatter formatter : formatters) {
                    try {
                        return LocalDateTime.parse(dateTimeStr, formatter);
                    } catch (Exception e) {
                        // 继续尝试下一个格式
                    }
                }
            }
            
            // 如果只有时间，使用当前日期
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            java.time.LocalTime time = java.time.LocalTime.parse(timeStr, timeFormatter);
            return java.time.LocalDate.now().atTime(time);
            
        } catch (Exception e) {
            logger.warn("解析成交时间失败，使用当前时间: {}", e.getMessage());
            return LocalDateTime.now();
        }
    }

    /**
     * 判断行是否为空
     */
    private boolean isRowEmpty(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
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
     * 根据多个可能的列名获取单元格值（字符串）
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
     * 根据多个可能的列名获取单元格值（Double）
     */
    private Double getCellValueByName(Row row, Map<String, Integer> columnMap, String[] possibleNames) {
        for (String name : possibleNames) {
            String normalizedName = name.replace("\ufeff", "");
            Integer colIndex = columnMap.get(normalizedName);
            if (colIndex == null) {
                colIndex = columnMap.get(normalizedName.replaceAll("\\s+", ""));
            }
            if (colIndex != null) {
                Cell cell = row.getCell(colIndex);
                return getCellValueAsDouble(cell);
            }
        }
        return null;
    }

    /**
     * 根据多个可能的列名获取单元格值（Integer）
     */
    private Integer getCellValueAsIntegerByName(Row row, Map<String, Integer> columnMap, String[] possibleNames) {
        for (String name : possibleNames) {
            String normalizedName = name.replace("\ufeff", "");
            Integer colIndex = columnMap.get(normalizedName);
            if (colIndex == null) {
                colIndex = columnMap.get(normalizedName.replaceAll("\\s+", ""));
            }
            if (colIndex != null) {
                Cell cell = row.getCell(colIndex);
                return getCellValueAsInteger(cell);
            }
        }
        return null;
    }
}
