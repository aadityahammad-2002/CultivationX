# ============================================
# STAGE 1: Build
# ============================================
FROM node:20-alpine AS builder

WORKDIR /app

# Package files copy karo
COPY package*.json ./

# Dependencies install karo
RUN npm ci --only=production

# Source code copy karo
COPY . .

# Build karo
RUN npm run build

# ============================================
# STAGE 2: Nginx Serve
# ============================================
FROM nginx:alpine

# Nginx config copy karo
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Build files copy karo
COPY --from=builder /app/dist /usr/share/nginx/html

# Port expose karo
EXPOSE 80

# Run karo
CMD ["nginx", "-g", "daemon off;"]