# 使用 Slim 版本以获得更好的兼容性 (Debian based)
FROM python:3.10-slim

# 设置时区 (同样为了对齐股票交易时间)
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app

# 1. 优先拷贝 requirements.txt 利用缓存
COPY requirements.txt .

# 安装依赖
# --no-cache-dir 减小镜像体积
# 建议加上阿里源或清华源加速下载
RUN pip install --no-cache-dir -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# 2. 拷贝脚本代码
COPY . .

# 启动命令
# 使用 -u 参数让 Python 的 print 立即输出到 Docker 日志中，方便调试
CMD ["python", "-u", "stock_etl.py"]