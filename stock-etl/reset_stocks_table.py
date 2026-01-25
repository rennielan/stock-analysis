import os
import urllib.parse
from sqlalchemy import create_engine, text

# 数据库配置
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = os.getenv('DB_PORT', '3306')
DB_USER = os.getenv('DB_USERNAME', 'root')
DB_PASS = os.getenv('DB_PASSWORD', 'root')
DB_NAME = os.getenv('DB_NAME', 'stock_db')

print(f"Connecting to database at {DB_HOST}:{DB_PORT} as {DB_USER}")

encoded_pass = urllib.parse.quote_plus(DB_PASS)
db_url = f"mysql+pymysql://{DB_USER}:{encoded_pass}@{DB_HOST}:{DB_PORT}/{DB_NAME}?charset=utf8mb4"
engine = create_engine(db_url)

def reset_stocks_table():
    try:
        with engine.connect() as conn:
            print("Dropping table 'stocks'...")
            conn.execute(text("DROP TABLE IF EXISTS stocks"))
            print("Table dropped. Hibernate will recreate it on next startup.")
            
            # 也可以选择手动创建，但依赖 Hibernate 自动创建更简单
            # 如果 Hibernate ddl-auto 不是 update/create，则需要手动创建
            # 这里我们假设是 update，所以删除后重启后端即可
            
    except Exception as e:
        print(f"Error resetting table: {e}")

if __name__ == "__main__":
    reset_stocks_table()