import time
import schedule
import pymysql
from sqlalchemy import create_engine, text
import pandas as pd
import os
import urllib.parse
import random
import baostock as bs
import datetime
import stock_basic_etl

# 数据库配置
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = os.getenv('DB_PORT', '3306')
DB_USER = os.getenv('DB_USERNAME', 'root')
DB_PASS = os.getenv('DB_PASSWORD', 'M_wandering1')
DB_NAME = os.getenv('DB_NAME', 'stock_db')

print(f"Connecting to database at {DB_HOST}:{DB_PORT} as {DB_USER}")

encoded_pass = urllib.parse.quote_plus(DB_PASS)
db_url = f"mysql+pymysql://{DB_USER}:{encoded_pass}@{DB_HOST}:{DB_PORT}/{DB_NAME}?charset=utf8mb4"
engine = create_engine(db_url)

def init_baostock():
    lg = bs.login()
    if lg.error_code != '0':
        print(f"login respond error_code:{lg.error_code}")
        return False
    return True

def fetch_and_save_daily_k_data(code):
    """
    获取并保存日线数据 (使用 code)
    """
    # code 已经是 baostock 格式 (如 sh.600000)
    print(f"Fetching data for {code}...")
    
    end_date = datetime.datetime.now().strftime("%Y-%m-%d")
    start_date = (datetime.datetime.now() - datetime.timedelta(days=50)).strftime("%Y-%m-%d")
    
    rs = bs.query_history_k_data_plus(code,
        "date,open,high,low,close,preclose,volume,amount,turn,pctChg,peTTM,pbMRQ",
        start_date=start_date, end_date=end_date,
        frequency="d", adjustflag="3")

    if rs.error_code != '0':
        print(f"query_history_k_data_plus error: {rs.error_msg}")
        return None

    data_list = []
    while (rs.error_code == '0') & rs.next():
        data_list.append(rs.get_row_data())
        
    if not data_list:
        print(f"No data found for {code}")
        return None
    
    result = pd.DataFrame(data_list, columns=rs.fields)
    
    numeric_cols = ['open', 'high', 'low', 'close', 'preclose', 'volume', 'amount', 'turn', 'pctChg', 'peTTM', 'pbMRQ']
    for col in numeric_cols:
        result[col] = pd.to_numeric(result[col], errors='coerce')
    
    db_columns = {
        'date': 'trade_date',
        'open': 'open_price',
        'high': 'high_price',
        'low': 'low_price',
        'close': 'close_price',
        'preclose': 'pre_close_price',
        'volume': 'volume',
        'amount': 'amount',
        'turn': 'turnover_rate',
        'pctChg': 'pct_chg',
        'peTTM': 'pe_ttm',
        'pbMRQ': 'pb_mrq'
    }
    result.rename(columns=db_columns, inplace=True)
    result['code'] = code
    # 提取 symbol (去掉 sh./sz.)
    result['symbol'] = code.split('.')[1] if '.' in code else code
    
    try:
        with engine.connect() as conn:
            query = text("SELECT MAX(trade_date) FROM daily_k_lines WHERE code = :code")
            max_date_result = conn.execute(query, {"code": code}).scalar()
            
        if max_date_result:
            max_date_str = max_date_result.strftime("%Y-%m-%d")
            result = result[result['trade_date'] > max_date_str]
        
        if not result.empty:
            result.to_sql('daily_k_lines', engine, if_exists='append', index=False)
            print(f"Saved {len(result)} rows for {code}")
        
        if not result.empty:
            latest_data = result.iloc[-1]
            return {
                'current_price': latest_data['close_price'],
                'change_percent': latest_data['pct_chg']
            }
        else:
             with engine.connect() as conn:
                query = text("SELECT close_price, pct_chg FROM daily_k_lines WHERE code = :code ORDER BY trade_date DESC LIMIT 1")
                row = conn.execute(query, {"code": code}).fetchone()
                if row:
                    return {
                        'current_price': row[0],
                        'change_percent': row[1]
                    }
            
    except Exception as e:
        print(f"Error saving data for {code}: {e}")
        if "Duplicate entry" in str(e):
            print("Data already exists, skipping...")
        else:
            raise e

    return None

def update_stock_prices():
    print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] 开始更新股票价格...")
    
    if not init_baostock():
        print("Baostock login failed")
        return

    try:
        with engine.connect() as conn:
            # 使用 code 查询
            result = conn.execute(text("SELECT id, code FROM stocks WHERE is_active = 1"))
            stocks = result.fetchall()
            
        if not stocks:
            print("没有需要更新的股票")
            bs.logout()
            return
        
        print(f"Found {len(stocks)} active stocks to update.")

        for stock_id, code in stocks:
            try:
                market_data = fetch_and_save_daily_k_data(code)
                
                if market_data:
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
                        
                    print(f"更新 stocks 表成功: {code} -> ${market_data['current_price']}")
                
                time.sleep(0.5)
                
            except Exception as e:
                print(f"更新失败 {code}: {str(e)}")
        
        bs.logout()
                
    except Exception as e:
        print(f"ETL任务执行出错: {str(e)}")
        bs.logout()

def main():
    print("Stock ETL 服务启动 (Baostock版)...")
    time.sleep(5)
    
    print("正在同步证券基本资料...")
    try:
        stock_basic_etl.fetch_and_save_stock_basics()
    except Exception as e:
        print(f"同步证券基本资料失败: {e}")
    
    update_stock_prices()
    
    schedule.every().day.at("15:30").do(update_stock_prices)
    schedule.every().day.at("02:00").do(stock_basic_etl.fetch_and_save_stock_basics)
    
    while True:
        schedule.run_pending()
        time.sleep(1)

if __name__ == "__main__":
    main()