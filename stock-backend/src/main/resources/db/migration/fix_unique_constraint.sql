-- 修复 trade_records 表的唯一约束问题
-- 问题：旧的唯一约束 uk_trade_record 包含已废弃的 trade_direction 字段
-- 解决：删除旧的唯一约束

-- 检查并删除旧的唯一索引
ALTER TABLE trade_records DROP INDEX IF EXISTS uk_trade_record;

-- 如果需要添加新的唯一约束（基于委托编号已经作为主键，通常不需要额外的唯一约束）
-- 可以根据业务需求决定是否添加其他索引
