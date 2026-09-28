# campus-marketplace

A campus marketplace web app. CISC 498.

The repo is a monorepo with two independent apps:

| Path        | What it is                                  |
| ----------- | ------------------------------------------- |
| `frontend/` | React + TypeScript SPA, built with Vite     |
| `backend/`  | Java 21 + Spring Boot REST API, built with Maven |

The two do not talk to each other yet — each just runs on its own.

## Prerequisites

- Node.js 22+
- JDK 21

Maven does not need to be installed; the backend ships the Maven wrapper (`./mvnw`).

## Running the frontend

```bash
cd frontend
npm install     # first time only
npm run dev     # http://localhost:5173
```

Other scripts: `npm run build` (typecheck + production build), `npm run lint`, `npm run preview`.

## Running the backend

```bash
cd backend
./mvnw spring-boot:run    # http://localhost:8080
```

There are no endpoints yet, so a request to `/` returns a 404 — that response is
itself the sign the server is up.

Other commands: `./mvnw test`, `./mvnw package` (jar lands in `backend/target/`).
