-- 交易记录表（新版本，使用委托编号作为主键）
CREATE TABLE trade_records (
    -- 主键：委托编号
    order_number VARCHAR(50) NOT NULL COMMENT '委托编号（主键）',
    
    -- 股票信息
    stock_code VARCHAR(20) NOT NULL COMMENT '证券代码',
    stock_name VARCHAR(100) NOT NULL COMMENT '证券名称',
    
    -- 交易信息
    settlement_date VARCHAR(20) COMMENT '交收日期',
    business_name VARCHAR(50) COMMENT '业务名称（证券买入、证券卖出、股息红利税补缴等）',
    trade_price DECIMAL(10, 4) NOT NULL COMMENT '成交价格',
    quantity INT NOT NULL COMMENT '成交数量',
    trade_amount DECIMAL(16, 2) COMMENT '成交金额',
    
    -- 费用信息
    commission DECIMAL(10, 2) COMMENT '手续费',
    stamp_tax DECIMAL(10, 2) COMMENT '印花税',
    transfer_fee DECIMAL(10, 2) COMMENT '过户费',
    additional_fee DECIMAL(10, 2) COMMENT '附加费',
    exchange_clearing_fee DECIMAL(10, 2) COMMENT '交易所清算费',
    fund_commission DECIMAL(10, 2) COMMENT '基金手续费',
    regulatory_fee DECIMAL(10, 2) COMMENT '规费',
    exchange_difference DECIMAL(10, 2) COMMENT '换汇尾差',
    
    -- 清算信息
    clearing_amount DECIMAL(16, 2) COMMENT '清算金额（负数表示支出，正数表示收入）',
    fund_balance DECIMAL(16, 2) COMMENT '资金本次余额',
    settlement_flag VARCHAR(20) COMMENT '交收标志（如：已交收）',
    
    -- 时间信息
    trade_time DATETIME NOT NULL COMMENT '成交时间',
    
    -- 账户信息
    shareholder_code VARCHAR(50) COMMENT '股东代码',
    fund_account VARCHAR(50) COMMENT '资金账号',
    customer_code VARCHAR(50) COMMENT '客户代码',
    
    -- 其他信息
    currency VARCHAR(20) COMMENT '币种',
    exchange_name VARCHAR(50) COMMENT '交易所名称',
    
    -- 系统字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    stock_id BIGINT COMMENT '关联 stocks 表的外键',
    
    -- 主键约束
    PRIMARY KEY (order_number),
    
    -- 外键约束
    CONSTRAINT fk_trade_record_stock FOREIGN KEY (stock_id) REFERENCES stocks(id) ON DELETE SET NULL,
    
    -- 索引
    INDEX idx_stock_code (stock_code),
    INDEX idx_trade_time (trade_time),
    INDEX idx_business_name (business_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易记录表';