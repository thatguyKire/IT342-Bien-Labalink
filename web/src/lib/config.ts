// Backend origin, e.g. https://api.labalink.example. Set VITE_API_URL in
// web/.env.local (dev) or your hosting provider (prod).
export const API_BASE_URL = (
  import.meta.env.VITE_API_URL ?? 'http://localhost:8080'
).replace(/\/+$/, '');
