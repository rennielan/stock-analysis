import baostock as bs
import pandas as pd
import os
import urllib.parse
from sqlalchemy import create_engine, text
import time

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

def fetch_and_save_stock_basics():
    # 登陆系统
    lg = bs.login()
    if lg.error_code != '0':
        print(f"login respond error_code:{lg.error_code}")
        print(f"login respond  error_msg:{lg.error_msg}")
        return

    try:
        # 获取证券基本资料
        rs = bs.query_stock_basic()
        if rs.error_code != '0':
            print(f"query_stock_basic respond error_code:{rs.error_code}")
            print(f"query_stock_basic respond  error_msg:{rs.error_msg}")
            return

        data_list = []
        while (rs.error_code == '0') & rs.next():
            data_list.append(rs.get_row_data())
        
        if not data_list:
            print("No stock basic data found")
            return

        result = pd.DataFrame(data_list, columns=rs.fields)

        # 数据清洗与转换
        # baostock 返回的日期格式可能是空字符串，需要处理
        result['ipoDate'] = pd.to_datetime(result['ipoDate'], errors='coerce').dt.date
        result['outDate'] = pd.to_datetime(result['outDate'], errors='coerce').dt.date
        
        # 映射列名到数据库字段
        db_columns = {
            'code': 'code',
            'code_name': 'name',
            'ipoDate': 'ipo_date',
            'outDate': 'out_date',
            'type': 'type',
            'status': 'status'
        }
        result.rename(columns=db_columns, inplace=True)
        
        # 提取 symbol (去掉 sh. sz. 前缀，方便关联)
        # 假设 code 格式为 sh.600000
        result['symbol'] = result['code'].apply(lambda x: x.split('.')[1] if '.' in x else x)

        # 保存到数据库
        # 使用临时表策略进行 upsert (更新或插入)
        # 或者简单策略：先清空表再全量插入（如果数据量不大且不需要保留历史变动）
        # 考虑到 stock_basics 数据量在 5000+，全量刷新是可以接受的，但为了稳妥，我们使用逐行 upsert 或者 pandas 的 to_sql + 临时表
        
        # 这里采用先删除所有数据再插入的简单策略（全量刷新）
        # 注意：这会短暂导致表为空，生产环境慎用。
        # 更好的方式是：读取现有数据 -> 对比 -> 仅更新/插入差异
        
        # 既然是基本资料，变化频率低，我们采用：
        # 1. 存入临时表
        # 2. 使用 SQL 语句从临时表同步到主表 (INSERT ON DUPLICATE KEY UPDATE)
        
        temp_table_name = 'stock_basics_temp'
        result.to_sql(temp_table_name, engine, if_exists='replace', index=False)
        
        with engine.begin() as conn:
            # MySQL 的 UPSERT 语法
            upsert_sql = text("""
                INSERT INTO stock_basics (code, symbol, name, ipo_date, out_date, type, status)
                SELECT code, symbol, name, ipo_date, out_date, type, status FROM stock_basics_temp
                ON DUPLICATE KEY UPDATE
                    symbol = VALUES(symbol),
                    name = VALUES(name),
                    ipo_date = VALUES(ipo_date),
                    out_date = VALUES(out_date),
                    type = VALUES(type),
                    status = VALUES(status);
            """)
            conn.execute(upsert_sql)
            
            # 删除临时表
            conn.execute(text(f"DROP TABLE {temp_table_name}"))
            
        print(f"Successfully updated {len(result)} stock basic records.")

    except Exception as e:
        print(f"Error fetching/saving stock basics: {e}")
    finally:
        bs.logout()

if __name__ == "__main__":
    print("Starting Stock Basic ETL...")
    fetch_and_save_stock_basics()
    print("Stock Basic ETL finished.")