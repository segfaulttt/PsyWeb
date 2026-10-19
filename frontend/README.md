
# PsyWeb Frontend

Web application for the PsyWeb psychological consultation platform.

## Technology Stack

- React
- TypeScript
- Vite
- ESLint

## Requirements

- Node.js 24 LTS
- npm

## Local Development

Install dependencies:

```bash
cd frontend
npm ci
```

Start the development server:

```bash
npm run dev
```

Open http://localhost:5173/

## Quality Checks

Check the production build:

```bash
npm run build
```

Run ESLint:

```bash
npm run lint
```

## Project Structure

- `src/App.tsx` — root React component
- `src/App.css` — component styles
- `src/index.css` — global styles
- `src/main.tsx` — React entry point
- `public/images/` — static image assets

## Backend

The frontend and Spring Boot backend are developed as separate applications within the same repository.

Backend runs locally on port `8080`. Frontend runs on port `5173`.

API integration will be implemented in subsequent tasks.
