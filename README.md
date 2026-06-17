# RoomWallah

RoomWallah is a broker-resistant full-stack property platform built for tenants, buyers, and verified property owners. This repository contains the Phase 0 foundational structure.

## Tech Stack

### Backend
- **Language**: Java 21
- **Framework**: Spring Boot 3.x
- **Build Tool**: Maven
- **Database**: PostgreSQL with Flyway Migrations
- **Security**: Spring Security (JWT Scaffolding)
- **API Documentation**: OpenAPI / Swagger (springdoc-openapi)
- **Other**: Lombok, MapStruct, Jakarta Validation, SLF4J, Redis Caching (Scaffolded)

### Frontend
- **Framework**: React 18+ (Vite)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **Routing**: React Router v6
- **State**: Zustand
- **HTTP Client**: Axios
- **Form Handling & Validation**: React Hook Form, Zod

### Infrastructure
- **Containerization**: Docker & Docker Compose
- **Key-Value Store**: Redis

---

## Getting Started

### Prerequisites
- **Java 21**
- **Maven**
- **Node.js** (v18+)
- **Docker & Docker Compose**

### Running with Docker Compose
To boot up the complete environment (PostgreSQL, Redis, Backend, Frontend):

1. Copy the environment variables:
   ```bash
   cp .env.example .env
   ```
2. Build and run the services:
   ```bash
   docker-compose up --build
   ```

- Backend API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Frontend App: `http://localhost:5173`

---

## Local Development

### Backend Local Run
1. Navigate to the `backend` folder:
   ```bash
   cd backend
   ```
2. Build the project:
   ```bash
   mvn clean compile
   ```
3. Run the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```

### Frontend Local Run
1. Navigate to the `frontend` folder:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the dev server:
   ```bash
   npm run dev
   ```
