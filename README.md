# 📊 Menu Intelligence & Feedback Ecosystem

A professional full-stack application designed for high-volume menu evaluation and data-driven insights. This project uses a modern decoupled architecture with a Vue.js frontend and a Java Spring Boot backend.

**🔗 [Live Demo](https://survey-xi-ten.vercel.app/)**

---

## 🌟 Project Highlights

- **Dynamic Data Collection:** Custom-built survey engine with 84 curated items.
- **Admin Analytics:** Real-time dashboard featuring data visualization of user sentiment.
- **Enterprise Security:** Stateless JWT-based authentication for secure admin access.
- **Cloud Native:** Fully containerized backend deployed on Render, frontend hosted on Vercel, with Neon.tech Serverless PostgreSQL persistence.

---

## 🛠️ Technical Stack

| Component | Technology |
| :--- | :--- |
| **Frontend** | Vue 3 (Composition API), Vite, Axios |
| **Backend** | Java 21, Spring Boot, Spring Security |
| **Database** | Serverless PostgreSQL (Neon.tech) |
| **Auth** | JWT (JSON Web Tokens) |
| **Hosting** | Vercel (Frontend) & Render (Backend) |

---

## 📂 Project Structure

This is a **Monorepo** containing both the client and server code:

```text
/Survey
├── /backend     # Spring Boot API & Database logic
└── /frontend    # Vue.js 3 Single Page Application
```

---

## 🚀 Getting Started (Dockerized Setup)

This project is fully containerized using **Docker** and **Docker Compose**, making it incredibly easy to spin up the entire stack locally without installing Java, Node.js, or PostgreSQL on your host machine.

### 1️⃣ Prerequisites

You only need one tool installed on your computer:
- [Docker Desktop](https://www.docker.com/products/docker-desktop) (includes Docker Compose)

### 2️⃣ Configuration Setup

Before spinning up the containers, you need to set up your environment variables. 

1. Copy the example `.env` file in the backend directory:
   ```bash
   cp backend/.env.example backend/.env
   ```
   *(Update the values in `backend/.env` if necessary. The default `docker-compose.yml` configures the database connection for you automatically.)*

2. Copy the example `.env` file in the frontend directory:
   ```bash
   cp frontend/.env.example frontend/.env
   ```
   *(Update any necessary Cloudinary or analytics variables in `frontend/.env`.)*

### 3️⃣ Run the Application

Open your terminal in the root directory of the project (where `docker-compose.yml` is located) and run:

```bash
docker-compose up --build -d
```

This single command will:
1. Pull the necessary PostgreSQL image and start the database.
2. Build and start the Spring Boot backend container.
3. Build and start the Vue.js frontend container.

### 4️⃣ Access the Application

Once the containers are up and running, you can access the application at:

- **Frontend:** [http://localhost](http://localhost) (Served via port 80)
- **Backend API:** [http://localhost:8080](http://localhost:8080)

### 🛑 Stopping the Application

To gracefully stop and remove the containers, run:

```bash
docker-compose down
```

*(Note: Your database data is persisted in a Docker volume, so you won't lose your data when you bring the containers down.)*
