# LabaLink Attendant Dashboard

React 19 + TypeScript + Vite + Tailwind 4 single-page app for shop
attendants. Customers use the Android app instead.

```bash
cp .env.example .env.local   # VITE_API_URL=http://localhost:8080
npm ci
npm run dev                  # http://localhost:5173
npm run lint && npm run build
```

| Path | Contents |
|---|---|
| `src/lib/` | `api.ts` (axios client, auth header, 401 handling), `session.ts` (token in sessionStorage), `config.ts`, `format.ts` |
| `src/services/` | One module per API area (auth, machines, bookings, payments, users) |
| `src/hooks/useWebSocket.ts` | Authenticated STOMP connection with auto-reconnect |
| `src/pages/` | Login, OAuth2 callback, Machines, Bookings, Transactions, Staff, payment result |
| `public/_headers` | Production security headers (CSP etc.) for static hosts |

See the [root README](../README.md), [UI/UX design](../docs/UI-UX-Design.md)
and [SECURITY.md](../SECURITY.md).
