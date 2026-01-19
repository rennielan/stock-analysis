# ==========================================
# 阶段 1: 基础环境 (Base)
# ==========================================
FROM node:18-alpine AS base

# 安装 libc6-compat，这是 Alpine 运行某些 Node 库必须的依赖
RUN apk add --no-cache libc6-compat
WORKDIR /app

# ==========================================
# 阶段 2: 依赖安装 (Deps)
# ==========================================
FROM node:18-alpine AS deps

RUN apk add --no-cache libc6-compat
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci --only=production && npm cache clean --force

# ==========================================
# 阶段 2: 构建应用 (Builder)
# ==========================================
FROM node:18-alpine AS builder

WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

ENV NODE_ENV=production
RUN npm run build

# ==========================================
# 阶段 3: 生产运行 (Production)
# ==========================================
FROM nginx:alpine AS production

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

COPY --from=builder /app/dist /usr/share/nginx/html

# 使用统一的nginx配置
# 注意：这里使用默认配置，因为nginx服务会处理路由

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]