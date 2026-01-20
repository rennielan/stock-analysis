# ==========================================
# 阶段 1: 基础环境 (Base)
# ==========================================
FROM node:20-alpine AS base

# 安装必要的依赖
RUN apk add --no-cache libc6-compat git python3 make g++
WORKDIR /app

# ==========================================
# 阶段 2: 依赖安装 (Deps)
# ==========================================
FROM base AS deps

WORKDIR /app

# 复制package文件
COPY package.json package-lock.json* ./

# 安装所有依赖（包括开发依赖）
RUN npm ci --no-audit --prefer-offline

# 安装terser（Vite v3+ 必需的可选依赖）
RUN npm install terser --save-dev

# ==========================================
# 阶段 3: 构建应用 (Builder)
# ==========================================
FROM base AS builder

WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# 设置构建环境
ENV NODE_ENV=production
ENV CI=true

# 执行构建
RUN npm run build

# ==========================================
# 阶段 4: 生产运行 (Production)
# ==========================================
FROM nginx:alpine AS production

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制自定义nginx配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80  # ✅ 确保暴露80端口
CMD ["nginx", "-g", "daemon off;"]