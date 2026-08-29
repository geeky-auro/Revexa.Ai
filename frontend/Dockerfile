# ---- dependencies ---------------------------------------------------------
FROM node:22-slim AS deps
WORKDIR /repo
ENV npm_config_audit=false npm_config_fund=false

# The web app depends on the shared @revexa/core workspace, so the install runs at the repo root.
COPY package.json package-lock.json ./
COPY frontend/package.json ./frontend/
COPY packages/core/package.json ./packages/core/
RUN npm ci --no-audit --no-fund

# ---- build ----------------------------------------------------------------
FROM node:22-slim AS build
WORKDIR /repo
# npm workspaces hoist every dependency to the root, so there is no per-workspace node_modules to
# copy; Node resolves upward from /repo/frontend and finds them there.
COPY --from=deps /repo/node_modules ./node_modules
COPY package.json package-lock.json ./
COPY packages ./packages
COPY frontend ./frontend

ENV NEXT_TELEMETRY_DISABLED=1
# Baked into the client bundle at build time; the default proxies through the Next server.
ARG NEXT_PUBLIC_API_BASE_URL=/backend
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL
RUN npm run build --workspace frontend

# ---- run ------------------------------------------------------------------
FROM node:22-slim AS runtime
WORKDIR /app
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 PORT=3000 HOSTNAME=0.0.0.0

RUN groupadd --system --gid 1001 revexa \
 && useradd --system --uid 1001 --gid revexa --home /app revexa

# Next's standalone output already contains a minimal server plus only the traced dependencies, so
# the dev toolchain never reaches the runtime image.
COPY --from=build --chown=revexa:revexa /repo/frontend/.next/standalone ./
COPY --from=build --chown=revexa:revexa /repo/frontend/.next/static ./frontend/.next/static

USER revexa
EXPOSE 3000

HEALTHCHECK --interval=15s --timeout=3s --start-period=20s --retries=5 \
  CMD ["node", "-e", "fetch('http://localhost:3000/').then(r => process.exit(r.ok ? 0 : 1)).catch(() => process.exit(1))"]

CMD ["node", "frontend/server.js"]
