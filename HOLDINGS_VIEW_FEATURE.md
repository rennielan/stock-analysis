# 持仓盈亏视图功能说明

## 功能概述

基于 stocks 表创建了一个数据库视图 `v_stock_holdings`，自动计算持仓相关的衍生字段，并在前端主页面展示持仓、成本、盈亏和盈亏比例等信息。

## 数据库视图

### 视图名称
`v_stock_holdings`

### 创建脚本
位置: `/mysql-init/04-create-holdings-view.sql`

### 衍生字段说明

| 字段名 | 类型 | 计算公式 | 说明 |
|--------|------|---------|------|
| market_value | DECIMAL | current_price × reference_shares | 持仓市值 |
| total_cost | DECIMAL | cost_price × reference_shares | 持仓总成本 |
| profit_loss | DECIMAL | (current_price - cost_price) × reference_shares | 持仓盈亏金额 |
| profit_loss_ratio | DECIMAL | (current_price - cost_price) / cost_price × 100 | 盈亏比例（百分比） |
| per_share_profit_loss | DECIMAL | current_price - cost_price | 单股盈亏 |

### 计算逻辑

```sql
-- 持仓市值
CASE 
    WHEN reference_shares IS NOT NULL AND reference_shares > 0 
    THEN ROUND(current_price * reference_shares, 2)
    ELSE 0 
END AS market_value

-- 持仓盈亏
CASE 
    WHEN reference_shares IS NOT NULL AND reference_shares > 0 AND cost_price IS NOT NULL
    THEN ROUND((current_price - cost_price) * reference_shares, 2)
    ELSE 0 
END AS profit_loss

-- 盈亏比例
CASE 
    WHEN cost_price IS NOT NULL AND cost_price > 0
    THEN ROUND((current_price - cost_price) / cost_price * 100, 2)
    ELSE 0 
END AS profit_loss_ratio
```

### 使用示例

```sql
-- 查询所有持仓股票的盈亏情况
SELECT 
    symbol,
    name,
    current_price,
    cost_price,
    reference_shares,
    market_value,
    total_cost,
    profit_loss,
    profit_loss_ratio
FROM v_stock_holdings
WHERE reference_shares IS NOT NULL AND reference_shares > 0
ORDER BY profit_loss DESC;

-- 查询盈利最多的股票
SELECT * FROM v_stock_holdings
WHERE profit_loss > 0
ORDER BY profit_loss DESC
LIMIT 10;

-- 查询亏损的股票
SELECT * FROM v_stock_holdings
WHERE profit_loss < 0
ORDER BY profit_loss ASC;
```

## 前端展示

### 新增列

在股票列表主页面添加了 4 个新列：

1. **持仓** - 显示参考持股数量（referenceShares）
2. **成本** - 显示成本价（costPrice）
3. **盈亏** - 显示持仓盈亏金额（profitLoss）
   - 盈利显示为绿色（+xxx.xx）
   - 亏损显示为红色（-xxx.xx）
4. **盈亏%** - 显示盈亏比例（profitLossRatio）
   - 盈利显示为绿色（+xx.xx%）
   - 亏损显示为红色（-xx.xx%）

### 显示规则

- 如果字段值为 null 或 0，显示 "-"
- 盈亏金额为正数时，前面添加 "+" 号，使用绿色显示
- 盈亏金额为负数时，显示负号，使用红色显示
- 数字格式：
  - 持股数量：保留 2 位小数
  - 成本价：保留 4 位小数，带 ¥ 符号
  - 盈亏金额：保留 2 位小数
  - 盈亏比例：保留 2 位小数，带 % 符号

### 表格布局

更新后的表格列顺序：

```
| 代码 | 价格 | 策略 | 交易计划 | 空间% | 盈亏比 | 持仓 | 成本 | 盈亏 | 盈亏% | 信心 | 备注 | 操作 |
```

## 技术实现

### 后端

1. **Stock 实体** - 已包含相关字段：
   - `referenceShares`: 参考持股数量
   - `costPrice`: 成本价

2. **JPA 自动计算** - 前端接收数据后，由前端计算衍生字段：
   ```typescript
   marketValue = currentPrice * referenceShares
   totalCost = costPrice * referenceShares
   profitLoss = (currentPrice - costPrice) * referenceShares
   profitLossRatio = (currentPrice - costPrice) / costPrice * 100
   ```

### 前端

1. **类型定义** ([types.ts](file:///Users/lejie/Documents/GitHub/stock-analysis/stock-frontend/types.ts))
   ```typescript
   interface StockData {
     // ... 其他字段
     referenceShares?: number | null;
     costPrice?: number | null;
     marketValue?: number;
     totalCost?: number;
     profitLoss?: number;
     profitLossRatio?: number;
     perShareProfitLoss?: number;
   }
   ```

2. **组件展示** ([StockCard.tsx](file:///Users/lejie/Documents/GitHub/stock-analysis/stock-frontend/components/StockCard.tsx))
   - 添加 4 个新的 `<td>` 列
   - 根据数值正负动态设置颜色类名
   - 格式化数字显示

3. **表头更新** ([App.tsx](file:///Users/lejie/Documents/GitHub/stock-analysis/stock-frontend/App.tsx))
   - 添加 4 个新的 `<th>` 列头
   - 调整 colspan 以匹配新列数

## 使用流程

1. **导入持仓数据**
   - 点击右上角"导入持仓"按钮
   - 上传包含证券代码、参考持股、成本价的 Excel 文件
   - 系统自动更新 stocks 表的 reference_shares 和 cost_price 字段

2. **查看盈亏信息**
   - 返回股票列表主页
   - 在表格中查看新增的持仓、成本、盈亏、盈亏%列
   - 盈亏数据会根据正负值显示不同颜色

3. **数据分析**
   - 可以快速识别盈利和亏损的股票
   - 通过盈亏比例了解收益率
   - 结合其他指标做出交易决策

## 注意事项

1. **数据来源**
   - 盈亏计算依赖于 `reference_shares` 和 `cost_price` 字段
   - 需要通过持仓导入功能或手动设置这些字段

2. **实时更新**
   - 当前价格（current_price）由 ETL 任务定期更新
   - 盈亏数据会随当前价格变化而自动重新计算

3. **空值处理**
   - 如果没有设置持股数量或成本价，盈亏相关字段显示为 "-"
   - 不会进行除以零等错误计算

4. **性能考虑**
   - 视图查询只包含活跃股票（is_active = TRUE）
   - 前端计算简单，不会影响性能

## 版本更新

- **v1.5.0**: 新增持仓盈亏展示功能
  - 创建 v_stock_holdings 视图
  - 前端添加持仓、成本、盈亏、盈亏%列
  - 优化表格布局和数据显示
