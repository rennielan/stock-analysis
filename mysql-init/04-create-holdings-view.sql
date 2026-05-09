-- 创建持仓盈亏视图
-- 计算每只股票的持仓盈亏、盈亏比例等信息

CREATE OR REPLACE VIEW v_stock_holdings AS
SELECT 
    id,
    code,
    symbol,
    name,
    current_price,
    change_percent,
    cost_price,
    reference_shares,
    strategy,
    is_active,
    
    -- 计算持仓市值 = 当前价格 × 持股数量
    CASE 
        WHEN reference_shares IS NOT NULL AND reference_shares > 0 
        THEN ROUND(current_price * reference_shares, 2)
        ELSE 0 
    END AS market_value,
    
    -- 计算持仓成本 = 成本价 × 持股数量
    CASE 
        WHEN reference_shares IS NOT NULL AND reference_shares > 0 AND cost_price IS NOT NULL
        THEN ROUND(cost_price * reference_shares, 2)
        ELSE 0 
    END AS total_cost,
    
    -- 计算持仓盈亏 = (当前价格 - 成本价) × 持股数量
    CASE 
        WHEN reference_shares IS NOT NULL AND reference_shares > 0 AND cost_price IS NOT NULL
        THEN ROUND((current_price - cost_price) * reference_shares, 2)
        ELSE 0 
    END AS profit_loss,
    
    -- 计算盈亏比例 = (当前价格 - 成本价) / 成本价 × 100%
    CASE 
        WHEN cost_price IS NOT NULL AND cost_price > 0
        THEN ROUND((current_price - cost_price) / cost_price * 100, 2)
        ELSE 0 
    END AS profit_loss_ratio,
    
    -- 计算单股盈亏 = 当前价格 - 成本价
    CASE 
        WHEN cost_price IS NOT NULL
        THEN ROUND(current_price - cost_price, 4)
        ELSE 0 
    END AS per_share_profit_loss,
    
    buy_price,
    target_price,
    stop_loss,
    confidence,
    notes,
    created_at,
    updated_at
    
FROM stocks
WHERE is_active = TRUE;

-- 添加注释
ALTER VIEW v_stock_holdings COMMENT '持仓盈亏视图 - 包含市值、成本、盈亏等衍生字段';

-- 测试查询
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
