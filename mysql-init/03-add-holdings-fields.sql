-- 为 stocks 表添加参考持股和成本价字段（如果不存在）
-- 此脚本用于手动执行，确保数据库结构与 JPA 实体一致

-- 添加 reference_shares 字段
ALTER TABLE stocks 
ADD COLUMN IF NOT EXISTS reference_shares DECIMAL(16,2) COMMENT '参考持股数量';

-- 添加 cost_price 字段  
ALTER TABLE stocks 
ADD COLUMN IF NOT EXISTS cost_price DECIMAL(10,4) COMMENT '成本价';

-- 为 symbol 字段添加索引（提高查询性能）
CREATE INDEX IF NOT EXISTS idx_stock_symbol ON stocks(symbol);

-- 验证字段是否添加成功
DESCRIBE stocks;
