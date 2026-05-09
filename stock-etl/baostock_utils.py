import baostock as bs
import pandas as pd
import datetime
from sqlalchemy import text
import time

class BaostockUtils:
    
    @staticmethod
    def login():
        """
        登录 baostock
        """
        lg = bs.login()
        if lg.error_code != '0':
            print(f"Baostock login error: {lg.error_code}")
            return False
        return True

    @staticmethod
    def logout():
        """
        登出 baostock
        """
        bs.logout()

    @staticmethod
    def fetch_k_data(code, frequency='d', days=50):
        """
        获取单只股票指定频率和天数的K线数据
        frequency: 'd'=日线, 'w'=周线, 'm'=月线
        返回 pandas DataFrame 或 None
        """
        end_date = datetime.datetime.now().strftime("%Y-%m-%d")
        
        # 根据不同周期计算大概的起止时间
        # 周线和月线需要更长的时间跨度才能拿到足够的数据
        if frequency == 'w':
            start_date = (datetime.datetime.now() - datetime.timedelta(days=days * 7)).strftime("%Y-%m-%d")
            # 周线/月线支持的字段比日线少
            fields = "date,code,open,high,low,close,volume,amount,adjustflag,turn,pctChg"
        elif frequency == 'm':
            start_date = (datetime.datetime.now() - datetime.timedelta(days=days * 30)).strftime("%Y-%m-%d")
            # 周线/月线支持的字段比日线少
            fields = "date,code,open,high,low,close,volume,amount,adjustflag,turn,pctChg"
        else: # 默认日线
            start_date = (datetime.datetime.now() - datetime.timedelta(days=days)).strftime("%Y-%m-%d")
            # 日线支持更多字段，如 preclose, peTTM, pbMRQ 等
            fields = "date,open,high,low,close,preclose,volume,amount,turn,pctChg,peTTM,pbMRQ"
        
        rs = bs.query_history_k_data_plus(code,
            fields,
            start_date=start_date, end_date=end_date,
            frequency=frequency, adjustflag="3")

        if rs.error_code != '0':
            print(f"query_history_k_data_plus error for {code} ({frequency}): {rs.error_msg}")
            return None

        data_list = []
        while (rs.error_code == '0') & rs.next():
            data_list.append(rs.get_row_data())
            
        if not data_list:
            print(f"No {frequency} data found for {code}")
            return None
        
        result = pd.DataFrame(data_list, columns=rs.fields)
        
        # 转换数字类型
        # 提取所有数值类型的列名 (排除 'date', 'code', 'adjustflag' 等)
        numeric_cols = [col for col in result.columns if col not in ['date', 'code', 'adjustflag']]
        for col in numeric_cols:
            result[col] = pd.to_numeric(result[col], errors='coerce')
        
        return result

    @staticmethod
    def save_k_data_to_db(result, code, frequency, engine):
        """
        将获取到的K线 DataFrame 存入 history_k_lines 数据库
        frequency: 'd', 'w', 'm'
        返回最新一天的数据字典：{'current_price': ..., 'change_percent': ...} (主要用于日线更新当前价格)
        """
        if result is None or result.empty:
            return None

        # 映射列名
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
        
        # 因为在 fetch 阶段，周线/月线查询字段里包含了 'code'，
        # 而日线没有包含 'code'（我们在下面手动添加）。
        # 如果从 baostock 查出的 DataFrame 已经有 'code' 列，就不需要覆盖，否则统一覆盖
        result['code'] = code 
        
        result['period'] = frequency
        # 提取 symbol (去掉 sh./sz.)
        result['symbol'] = code.split('.')[1] if '.' in code else code
        
        # 补充缺失的列，防止数据库插入报错（周线/月线缺少 preclose, peTTM, pbMRQ）
        if 'pre_close_price' not in result.columns:
            result['pre_close_price'] = None
        if 'pe_ttm' not in result.columns:
            result['pe_ttm'] = None
        if 'pb_mrq' not in result.columns:
            result['pb_mrq'] = None

        temp_table_name = f"temp_history_k_{frequency}_{code.replace('.', '_')}_{int(time.time())}"
        
        try:
            # 1. 将数据写入临时表
            # 只选取我们需要的列写入数据库
            cols_to_save = ['code', 'symbol', 'period', 'trade_date', 'open_price', 'high_price', 
                            'low_price', 'close_price', 'pre_close_price', 'volume', 'amount', 
                            'turnover_rate', 'pct_chg', 'pe_ttm', 'pb_mrq']
            
            result[cols_to_save].to_sql(temp_table_name, engine, if_exists='replace', index=False)
            
            with engine.begin() as conn:
                # 2. 执行 MERGE 操作 (INSERT ON DUPLICATE KEY UPDATE)
                # 解决 ambiguous 错误：在 IFNULL 中明确指定表名/别名
                # 在 MySQL 中，VALUES() 伪函数获取的是尝试插入的值
                # 对于旧值，需要指定目标表名 history_k_lines (在一些MySQL版本中可能需要别名)
                merge_sql = text(f"""
                    INSERT INTO history_k_lines (
                        code, symbol, period, trade_date, open_price, high_price, low_price, close_price, 
                        pre_close_price, volume, amount, turnover_rate, pct_chg, pe_ttm, pb_mrq
                    )
                    SELECT 
                        code, symbol, period, trade_date, open_price, high_price, low_price, close_price, 
                        pre_close_price, volume, amount, turnover_rate, pct_chg, pe_ttm, pb_mrq
                    FROM {temp_table_name}
                    ON DUPLICATE KEY UPDATE
                        open_price = VALUES(open_price),
                        high_price = VALUES(high_price),
                        low_price = VALUES(low_price),
                        close_price = VALUES(close_price),
                        pre_close_price = IFNULL(VALUES(pre_close_price), history_k_lines.pre_close_price),
                        volume = VALUES(volume),
                        amount = VALUES(amount),
                        turnover_rate = VALUES(turnover_rate),
                        pct_chg = VALUES(pct_chg),
                        pe_ttm = IFNULL(VALUES(pe_ttm), history_k_lines.pe_ttm),
                        pb_mrq = IFNULL(VALUES(pb_mrq), history_k_lines.pb_mrq);
                """)
                conn.execute(merge_sql)
                
                # 3. 删除临时表
                conn.execute(text(f"DROP TABLE {temp_table_name}"))
                
            print(f"Merged {len(result)} {frequency} rows for {code}")
            
            # 只有当保存的是日线时，才返回最新价格用于更新 stocks 表
            if frequency == 'd':
                latest_data = result.iloc[-1]
                return {
                    'current_price': latest_data['close_price'],
                    'change_percent': latest_data['pct_chg']
                }
                
        except Exception as e:
            print(f"Error saving {frequency} data for {code}: {e}")
            if "Duplicate entry" in str(e):
                print("Data already exists (duplicate entry), skipping...")
            else:
                try:
                    with engine.begin() as conn:
                        conn.execute(text(f"DROP TABLE IF EXISTS {temp_table_name}"))
                except:
                    pass
                raise e

        return None

    @staticmethod
    def get_latest_price_from_db(code, frequency, engine):
        """
        如果保存失败或未保存，尝试从数据库读取最新的价格
        """
        try:
            with engine.connect() as conn:
                query = text("SELECT close_price, pct_chg FROM history_k_lines WHERE code = :code AND period = :period ORDER BY trade_date DESC LIMIT 1")
                row = conn.execute(query, {"code": code, "period": frequency}).fetchone()
                if row:
                    return {
                        'current_price': row[0],
                        'change_percent': row[1]
                    }
        except Exception as e:
            print(f"Error getting latest {frequency} price for {code} from DB: {e}")
        return None
