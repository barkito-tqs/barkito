# Barkito web UI

React 18 + TypeScript + Vite. One web app with an area per role (US-02): end user, operator, Field Technician and client manager.

```bash
npm install
npm run dev     # http://localhost:5173
npm test        # unit tests (Vitest)
npm run build   # type-check and production build
```

## Mock API

The backend exposes only the health endpoint so far, so `src/api/store.ts` stands in for the REST API and the WebSocket push. Each action matches a future endpoint and throws `ApiError` with the status code the API will return (400, 403, 404, 409). A telemetry simulator drains the battery of boats on a trip and charges plugged-in boats, so the low-battery alerts (BR7) and live fleet updates can be seen. State lives in memory and resets on reload.

Until sign-in arrives (US-01, I3), the sign-in page lets you pick one of the persona accounts from the Product Specification. In development, the Unlock page lists the codes printed below each boat's QR code.

## Structure

| Folder | Contents |
|---|---|
| `src/domain` | Business rules and prices (BR1–BR14), with unit tests. No React. |
| `src/api` | Mock API and seed data. Replace with REST calls once the endpoints exist. |
| `src/pages` | Screens per role: `user`, `operator`, `technician`, `client`. |
| `src/components` | App shell, canal map and shared UI. |
| `src/styles` | Design tokens (colours from the logo; Red Hat Display and Red Hat Text) and component styles. |

## Screens and stories

| Role | Screen | Stories |
|---|---|---|
| End user | Boats, with the floating Scan to Unlock button | US-09, US-10 |
| End user | Unlock (QR code or the code below it) | US-12 |
| End user | My trip | US-08, US-11, US-13, US-20, US-38 |
| End user | History, Wallet | US-29, US-16 |
| Operator | Fleet (5 per page), boat page with confirmed actions | US-03, US-05 to US-07, US-30 |
| Operator | Docks, dock page with its boats | US-04, US-30 |
| Operator | Tickets, Technicians (changes saved and confirmed) | US-21, US-23, US-24, US-39 |
| Field Technician | My tickets, ticket page | US-22, US-25 |
| Client manager | KPIs, Rules of use, Berths (changes saved and confirmed) | US-26 to US-28, US-31, US-32 |
