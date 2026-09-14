# 🐳 Docker Setup Guide — Menu Intelligence & Feedback Ecosystem

This guide walks you through running the full application stack locally using **Docker**.  
You do **not** need to install Java, Node.js, or PostgreSQL — Docker handles everything.

---

## 📋 Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Clone the Repository](#2-clone-the-repository)
3. [Configure Environment Variables](#3-configure-environment-variables)
4. [Build & Start the Containers](#4-build--start-the-containers)
5. [Access the Application](#5-access-the-application)
6. [Useful Commands](#6-useful-commands)
7. [Troubleshooting](#7-troubleshooting)

---

## 1. Prerequisites

Install the following tool on your machine (it includes both Docker Engine and Docker Compose):

| Tool | Download |
| :--- | :--- |
| **Docker Desktop** | https://www.docker.com/products/docker-desktop |

> [!IMPORTANT]
> After installing Docker Desktop, make sure it is **running** before proceeding. You should see the Docker whale icon in your system tray.

Verify your installation by opening a terminal and running:

```bash
docker --version
docker compose version
```

Both commands should print a version number without errors.

---

## 2. Clone the Repository

```bash
git clone https://github.com/Zephyrine16/Survey.git
cd Survey
```

---

## 3. Configure Environment Variables

The app requires two `.env` files — one for the backend and one for the frontend. Example files are already provided.

### Backend `.env`

```bash
# On macOS / Linux
cp backend/.env.example backend/.env

# On Windows (PowerShell)
Copy-Item backend\.env.example backend\.env
```

Open `backend/.env` and fill in the required values:

| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `DB_USERNAME` | PostgreSQL username | `postgres` |
| `DB_PASSWORD` | PostgreSQL password | `postgres` |
| `ADMIN_USER` | Admin login username | `admin` |
| `ADMIN_PASSWORD_HASH` | **Bcrypt hash** of the admin password | *(generate one — see tip below)* |
| `JWT_SECRET` | Secret key for signing JWT tokens (≥ 32 chars) | *(generate a random string)* |

> [!TIP]
> You can generate a bcrypt hash for your admin password at https://bcrypt-generator.com (use cost factor 10).  
> You can generate a random JWT secret by running: `openssl rand -base64 32`

> [!NOTE]
> The `DATABASE_URL` is **automatically overridden** by `docker-compose.yml` to point to the internal `postgres-db` container. You do not need to change it.

All other values in the file have safe defaults for local development and can be left as-is.

---

### Frontend `.env`

```bash
# On macOS / Linux
cp frontend/.env.example frontend/.env

# On Windows (PowerShell)
Copy-Item frontend\.env.example frontend\.env
```

Open `frontend/.env` and verify the values:

| Variable | Description | Default |
| :--- | :--- | :--- |
| `VITE_API_BASE_URL` | URL of the backend API | `http://localhost:8080` |
| `VITE_SURVEY_ITEM_LIMIT` | Max items shown per survey page | `15` |
| `VITE_SURVEY_TEXT_MAX_LENGTH` | Character limit for text responses | `250` |
| `VITE_SURVEY_BASELINE_TARGET` | Target participant count | `30` |
| `VITE_REPORT_FILENAME` | CSV export file name | `FoodPreferenceSurvey_Analytics.csv` |

> [!NOTE]
> The defaults work out of the box for local Docker development. Only change `VITE_API_BASE_URL` if you expose the backend on a different host or port.

---

## 4. Build & Start the Containers

From the **root of the project** (the folder containing `docker-compose.yml`), run:

```bash
docker compose up --build -d
```

What this does:
1. **Pulls** the `postgres:15-alpine` image and starts the database.
2. **Builds** the Spring Boot backend image (compiles Java via Maven inside Docker).
3. **Builds** the Vue.js frontend image (runs `npm install` + `npm run build`, then serves with Nginx).
4. **Starts** all three containers in the background (`-d` = detached mode).

> [!NOTE]
> The **first build** takes several minutes because Maven downloads dependencies and compiles the Java source. Subsequent builds are much faster thanks to Docker's layer cache.

You can watch the live logs as the containers start up:

```bash
docker compose logs -f
```

Wait until you see a line similar to:
```
survey-backend  | Started SurveyApplication in X.XXX seconds
```

---

## 5. Access the Application

| Service | URL |
| :--- | :--- |
| 🌐 **Frontend** | http://localhost |
| ⚙️ **Backend API** | http://localhost:8080 |
| 🐘 **PostgreSQL** | `localhost:5432` (user/pass from your `.env`) |

---

## 6. Useful Commands

| Action | Command |
| :--- | :--- |
| Start containers (detached) | `docker compose up -d` |
| Start and rebuild images | `docker compose up --build -d` |
| View live logs (all services) | `docker compose logs -f` |
| View logs for one service | `docker compose logs -f backend` |
| Stop containers (keep data) | `docker compose down` |
| Stop and **delete all data** | `docker compose down -v` |
| List running containers | `docker compose ps` |
| Open a shell in the backend | `docker exec -it survey-backend sh` |
| Open a shell in the database | `docker exec -it survey-postgres psql -U postgres` |

> [!CAUTION]
> `docker compose down -v` deletes the **PostgreSQL volume**, permanently erasing all survey data. Only use this for a clean reset.

---

## 7. Troubleshooting

### Port already in use

If you see an error like `Bind for 0.0.0.0:8080 failed: port is already allocated`, another process is using that port. Either stop the conflicting process, or edit `docker-compose.yml` to map to a different host port (e.g. `"8081:8080"`).

### Backend fails to connect to the database

The backend container starts immediately but the database may still be initialising. Docker Compose's `depends_on` only waits for the container to *start*, not for Postgres to be *ready*. If you see connection errors, wait 10–15 seconds and run:

```bash
docker compose restart backend
```

### Environment variable not picked up

Ensure your `.env` file is saved in exactly the right location (`backend/.env` and `frontend/.env`) with **no extra spaces** around the `=` sign, and that you ran `docker compose up --build` (not just `up`) to rebuild with the latest config.

### Frontend shows blank page or API errors

Open your browser's DevTools (F12 → Console/Network tab) and check for failed API requests. The most common cause is a wrong `VITE_API_BASE_URL` in `frontend/.env`. It must match the host and port your backend is accessible on.

---

## 🏗️ Architecture Overview

```
Browser
  │
  ▼
┌─────────────────────┐      port 80
│  survey-frontend    │  ◄──────────────  http://localhost
│  (Nginx + Vue SPA)  │
└────────┬────────────┘
         │ proxies /api requests
         ▼
┌─────────────────────┐      port 8080
│  survey-backend     │  ◄──────────────  http://localhost:8080
│  (Spring Boot)      │
└────────┬────────────┘
         │ JDBC
         ▼
┌─────────────────────┐      port 5432
│  survey-postgres    │
│  (PostgreSQL 15)    │
└─────────────────────┘
```

Data is persisted across container restarts using named Docker volumes:
- `postgres_data` — database files
- `backend_uploads` — user-uploaded files
