import time
import schedule
import os
import urllib.parse
from sqlalchemy import create_engine, text
import stock_basic_etl

# 引入新的工具类
from baostock_utils import BaostockUtils

# 数据库配置
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = os.getenv('DB_PORT', '3306')
DB_USER = os.getenv('DB_USERNAME', 'root')
DB_PASS = os.getenv('DB_PASSWORD', 'root')
DB_NAME = os.getenv('DB_NAME', 'stock_db')

print(f"Connecting to database at {DB_HOST}:{DB_PORT} as {DB_USER}")

encoded_pass = urllib.parse.quote_plus(DB_PASS)
db_url = f"mysql+pymysql://{DB_USER}:{encoded_pass}@{DB_HOST}:{DB_PORT}/{DB_NAME}?charset=utf8mb4"

connect_args = {
    "ssl": {"fake_flag_to_enable_tls": False},
}

try:
    engine = create_engine(db_url, connect_args=connect_args)
    with engine.connect() as conn:
        print("Database connection successful!")
except Exception as e:
    print(f"Initial database connection failed: {e}")

def process_single_stock(code):
    """
    处理单只股票的数据拉取和入库 (包含日线、周线、月线)
    """
    print(f"Fetching data for {code}...")
    
    market_data = None

    # 拉取并保存日线 ('d')
    df_daily = BaostockUtils.fetch_k_data(code, frequency='d', days=50)
    if df_daily is not None:
        # 日线的保存会返回最新的价格和涨跌幅，用于更新 stocks 主表
        market_data = BaostockUtils.save_k_data_to_db(df_daily, code, 'd', engine)
    else:
        # 如果由于某种原因获取为空，尝试读取最后一条数据（如节假日）
        market_data = BaostockUtils.get_latest_price_from_db(code, 'd', engine)

    # 拉取并保存周线 ('w')
    df_weekly = BaostockUtils.fetch_k_data(code, frequency='w', days=50)
    if df_weekly is not None:
        BaostockUtils.save_k_data_to_db(df_weekly, code, 'w', engine)

    # 拉取并保存月线 ('m')
    df_monthly = BaostockUtils.fetch_k_data(code, frequency='m', days=50)
    if df_monthly is not None:
        BaostockUtils.save_k_data_to_db(df_monthly, code, 'm', engine)

    return market_data

def update_stock_prices():
    print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] 开始更新股票价格...")
    
    if not BaostockUtils.login():
        return

    try:
        with engine.connect() as conn:
            result = conn.execute(text("SELECT id, code FROM stocks WHERE is_active = 1"))
            stocks = result.fetchall()
            
        if not stocks:
            print("没有需要更新的股票")
            BaostockUtils.logout()
            return
        
        print(f"Found {len(stocks)} active stocks to update.")

        for stock_id, code in stocks:
            try:
                market_data = process_single_stock(code)
                
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
                
                time.sleep(0.5) # 稍微睡眠以避免频繁请求
                
            except Exception as e:
                print(f"更新失败 {code}: {str(e)}")
        
        BaostockUtils.logout()
                
    except Exception as e:
        print(f"ETL任务执行出错: {str(e)}")
        BaostockUtils.logout()

def check_new_active_stocks():
    """
    检查是否有新添加的活跃股票（没有价格数据的），并立即更新
    """
    try:
        with engine.connect() as conn:
            # 查找 is_active=1 且 current_price=0 的股票
            result = conn.execute(text("SELECT id, code FROM stocks WHERE is_active = 1 AND current_price = 0"))
            new_stocks = result.fetchall()
            
        if new_stocks:
            print(f"Found {len(new_stocks)} new active stocks without price data. Updating...")
            if not BaostockUtils.login():
                return

            for stock_id, code in new_stocks:
                try:
                    market_data = process_single_stock(code)
                    
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
                        print(f"Initialized price for new stock: {code}")
                    time.sleep(0.5)
                except Exception as e:
                    print(f"Failed to initialize new stock {code}: {e}")
            
            BaostockUtils.logout()
            
    except Exception as e:
        print(f"Error checking new stocks: {e}")

def main():
    print("Stock ETL 服务启动 (Baostock版)...")
    time.sleep(5)
    
    print("正在同步证券基本资料...")
    try:
        stock_basic_etl.fetch_and_save_stock_basics()
    except Exception as e:
        print(f"同步证券基本资料失败: {e}")
    
    # 首次全量更新
    update_stock_prices()
    
    # 定时任务
    schedule.every().day.at("15:30").do(update_stock_prices)
    schedule.every().day.at("17:30").do(update_stock_prices)
    schedule.every().day.at("18:30").do(update_stock_prices)
    schedule.every().day.at("19:30").do(update_stock_prices)
    schedule.every().day.at("20:30").do(update_stock_prices)
    schedule.every().day.at("02:00").do(stock_basic_etl.fetch_and_save_stock_basics)
    
    # 每 10 秒检查一次是否有新添加的股票需要初始化数据
    schedule.every(10).seconds.do(check_new_active_stocks)
    
    while True:
        schedule.run_pending()
        time.sleep(1)

if __name__ == "__main__":
    main()