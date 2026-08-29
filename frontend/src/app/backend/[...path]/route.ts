import type { NextRequest } from 'next/server';

/**
 * Same-origin proxy to the Spring Boot API.
 *
 * This is a route handler rather than a `next.config` rewrite on purpose: rewrites are evaluated
 * during `next build` and frozen into the routes manifest, so a containerised deployment could not
 * point the image at a different API host without rebuilding it. Resolving the target per request
 * keeps `BACKEND_INTERNAL_URL` a genuine runtime setting.
 *
 * Serving the API from the app's own origin also means the browser never deals with CORS, and the
 * API host is never exposed to the client.
 */
const BACKEND_URL = process.env.BACKEND_INTERNAL_URL ?? 'http://localhost:8080';

// Hop-by-hop headers must not be forwarded, and the upstream sets its own encoding and length.
const STRIPPED_REQUEST_HEADERS = ['host', 'connection', 'content-length', 'accept-encoding'];
const STRIPPED_RESPONSE_HEADERS = ['content-encoding', 'content-length', 'transfer-encoding', 'connection'];

async function proxy(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  const { path } = await context.params;
  const target = new URL(`/api/v1/${path.join('/')}`, BACKEND_URL);
  target.search = request.nextUrl.search;

  const headers = new Headers(request.headers);
  STRIPPED_REQUEST_HEADERS.forEach((header) => headers.delete(header));

  const hasBody = request.method !== 'GET' && request.method !== 'HEAD';

  try {
    const upstream = await fetch(target, {
      method: request.method,
      headers,
      body: hasBody ? await request.arrayBuffer() : undefined,
      redirect: 'manual',
      cache: 'no-store',
    });

    const responseHeaders = new Headers(upstream.headers);
    STRIPPED_RESPONSE_HEADERS.forEach((header) => responseHeaders.delete(header));

    return new Response(upstream.body, {
      status: upstream.status,
      statusText: upstream.statusText,
      headers: responseHeaders,
    });
  } catch {
    // The API being down is an infrastructure problem, but the client should still receive the
    // error envelope it knows how to render.
    return Response.json(
      { code: 'upstream_unreachable', message: 'The Revexa API is not reachable right now.' },
      { status: 502 },
    );
  }
}

export const dynamic = 'force-dynamic';

export {
  proxy as GET,
  proxy as POST,
  proxy as PUT,
  proxy as PATCH,
  proxy as DELETE,
  proxy as HEAD,
  proxy as OPTIONS,
};
