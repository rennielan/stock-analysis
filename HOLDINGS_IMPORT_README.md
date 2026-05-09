# 持仓数据导入功能说明

## 功能概述

支持通过 Excel 文件批量导入股票的参考持股数量和成本价数据。系统会根据 Excel 中的证券代码字段与 stocks 表中的 symbol 字段进行匹配，采用 merge 操作更新或插入数据。

## API 端点

```
POST /trades/upload-holdings
```

## 请求参数

- **file**: Excel 文件（.xls 或 .xlsx 格式）

## Excel 文件格式要求

### 必需的列

Excel 文件中应包含以下列（列名可以有多种变体）：

1. **证券代码**（必需）
   - 可能的列名：`证券代码`、`股票代码`、`代码`
   - 格式：可以是 `sh.600000`、`sz.000001` 或纯数字 `600000`、`000001`
   - 系统会自动提取纯数字部分作为 symbol 进行匹配

2. **参考持股**（可选）
   - 可能的列名：`参考持股`、`持股数量`、`持仓数量`、`数量`
   - 格式：数字，支持小数

3. **成本价**（可选）
   - 可能的列名：`成本价`、`持仓成本`、`成本价格`、`买入成本`
   - 格式：数字，支持小数

4. **证券名称**（可选，用于创建新股票时）
   - 可能的列名：`证券名称`、`股票名称`、`名称`

### 示例 Excel 结构

| 证券代码 | 证券名称 | 参考持股 | 成本价 |
|---------|---------|---------|--------|
| sh.600000 | 浦发银行 | 1000 | 10.50 |
| sz.000001 | 平安银行 | 2000 | 12.30 |
| 600036 | 招商银行 | 1500 | 35.80 |

## 匹配逻辑

系统会按以下顺序查找股票：

1. **通过 symbol 匹配**：从证券代码中提取纯数字部分（如 `sh.600000` → `600000`），在 stocks 表的 symbol 字段中查找
   
2. **通过 code 匹配**：如果 symbol 未找到，尝试用完整的代码在 stocks 表的 code 字段中查找

3. **从 stock_basic 表获取信息**：如果 stocks 表中不存在，会从 stock_basic 表中查询获取完整的 code、symbol 和 name

4. **创建新记录**：如果以上都不存在，会创建新的 Stock 记录

## Merge 操作

- 如果找到已存在的股票记录，会更新其 `reference_shares` 和 `cost_price` 字段
- 如果不存在，会创建新的股票记录并设置这些字段
- 使用事务确保数据一致性

## 响应示例

### 成功响应

```json
{
  "message": "持仓数据导入完成",
  "successCount": 10,
  "failCount": 0,
  "notFoundCount": 0,
  "updatedStocks": [
    {
      "id": 1,
      "code": "sh.600000",
      "name": "浦发银行",
      "referenceShares": 1000.00,
      "costPrice": 10.5000
    },
    {
      "id": 2,
      "code": "sz.000001",
      "name": "平安银行",
      "referenceShares": 2000.00,
      "costPrice": 12.3000
    }
  ]
}
```

### 错误响应

```json
{
  "error": "文件不能为空"
}
```

或

```json
{
  "error": "只支持 Excel 文件格式（.xls 或 .xlsx）"
}
```

## 使用示例

### cURL

```bash
curl -X POST http://localhost:8080/trades/upload-holdings \
  -F "file=@/path/to/持仓_20260509_104147.xls"
```

### JavaScript (Fetch)

```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch('http://localhost:8080/trades/upload-holdings', {
  method: 'POST',
  body: formData
})
.then(response => response.json())
.then(data => {
  console.log('导入结果:', data);
})
.catch(error => {
  console.error('导入失败:', error);
});
```

## 注意事项

1. **股票代码格式**：系统支持多种格式的股票代码，会自动提取纯数字部分进行匹配
2. **数据合并**：如果股票已存在，只会更新 reference_shares 和 cost_price 字段，不会影响其他字段
3. **事务处理**：整个导入过程在一个事务中执行，确保数据一致性
4. **错误处理**：单行数据处理失败不会影响其他行的处理
5. **日志记录**：所有操作都会记录详细的日志，便于排查问题

## 数据库字段说明

stocks 表新增字段：

- `reference_shares`: DECIMAL(16,2) - 参考持股数量
- `cost_price`: DECIMAL(10,4) - 成本价
- `idx_stock_symbol`: INDEX - symbol 字段的索引，提高查询性能

## 实现细节

### HoldingExcelService

参考 ExcelTradeService 的实现方式，使用 Apache POI 解析 Excel 文件：

1. **动态列名识别**：支持多种列名变体，自动去除 BOM 字符和空格
2. **智能匹配**：优先通过 symbol 匹配，降级到 code 匹配
3. **stock_basic 关联**：如果 stocks 表中不存在，从 stock_basic 表获取完整信息
4. **Merge 操作**：找到则更新，不存在则创建

### StockRepository

新增方法：
- `findBySymbol(String symbol)`: 根据 symbol 字段查找股票

### Stock 实体

已有字段：
- `referenceShares`: 参考持股数量
- `costPrice`: 成本价

新增索引：
- `idx_stock_symbol`: symbol 字段索引
