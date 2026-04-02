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
