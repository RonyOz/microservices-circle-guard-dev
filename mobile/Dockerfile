# syntax=docker/dockerfile:1.6
FROM oven/bun:1.1-alpine AS deps
WORKDIR /app
COPY package.json bun.lock ./
RUN --mount=type=cache,target=/root/.bun/install/cache \
    bun install --frozen-lockfile

FROM oven/bun:1.1-alpine AS build
WORKDIR /app
ENV NODE_ENV=production
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN bunx expo export --platform web --output-dir dist

FROM nginx:1.27-alpine AS runtime
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
