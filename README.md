# Personal Budget Management — Backend API

Spring Boot 3.3.5 REST API for the Personal Budget Management System.

## Tech Stack
- **Java**: 21
- **Framework**: Spring Boot 3.3.5
- **Security**: Spring Security 6 with Stateless JWT
- **Persistence**: Spring Data JPA / Hibernate
- **Database**: H2 (local development) / PostgreSQL or MySQL (production)
- **Documentation**: SpringDoc OpenAPI / Swagger UI

## API Endpoints Overview
- **Authentication**:
  - `POST /api/auth/register` — Register a new account
  - `POST /api/auth/login` — Login and receive JWT
  - `GET /api/auth/me` — Current user profile
  - `PUT /api/auth/me` — Update user profile
  - `PUT /api/auth/me/password` — Change password
- **Transactions**:
  - `GET /api/transactions` — Query transactions with filtering and pagination
  - `POST /api/transactions` — Create an income or expense transaction
  - `GET /api/transactions/{id}` — Retrieve transaction details
  - `PUT /api/transactions/{id}` — Update transaction
  - `DELETE /api/transactions/{id}` — Delete transaction
- **Budgets**:
  - `GET /api/budgets` — List monthly budgets with spending and utilization
  - `POST /api/budgets` — Set category spending limit
  - `PUT /api/budgets/{id}` — Update budget limit
  - `DELETE /api/budgets/{id}` — Remove budget
- **Savings Goals**:
  - `GET /api/savings-goals` — List savings goals with progress percentage
  - `POST /api/savings-goals` — Create savings goal
  - `PUT /api/savings-goals/{id}` — Update savings goal progress
  - `DELETE /api/savings-goals/{id}` — Delete savings goal
- **Categories**:
  - `GET /api/categories` — List user categories (auto-seeds defaults)
  - `POST /api/categories` — Add custom category
  - `PUT /api/categories/{id}` — Update category
  - `DELETE /api/categories/{id}` — Remove category
- **Dashboard & Analytics**:
  - `GET /api/dashboard/summary` — Aggregated monthly totals, balance, and alerts
  - `GET /api/reports/monthly` — 12-month cash flow trends
  - `GET /api/reports/categories` — Category spending breakdown
- **AI Assistant**:
  - `POST /api/ai/chat` — Financial assistant advice
- **Monitoring**:
  - `GET /actuator/health` — Service health probe

## Environment Variables
| Variable | Description | Default |
|---|---|---|
| `PORT` | Server listening port | `8080` |
| `DB_URL` | JDBC database connection URL | (local H2 file fallback) |
| `DB_USERNAME` | Database username | `sa` |
| `DB_PASSWORD` | Database password | empty |
| `DB_DRIVER` | Database JDBC driver | `org.h2.Driver` / `com.mysql.cj.jdbc.Driver` |
| `JWT_SECRET` | Secret key for JWT signing | generated / local fallback |
| `FRONTEND_URL` | Frontend domain for CORS | `http://localhost:5173` |
| `AI_API_KEY` | OpenAI API key for AI assistant | optional |

## Run Locally
```bash
mvn "-Dspring-boot.run.profiles=local" spring-boot:run
```

## Build Jar
```bash
mvn clean package -DskipTests
java -jar target/budget-management-backend-0.0.1-SNAPSHOT.jar
```
