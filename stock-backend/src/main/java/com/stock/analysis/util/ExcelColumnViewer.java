package com.stock.analysis.util;

import org.apache.poi.ss.usermodel.*;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Excel 列结构查看工具
 * 用于查看交割单 Excel 文件的实际列顺序
 */
public class ExcelColumnViewer {
    
    public static void main(String[] args) {
        String filePath = "/Users/lejie/Documents/GitHub/stock-analysis/交割单_20260508_232211.xls";
        
        try (InputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            System.out.println("========== Excel 文件结构分析 ==========");
            System.out.println("工作表名称: " + sheet.getSheetName());
            System.out.println("总行数: " + (sheet.getLastRowNum() + 1));
            
            // 读取标题行（假设第一行是标题）
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                System.out.println("\n========== 列结构（从第0列开始）==========");
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    String value = getCellValue(cell);
                    System.out.printf("列 %2d: %s%n", i, value);
                }
                
                System.out.println("\n========== 前3行数据示例 ==========");
                for (int rowNum = 1; rowNum <= Math.min(3, sheet.getLastRowNum()); rowNum++) {
                    Row row = sheet.getRow(rowNum);
                    if (row != null) {
                        System.out.println("\n第 " + (rowNum + 1) + " 行:");
                        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                            Cell cell = row.getCell(i);
                            String value = getCellValue(cell);
                            System.out.printf("  列 %2d: %s%n", i, value);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "(空)";
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
                return cell.getCellFormula();
            default:
                return "(未知类型)";
        }
    }
}
