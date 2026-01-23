import time
import schedule
import requests
import pymysql
from sqlalchemy import create_engine, text
import pandas as pd
import os
import yfinance as yf

# 数据库配置
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = os.getenv('DB_PORT', '3306')
DB_USER = os.getenv('DB_USERNAME', 'root')
DB_PASS = os.getenv('DB_PASSWORD', 'root')
DB_NAME = os.getenv('DB_NAME', 'stock_db')

# 创建数据库连接引擎
db_url = f"mysql+pymysql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}?charset=utf8mb4"
engine = create_engine(db_url)

def fetch_real_stock_data(symbol):
    """
    使用 yfinance 获取真实股票数据
    """
    try:
        ticker = yf.Ticker(symbol)
        # 获取最新一天的历史数据
        hist = ticker.history(period="1d")
        
        if hist.empty:
            print(f"警告: 无法获取 {symbol} 的数据")
            return None
            
        current_price = hist['Close'].iloc[-1]
        
        # 计算涨跌幅 (需要前一天的收盘价，这里简化处理，使用Open作为参考，或者尝试获取更多历史数据)
        # 更准确的做法是获取前一天的 Close
        hist_5d = ticker.history(period="5d")
        if len(hist_5d) >= 2:
            prev_close = hist_5d['Close'].iloc[-2]
            change_percent = ((current_price - prev_close) / prev_close) * 100
        else:
            # 如果是新股或数据不足，用开盘价估算
            open_price = hist['Open'].iloc[-1]
            change_percent = ((current_price - open_price) / open_price) * 100

        return {
            'symbol': symbol,
            'current_price': round(current_price, 2),
            'change_percent': round(change_percent, 2)
        }
    except Exception as e:
        print(f"获取 {symbol} 数据时出错: {e}")
        return None

def update_stock_prices():
    print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] 开始更新股票价格...")
    
    try:
        # 1. 获取数据库中所有活跃的股票代码
        with engine.connect() as conn:
            result = conn.execute(text("SELECT id, symbol FROM stocks WHERE is_active = 1"))
            stocks = result.fetchall()
            
        if not stocks:
            print("没有需要更新的股票")
            return

        # 2. 遍历更新每个股票的价格
        for stock_id, symbol in stocks:
            try:
                market_data = fetch_real_stock_data(symbol)
                
                if market_data:
                    # 3. 更新数据库
                    with engine.connect() as conn:
                        update_sql = text("""
                            UPDATE stocks 
                            SET current_price = :price, 
                                change_percent = :change,
                                updated_at = NOW()
                            WHERE id = :id
                        """)
                        conn.execute(update_sql, {
                            "price": market_data['current_price'],
                            "change": market_data['change_percent'],
                            "id": stock_id
                        })
                        conn.commit()
                        
                    print(f"更新成功: {symbol} -> ${market_data['current_price']}")
                
                # 避免请求过快被封禁
                time.sleep(1)
                
            except Exception as e:
                print(f"更新失败 {symbol}: {str(e)}")
                
    except Exception as e:
        print(f"ETL任务执行出错: {str(e)}")

def main():
    print("Stock ETL 服务启动...")
    
    # 立即执行一次
    update_stock_prices()
    
    # 每5分钟执行一次 (Yahoo Finance 建议不要太频繁)
    schedule.every(5).minutes.do(update_stock_prices)
    
    while True:
        schedule.run_pending()
        time.sleep(1)

if __name__ == "__main__":
    main()