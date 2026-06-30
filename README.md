# 🌱 CultivationX

**AI-Powered Developer Career Growth Platform**

CultivationX helps developers analyze resumes, identify skill gaps, track coding progress (GitHub + LeetCode), push LeetCode solutions to GitHub with AI review and auto-generated README, and get AI-powered mentorship — all in one dashboard.

---

## Features

| Module | Description |
|--------|-------------|
| **Rise** | AI resume parser + ATS score + skill gap analysis |
| **DevDNA** | AI-powered developer personality & growth report |
| **Nexus** | GitHub OAuth sync & LeetCode stats tracking |
| **Mentor** | AI coding mentor with code review |
| **LeetGit+** | Submit LeetCode solutions → AI review → auto-push to GitHub |
| **Dashboard** | Unified progress tracking across all modules |

---

## Tech Stack

**Backend**
- Java 21 + Spring Boot 3.5
- Spring AI (Groq API)
- Spring Security + JWT Authentication
- Spring Data JPA + MySQL
- Apache Tika (PDF/DOCX parsing)

**Frontend**
- React 18 + TypeScript
- Vite

**DevOps**
- Docker + Docker Compose
- Nginx (reverse proxy)

---

## Quick Start

### Prerequisites

- Java 21
- Maven 3.9+
- Node.js 18+
- MySQL 8.0

### Project Structure
```
CultivationX/
├── backend/
│   ├── src/main/java/com/cultivationx/
│   │   ├── auth/
│   │   ├── rise/
│   │   ├── mentor/
│   │   ├── nexus/
│   │   ├── devdna/
│   │   └── ai/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── application-example.yml
│   ├── .env
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── api/
│   │   └── types/
│   ├── public/
│   ├── package.json
│   └── vite.config.ts
├── docker-compose.yml
└── README.md
```

### 1. Clone

```bash
git clone https://github.com/aadityahammad-2002/CultivationX.git
cd CultivationX
```

### 2. Setup Environment

```bash
cp backend/.env.example backend/.env
```

Edit `backend/.env` with your keys:

```properties
GROQ_API_KEY=your-groq-api-key
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
JWT_SECRET=your-secret-min-32-chars
```

### 3. Start MySQL

```bash
docker run -d \
  --name mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -p 3306:3306 \
  mysql:8
```

### 4. Run Backend

```bash
cd backend
mvn clean spring-boot:run
```

API: `http://localhost:8080/api`

### 5. Run Frontend

```bash
cd frontend
npm install
npm run dev
```

App: `http://localhost:5173`

---

## Docker Setup

```bash
docker-compose up --build
```

---

## API Keys Setup

**Groq**
- Sign up at [console.groq.com](https://console.groq.com)
- Create API key

**GitHub OAuth**
- Settings → Developer settings → OAuth Apps → New OAuth App
- Callback URL: `http://localhost:8080/api/auth/github/callback`

---

## Screenshots

<p align="center">
  <img src="screenshots/dashboard.png" width="32%" alt="Dashboard" />
  <img src="screenshots/rise-upload.png" width="32%" alt="Rise Upload" />
  <img src="screenshots/rise-analysis.png" width="32%" alt="Rise Analysis" />
</p>

<p align="center">
  <img src="screenshots/rise-skills.png" width="32%" alt="Rise Skills" />
  <img src="screenshots/rise-gap.png" width="32%" alt="Rise Gap Analysis" />
  <img src="screenshots/rise-roadmap.png" width="32%" alt="Rise Roadmap" />
</p>

<p align="center">
  <img src="screenshots/rise-history.png" width="32%" alt="Rise History" />
  <img src="screenshots/mentor-chat.png" width="32%" alt="Mentor AI Chat" />
  <img src="screenshots/mentor-review.png" width="32%" alt="Mentor Code Review" />
</p>

<p align="center">
  <img src="screenshots/mentor-history.png" width="32%" alt="Mentor History" />
  <img src="screenshots/nexus-overview.png" width="32%" alt="Nexus Overview" />
  <img src="screenshots/nexus-github.png" width="32%" alt="Nexus GitHub" />
</p>

<p align="center">
  <img src="screenshots/nexus-leetcode.png" width="32%" alt="Nexus LeetCode" />
  <img src="screenshots/nexus-leetgit.png" width="32%" alt="Nexus LeetGit" />
  <img src="screenshots/devdna-overview.png" width="32%" alt="DevDNA Overview" />
</p>

<p align="center">
  <img src="screenshots/devdna-skills.png" width="32%" alt="DevDNA Skills" />
  <img src="screenshots/devdna-analytics.png" width="32%" alt="DevDNA Analytics" />
  <img src="screenshots/devdna-insights.png" width="32%" alt="DevDNA Insights" />
</p>

<p align="center">
  <img src="screenshots/devdna-reports.png" width="32%" alt="DevDNA Reports" />
</p>

## Notes

- LeetGit+ sync is manual (LeetCode has no webhooks)
- LeetCode stats use a public proxy with occasional downtime
- Never commit `.env` or `application-local.yml`

---

## License

MIT
