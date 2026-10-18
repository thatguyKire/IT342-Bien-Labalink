// Backend origin, e.g. https://api.labalink.example. Set VITE_API_URL in
// web/.env.local (dev) or your hosting provider (prod).
export const API_BASE_URL = (
  import.meta.env.VITE_API_URL ?? 'http://localhost:8080'
).replace(/\/+$/, '');

// Public legal pages. Defaults to the copies in the GitHub repo.
const LEGAL_DOCS_URL =
  'https://github.com/thatguyKire/IT342-Bien-Labalink/blob/main/docs/legal';
export const TERMS_URL =
  import.meta.env.VITE_TERMS_URL ?? `${LEGAL_DOCS_URL}/Terms-of-Service.md`;
export const PRIVACY_URL =
  import.meta.env.VITE_PRIVACY_URL ?? `${LEGAL_DOCS_URL}/Privacy-Policy.md`;
