FROM node:24-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . ./
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine

COPY --from=builder /app/dist/web-app/browser/browser /usr/share/nginx/html

EXPOSE 8080

USER 101
CMD ["nginx", "-g", "daemon off;"]
