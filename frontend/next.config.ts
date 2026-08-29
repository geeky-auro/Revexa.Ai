import path from 'node:path';
import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  reactStrictMode: true,
  // Traces the minimal set of files the server actually needs, so the runtime image carries neither
  // the dev toolchain nor the parts of Monaco that were never imported.
  output: 'standalone',
  // The workspace root, so tracing follows the hoisted node_modules and @revexa/core.
  outputFileTracingRoot: path.join(import.meta.dirname, '..'),
  // The shared core package is consumed as TypeScript source so the web app, and later the mobile,
  // desktop and editor surfaces, all build from one copy of the domain logic.
  transpilePackages: ['@revexa/core'],
};

export default nextConfig;
