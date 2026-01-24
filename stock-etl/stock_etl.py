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
import stock_basic_etl  # 导入基本资料ETL模块

# 数据库配置
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = os.getenv('DB_PORT', '3306')
DB_USER = os.getenv('DB_USERNAME', 'root')
DB_PASS = os.getenv('DB_PASSWORD', 'M_wandering1')
DB_NAME = os.getenv('DB_NAME', 'stock_db')

print(f"Connecting to database at {DB_HOST}:{DB_PORT} as {DB_USER}")

# 对密码进行 URL 编码
encoded_pass = urllib.parse.quote_plus(DB_PASS)
db_url = f"mysql+pymysql://{DB_USER}:{encoded_pass}@{DB_HOST}:{DB_PORT}/{DB_NAME}?charset=utf8mb4"
engine = create_engine(db_url)

def init_baostock():
    """初始化 baostock 连接"""
    lg = bs.login()
    if lg.error_code != '0':
        print(f"login respond error_code:{lg.error_code}")
        print(f"login respond  error_msg:{lg.error_msg}")
        return False
    return True

def format_symbol(symbol):
    """
    格式化股票代码以适应 baostock
    假设输入的 symbol 可能是 "600000" 或 "sh.600000"
    baostock 需要 "sh.600000" 或 "sz.000001" 格式
    """
    if '.' in symbol:
        return symbol.lower()
    
    # 简单的推断逻辑：6开头是上海，其他假设是深圳（实际需要更严谨的判断）
    if symbol.startswith('6'):
        return f"sh.{symbol}"
    else:
        return f"sz.{symbol}"

def fetch_and_save_daily_k_data(symbol):
    """
    获取并保存日线数据
    """
    bs_symbol = format_symbol(symbol)
    print(f"Fetching data for {symbol} (Baostock symbol: {bs_symbol})...")
    
    # 获取最近一年的数据
    end_date = datetime.datetime.now().strftime("%Y-%m-%d")
    start_date = (datetime.datetime.now() - datetime.timedelta(days=50)).strftime("%Y-%m-%d")
    
    print(f"Querying history k data from {start_date} to {end_date}...")
    rs = bs.query_history_k_data_plus(bs_symbol,
        "date,open,high,low,close,preclose,volume,amount,turn,pctChg,peTTM,pbMRQ",
        start_date=start_date, end_date=end_date,
        frequency="d", adjustflag="3") # adjustflag="3" 代表不复权，也可以选 "2" 前复权

    if rs.error_code != '0':
        print(f"query_history_k_data_plus respond error_code:{rs.error_code}")
        print(f"query_history_k_data_plus respond  error_msg:{rs.error_msg}")
        return None

    data_list = []
    while (rs.error_code == '0') & rs.next():
        data_list.append(rs.get_row_data())
        
    if not data_list:
        print(f"No data found for {symbol}")
        return None
    
    print(f"Found {len(data_list)} records for {symbol}")

    result = pd.DataFrame(data_list, columns=rs.fields)
    
    # 数据类型转换
    numeric_cols = ['open', 'high', 'low', 'close', 'preclose', 'volume', 'amount', 'turn', 'pctChg', 'peTTM', 'pbMRQ']
    for col in numeric_cols:
        # baostock 返回空字符串时转为 NaN，然后 fillna(0) 或者保持 NaN
        result[col] = pd.to_numeric(result[col], errors='coerce')
    
    # 保存到数据库
    # 映射 DataFrame 列名到数据库列名
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
    result['symbol'] = symbol # 使用原始 symbol 存入数据库，或者统一格式
    
    try:
        # 1. 存入 daily_k_lines
        # 为了防止重复主键错误，我们可以先读取数据库中该 symbol 已有的最大日期
        with engine.connect() as conn:
            query = text("SELECT MAX(trade_date) FROM daily_k_lines WHERE symbol = :symbol")
            max_date_result = conn.execute(query, {"symbol": symbol}).scalar()
            
        if max_date_result:
            max_date_str = max_date_result.strftime("%Y-%m-%d")
            print(f"Latest data in DB is {max_date_str}, filtering new data...")
            # 过滤出比数据库中更新的数据
            result = result[result['trade_date'] > max_date_str]
        
        if not result.empty:
            print(f"Saving {len(result)} new rows to daily_k_lines...")
            result.to_sql('daily_k_lines', engine, if_exists='append', index=False)
            print(f"Saved {len(result)} rows for {symbol} to daily_k_lines")
        else:
            print(f"No new data for {symbol}")

        # 2. 返回最新的一条数据用于更新 stocks 表
        if not result.empty:
            latest_data = result.iloc[-1]
            return {
                'current_price': latest_data['close_price'],
                'change_percent': latest_data['pct_chg']
            }
        else:
            # 如果没有新数据，尝试获取数据库中最新的一条
             with engine.connect() as conn:
                query = text("SELECT close_price, pct_chg FROM daily_k_lines WHERE symbol = :symbol ORDER BY trade_date DESC LIMIT 1")
                row = conn.execute(query, {"symbol": symbol}).fetchone()
                if row:
                    return {
                        'current_price': row[0],
                        'change_percent': row[1]
                    }
            
    except Exception as e:
        print(f"Error saving data for {symbol}: {e}")
        # 如果是因为重复键错误，说明数据已存在，忽略
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
        # 1. 获取数据库中所有活跃的股票代码
        with engine.connect() as conn:
            result = conn.execute(text("SELECT id, symbol FROM stocks WHERE is_active = 1"))
            stocks = result.fetchall()
            
        if not stocks:
            print("没有需要更新的股票")
            bs.logout()
            return
        
        print(f"Found {len(stocks)} active stocks to update.")

        # 2. 遍历更新每个股票
        for stock_id, symbol in stocks:
            try:
                market_data = fetch_and_save_daily_k_data(symbol)
                
                if market_data:
                    # 3. 更新 stocks 表的最新价格
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
                        
                    print(f"更新 stocks 表成功: {symbol} -> ${market_data['current_price']}")
                
                time.sleep(0.5) # 避免请求过快
                
            except Exception as e:
                print(f"更新失败 {symbol}: {str(e)}")
        
        bs.logout()
                
    except Exception as e:
        print(f"ETL任务执行出错: {str(e)}")
        bs.logout()

def main():
    print("Stock ETL 服务启动 (Baostock版)...")
    time.sleep(5)
    
    # 1. 启动时先同步一次证券基本资料 (新增)
    print("正在同步证券基本资料...")
    try:
        stock_basic_etl.fetch_and_save_stock_basics()
    except Exception as e:
        print(f"同步证券基本资料失败: {e}")
    
    # 2. 首次运行价格更新
    update_stock_prices()
    
    # 3. 定时任务配置
    # 每天下午 15:30 执行一次 (A股收盘后)
    schedule.every().day.at("15:30").do(update_stock_prices)
    
    # 每天凌晨 02:00 同步一次基本资料 (新增)
    schedule.every().day.at("02:00").do(stock_basic_etl.fetch_and_save_stock_basics)
    
    while True:
        schedule.run_pending()
        time.sleep(1)

if __name__ == "__main__":
    main()