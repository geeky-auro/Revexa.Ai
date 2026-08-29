import type { NextConfig } from 'next';

const backendUrl = process.env.BACKEND_INTERNAL_URL ?? 'http://localhost:8080';

const nextConfig: NextConfig = {
  reactStrictMode: true,
  // The shared core package is consumed as TypeScript source so the web app, and later the mobile,
  // desktop and editor surfaces, all build from one copy of the domain logic.
  transpilePackages: ['@revexa/core'],
  async rewrites() {
    // Same-origin proxy in development so the browser never has to care about CORS or ports.
    return [
      {
        source: '/backend/:path*',
        destination: `${backendUrl}/api/v1/:path*`,
      },
    ];
  },
};

export default nextConfig;
