import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Production security headers. Mirror these on whatever serves dist/
// (see web/public/_headers and docs/SECURITY.md). connect-src must include
// the API origin; set VITE_API_URL at build time.
const apiOrigin = process.env.VITE_API_URL ?? 'http://localhost:8080'
const wsOrigin = apiOrigin.replace(/^http/, 'ws')

const securityHeaders = {
  'Content-Security-Policy': [
    "default-src 'self'",
    "script-src 'self'",
    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
    "font-src 'self' https://fonts.gstatic.com",
    "img-src 'self' data:",
    `connect-src 'self' ${apiOrigin} ${wsOrigin}`,
    "frame-ancestors 'none'",
    "base-uri 'self'",
    "form-action 'self'",
    "object-src 'none'",
  ].join('; '),
  'X-Content-Type-Options': 'nosniff',
  'X-Frame-Options': 'DENY',
  'Referrer-Policy': 'no-referrer',
  'Permissions-Policy': 'camera=(), microphone=(), geolocation=()',
}

export default defineConfig({
  define: {
    // sockjs-client expects Node's `global`
    global: 'globalThis',
  },
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    port: 5173,
    strictPort: true,
  },
  preview: {
    port: 5173,
    strictPort: true,
    headers: securityHeaders,
  },
  build: {
    sourcemap: false,
  },
})
