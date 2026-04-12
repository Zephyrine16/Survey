# 📊 Menu Intelligence & Feedback Ecosystem

A professional full-stack application designed for high-volume menu evaluation and data-driven insights. This project utilizes a modern decoupled architecture with a Vue.js frontend and a Java Spring Boot backend.

**🔗 [Live Demo](https://survey-frontend-u6k3.onrender.com)**

---

## 🌟 Project Highlights

- **Dynamic Data Collection:** Custom-built survey engine with 84 curated items.
- **Admin Analytics:** Real-time dashboard featuring data visualization of user sentiment.
- **Enterprise Security:** Stateless JWT-based authentication for secure admin access.
- **Cloud Native:** Fully containerized logic deployed on Render with PostgreSQL persistence.

---

## 🛠️ Technical Stack

| Component | Technology |
| :--- | :--- |
| **Frontend** | Vue 3 (Composition API), Vite, Axios |
| **Backend** | Java 17, Spring Boot, Spring Security |
| **Database** | PostgreSQL |
| **Auth** | JWT (JSON Web Tokens) |
| **Hosting** | Render (CI/CD Pipeline) |

---

## 📂 Project Structure

This is a **Monorepo** containing both the client and server code:

```text
/Survey
├── /backend     # Spring Boot API & Database logic
└── /frontend    # Vue.js 3 Single Page Application
```

---

## 🚀 Getting Started (Local Development Setup)

Welcome to the project! If you are starting from scratch and don't have a local development environment set up for Java or Vue.js, follow these step-by-step instructions to get the application running on your machine.

### 1️⃣ Prerequisites: The Tools You Need
Before downloading the code, ensure you have the following installed on your computer:

#### 💻 IDE (Code Editor)

Choose the right development environment for each part of the project:

**For Backend (Recommended):**  
[IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/) - Highly recommended for Spring Boot development

**For Frontend:**  
[Visual Studio Code (VS Code)](https://code.visualstudio.com/) - Lightweight and powerful for Vue.js

> **💡 Tip:** You can also use VS Code for the backend if you prefer a single editor for both parts of the project.

---

#### ☕ Java Development Kit (JDK)

**Version 17 or higher required**

[Download Eclipse Temurin JDK](https://adoptium.net/)

---

#### 🟢 Node.js & npm

Required to run the Vue.js frontend. Download the **LTS** (Long Term Support) version.

[Download Node.js](https://nodejs.org/)

---

#### 🐘 PostgreSQL

The relational database used for this project.

[Download PostgreSQL](https://www.postgresql.org/download/)

> **💡 Important:** Remember the username and password you set during installation!

---

### 2️⃣ Database Setup

Before running the application, we need to give it a place to store data.

> **⚠️ Note:** The backend will instantly crash on startup if PostgreSQL is not installed and running, as it needs to build a connection pool.

---

#### 🔨 Create Database

1. Open your PostgreSQL tool (pgAdmin or DBeaver)
2. Create a new database named `postgres` (or your preferred database name)

---

#### ✨ Auto-Generated Schema

You do **not** need to create the tables manually.

Spring Boot will automatically:
- Generate the schema
- Seed the initial data

This happens when the server starts for the first time.

---

### 3️⃣ Backend Setup (Spring Boot)

The backend serves the API and connects to the database.

---

#### 📦 Clone the Repository

First, clone the repository and navigate to the backend folder:

```bash
git clone <your-repository-url>
cd <your-project-folder>/backend
```

---

#### 🔧 Configure Database Connection

Navigate to your application properties file and update the database credentials:

**Location:** `src/main/resources/application.properties` (or `.yml`)

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=YOUR_POSTGRES_USERNAME
spring.datasource.password=YOUR_POSTGRES_PASSWORD
```

> **💡 Tip:** Make sure these credentials match what you set up in PostgreSQL earlier.

---

#### 🚀 Run the Server

You have three options to start your Spring Boot application:

**Option 1: IntelliJ IDEA (Recommended)**

1. Open the `backend` folder in IntelliJ
2. Wait for Maven to download all dependencies
3. Locate `SurveyApplication.java`
4. Click the green **▶️ Run** button

**Option 2: VS Code**

1. Open the `backend` folder in VS Code
2. Install the **Extension Pack for Java** and **Spring Boot Extension Pack** from the extensions tab
3. Once installed, navigate to the `SurveyApplication.java` file and click the small `Run` inline link that appears above the `main` method, or use the "Spring Boot Dashboard" in the sidebar

**Option 3: Terminal/Command Line**

**Windows:**
```bash
mvnw.cmd spring-boot:run
```

**Mac/Linux:**
```bash
./mvnw spring-boot:run
```

---

✅ **Your backend should now be running!** Check the console for the port (usually `http://localhost:8080`)

### 4️⃣ Frontend Setup (Vue.js)
The frontend is the user interface you interact with in the browser.

#### 📋 Prerequisites

> **⚠️ Important:** Keep your backend server running! Open a **new** terminal window for the frontend setup.

---

#### 📂 Navigate to Frontend

```bash
cd <your-project-folder>/frontend
```
---

#### 📥 Install Dependencies

Install all required Node modules:

```bash
npm install
```
⏱️ *This may take a minute or two...*

---

#### ▶️ Start Development Server

Launch the Vue development server:

```bash
npm run dev
```

---

#### 🌐 Access the Application

Once the server starts, you'll see a local URL in the terminal (usually `http://localhost:5173`)

**To open the app:**
- **Windows/Linux:** `Ctrl` + `Click` on the URL
- **Mac:** `Cmd` + `Click` on the URL

Or simply copy the URL and paste it into your browser!

---

✅ **Success!** Your frontend should now be live and connected to the backend. Happy coding! 🎉