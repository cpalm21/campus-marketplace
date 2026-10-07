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

## Database deployment

Database changes deploy to Supabase when pushed to `database_skeleton`.
The workflow in `.github/workflows/deploy-database.yml` first rebuilds a local
database from the migrations and sample seed, then deploys pending migrations.
Pull requests run validation only. Manual deployment is available in GitHub
Actions once the workflow is on the default branch; select `database_skeleton`
when running it. Other branches cannot deploy through this workflow.

Required repository Actions secrets: `SUPABASE_ACCESS_TOKEN`,
`SUPABASE_DB_PASSWORD`, and `SUPABASE_PROJECT_ID` (the hosted project reference,
not the local `project_id` in `config.toml`).

Install the local CLI with `npm ci`, then link the project:

```bash
npx supabase login
npx supabase link --project-ref YOUR_PROJECT_REF
npx supabase migration list
```

Create each new database change with `npx supabase migration new change_name`
and write the SQL in the generated file under `supabase/migrations/`. With Docker
running, use `npx supabase db start` and `npx supabase db reset --local` to check
the migrations and seed on your local database. Local reset deletes local data.
Review `npx supabase db push --dry-run` before committing changes.

`supabase/migrations/` is the source of deployed schema and data changes:

- `20261005172915_create_marketplace_tables.sql` creates the nine marketplace tables.
- `20261007143500_add_marketplace_reference_data.sql` inserts the universities and listing categories.

Add a new migration instead of editing one already applied. Keep the timestamp
prefix of applied migrations unchanged, even when renaming their descriptions.
SQL files elsewhere in the repo do not automatically run during deployment.
`supabase/seed.sql` contains demo users, listings, and related records for local
validation. It runs after migrations during local reset and is not uploaded to
the hosted database. To deploy additional data, add INSERT statements in a new
migration and push it to `database_skeleton`.

If tables were created manually in an existing hosted project, reconcile its
migration history before enabling deployment. Only mark a migration applied
after confirming its SQL matches the existing schema. Do not run a database
reset against the hosted project.
