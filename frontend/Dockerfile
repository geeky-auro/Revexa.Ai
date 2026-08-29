# ---- dependencies ---------------------------------------------------------
FROM node:22-alpine AS deps
WORKDIR /repo

# The web app depends on the shared @revexa/core workspace, so the install runs at the repo root.
COPY package.json package-lock.json ./
COPY frontend/package.json ./frontend/
COPY packages/core/package.json ./packages/core/
RUN npm ci --no-audit --no-fund

# ---- build ----------------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /repo
COPY --from=deps /repo/node_modules ./node_modules
COPY --from=deps /repo/frontend/node_modules ./frontend/node_modules
COPY package.json package-lock.json ./
COPY packages ./packages
COPY frontend ./frontend

ENV NEXT_TELEMETRY_DISABLED=1
# Baked into the client bundle at build time; the default proxies through the Next server.
ARG NEXT_PUBLIC_API_BASE_URL=/backend
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL
RUN npm run build --workspace frontend

# ---- run ------------------------------------------------------------------
FROM node:22-alpine AS runtime
WORKDIR /repo
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1

RUN addgroup --system --gid 1001 revexa && adduser --system --uid 1001 --ingroup revexa revexa

COPY --from=build /repo/node_modules ./node_modules
COPY --from=build /repo/frontend/node_modules ./frontend/node_modules
COPY --from=build /repo/package.json ./package.json
COPY --from=build /repo/packages ./packages
COPY --from=build --chown=revexa:revexa /repo/frontend ./frontend

USER revexa
WORKDIR /repo/frontend
EXPOSE 3000

HEALTHCHECK --interval=15s --timeout=3s --start-period=20s --retries=5 \
  CMD ["sh", "-c", "wget -q --spider http://localhost:3000/ || exit 1"]

CMD ["npx", "next", "start", "-p", "3000", "-H", "0.0.0.0"]
