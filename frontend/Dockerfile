# syntax=docker/dockerfile:1

## ---- Build stage -----------------------------------------------------------
FROM node:24-alpine AS build
WORKDIR /app

# No "packageManager" field is pinned in package.json; pin the pnpm major here instead
# (lockfileVersion 9.0 in pnpm-lock.yaml is compatible with pnpm 9.x-12.x).
RUN corepack enable && corepack prepare pnpm@10 --activate

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .
RUN pnpm build

## ---- Runtime stage ----------------------------------------------------------
FROM nginxinc/nginx-unprivileged:1.31-alpine

COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

# Cloud Run injects PORT itself (defaulting to 8080); this default lets `docker run` work standalone too.
ENV PORT=8080
EXPOSE 8080
