# ==========================================
# 阶段 1: 基础环境 (Base)
# ==========================================
FROM node:18-alpine AS base

# 安装 libc6-compat，这是 Alpine 运行某些 Node 库必须的依赖
RUN apk add --no-cache libc6-compat
WORKDIR /app

# ==========================================
# 阶段 2: 安装依赖 (Deps)
# ==========================================
FROM base AS deps
# 复制 package.json 和 lock 文件
COPY package.json package-lock.json* ./
# 使用 ci 安装依赖，比 install 更快更严格
RUN npm ci

# ==========================================
# 阶段 3: 构建项目 (Builder)
# ==========================================
FROM base AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# 开始构建 (这会根据 next.config.js 生成 standalone 文件夹)
# 这一步建议关闭 ESLint 检查以加快速度
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

# ==========================================
# 阶段 4: 生产运行 (Runner)
# ==========================================
FROM base AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# 创建非 root 用户，提高安全性
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# 自动处理静态文件缓存
COPY --from=builder /app/public ./public

# 只复制构建出来的 standalone 文件夹，极大减小体积
# 注意：Next.js 会自动把必要的 node_modules 打包进这里
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

# 切换用户
USER nextjs

# 暴露端口
EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

# 启动命令
CMD ["node", "server.js"]
