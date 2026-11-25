# Getting started with Yvette

Yvette is an online boutique for Kenya (prices in KSh, payment by M-Pesa or card). It has three parts that run
together on your computer:

| Part | What it is | Address once running |
|---|---|---|
| **Database + email catcher** | PostgreSQL and Mailpit, run in Docker | Mailpit inbox: http://localhost:18025 |
| **API** (`apps/api`) | Java / Spring Boot back end | http://localhost:8080 |
| **Website** (`apps/web`) | Next.js shop and back office | Shop: http://localhost:3000 · Back office: http://localhost:3000/admin |

Everything here runs **locally**: the full product catalogue (26 products with photos), test payments, no real money and no real emails.

---

## 1. Prerequisites

Install these first (all free):

| Tool | Version | Get it | Check it works |
|---|---|---|---|
| **Java JDK** | 21 | [Adoptium Temurin 21](https://adoptium.net/temurin/releases/?version=21) | `java -version` shows `21` |
| **Node.js** | 20.9 or newer (22 or 24 LTS recommended) | [nodejs.org](https://nodejs.org/) | `node --version` |
| **Docker Desktop** | any recent | [docker.com](https://www.docker.com/products/docker-desktop/) | `docker info` runs without error |
| **Git** *(optional)* | any | [git-scm.com](https://git-scm.com/) | `git --version` |

You don't need to install Maven: the project includes its own (`mvnw` / `mvnw.cmd`).

**Computer:** at least 8 GB RAM free while everything runs (16 GB+ total recommended). Docker, the Java API and the
Next.js dev server together use several GB.

**Ports used:** 8080 (API), 3000 (website), 55432 (database), 6379 (Redis), 11025 and 18025 (Mailpit).
These must be free. If one is taken, see *Troubleshooting* below.

---

## 2. First-time setup

Unzip the folder, open a terminal in it (the folder containing this file), then:

```bash
# Website dependencies (one time, takes a few minutes)
cd apps/web
npm install
cp .env.example .env.local        # Windows Command Prompt: copy .env.example .env.local
cd ../..
```

The defaults in `.env.local` already point at the local API. Nothing needs changing.

---

## 3. Start it (every time)

Use **three steps, in this order**. Steps 2 and 3 each keep running in their own terminal window.

**Step 1: start Docker Desktop**, wait until it says it's running, then start the database and email catcher:

```bash
docker compose -f infra/docker-compose.yml up -d
```

**Step 2: start the API** (new terminal window, from the project folder):

```bash
cd apps/api
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

On Windows (PowerShell or Command Prompt) use:

```powershell
cd apps\api
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

The first start downloads libraries and takes a few minutes. It's ready when you see
`Started ApiApplication`. The `dev` profile loads the product catalogue and a local owner login.

**Step 3: start the website** (another new terminal window, from the project folder):

```bash
cd apps/web
npm run dev
```

It's ready when you see `Ready`. Open **http://localhost:3000**.

---

## 4. Try it

| Who | Where | Login |
|---|---|---|
| Store owner (back office) | http://localhost:3000/admin | `owner@yvette.local` / `yvette-dev-owner` |
| Shopper | http://localhost:3000/account/register | Register any email; the verification email arrives in Mailpit (http://localhost:18025) |

- **Discount codes:** `KARIBU10` (10% off), `FREESHIP` (free delivery over KSh 5,000).
- **Test payments (no real money):**
  - **M-Pesa:** choose M-Pesa at checkout, then press *Approve payment* on the next page.
  - **Card:** use `4242 4242 4242 4242`, any future date and any CVC. To see a declined payment, use `4000 0000 0000 0002`.
- **All emails** (order confirmations, password resets, shipping updates) land in Mailpit: http://localhost:18025.
- **Product spreadsheet:** in the back office go to *Products → Export CSV*, edit the file in Excel or Google Sheets,
  then use *Import CSV*. You see a preview before anything is saved.

---

## 5. Stop it

- Press `Ctrl + C` in the API and website terminal windows.
- Stop the database: `docker compose -f infra/docker-compose.yml down` (your data is kept; add `-v` to wipe it).

---

## 6. Run the tests *(optional)*

Docker must be running: the API tests start their own temporary database.

```bash
cd apps/api && ./mvnw test                       # Windows: .\mvnw.cmd test
cd apps/web && npm run lint && npx tsc --noEmit
```

---

## 7. Troubleshooting

| Problem | Fix |
|---|---|
| API says `Connection to localhost:55432 refused` | The database isn't running. Start Docker Desktop, then run Step 1 again. |
| `Port 8080 was already in use` | An earlier API is still running (closing the terminal doesn't always stop Java). Close it in Task Manager (a `java.exe` process) or run `npx kill-port 8080`, then start again. |
| Port 55432, 3000 or another port is taken | Stop the program using it, or change the port: the database port is in `infra/docker-compose.yml` and `apps/api/src/main/resources/application.yml` (`DB_URL`). |
| `docker: command not found` / `cannot connect to the Docker daemon` | Docker Desktop isn't installed or isn't started yet. |
| `java -version` shows something other than 21 | Install JDK 21 and make sure it's first on your `PATH` (or set `JAVA_HOME` to it). |
| Website shows "API ... failed" errors | Start the API (Step 2) and wait for `Started ApiApplication`, then reload. |
| `./mvnw: Permission denied` (Mac/Linux) | Run `chmod +x apps/api/mvnw` once. |
| Computer slows down or programs get closed | Not enough free memory. Close other heavy apps (browsers with many tabs, IDEs, other Docker containers). |

---

## More information

- **[README.md](README.md):** what's built, settings needed to go live, project layout.
- **[docs/](docs/):** project plan, requirements, actors and use cases, architecture, backlog, AI strategy,
  open questions and decisions.
