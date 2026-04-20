# Mobflow

## Overview

Mobflow is a productivity and collaboration SaaS for teams that need a structured workspace model, task execution visibility, and event-driven notifications without collapsing all responsibilities into a single deployable unit. The project is implemented as a monorepo with independent Spring Boot microservices and an Angular frontend. It is designed to demonstrate production-grade engineering concerns: stateless authentication with JWT, asynchronous messaging with Kafka, polyglot persistence with PostgreSQL and MongoDB, Redis-backed caching, object storage with MinIO, and full containerized local orchestration with Docker Compose.

The platform is intended for engineering teams, product teams, and portfolio reviewers who want to inspect a realistic microservice-based collaboration system rather than a simplified CRUD sample. Each service owns a bounded slice of the domain and persists its own data store, while the frontend consumes the public HTTP APIs with Bearer authentication.

## Architecture

```text
Browser
  |
  v
nginx (80)
  |
  +--> static Angular web-app (Angular 21 production build)
  |
  +--> auth-service (8080) --------> PostgreSQL: mobflow_auth
  |
  +--> user-service (8081) --------> PostgreSQL: mobflow_user
  |                                   |
  |                                   +--> Redis 7 (profile cache)
  |                                   |
  |                                   +--> MinIO (avatar objects)
  |
  +--> workspace-service (8082) ----> PostgreSQL: mobflow_workspace
  |          |
  |          +--> user-service /internal/users/by-username/{username}
  |          +--> user-service /internal/users/batch
  |          +--> Kafka topic: workspace-events
  |
  +--> task-service (8083, context-path /tasks) -> PostgreSQL: mobflow_task
             |
             +--> workspace-service /internal/workspaces/{id}/members/{authId}/role
             +--> user-service /internal/users/batch
             +--> Kafka topic: task-events
  |
  +--> social-service (8085, context-path /social) -> MongoDB: social
  |          |
  |          +--> task-service /tasks/internal/tasks/{taskId}
  |          +--> user-service /internal/users/batch
  |          +--> workspace-service /internal/workspaces/{id}/members/{authId}/role
  |          +--> Kafka topics: social-comment-events, social-friendship-events
  |
  +--> chat-service (8086, context-path /chat) -> MongoDB: chat
             |
             +--> social-service /social/internal/social/friendships/{authId}/friends/{targetAuthId}
             +--> WebSocket endpoint: /chat/ws/chat
             +--> Kafka topic: social.events

auth-service
  |
  +--> Kafka topic: auth-events

notification-service (8084)
  |
  +--> consumes task-events
  +--> consumes workspace-events
  +--> consumes auth-events
  +--> consumes social-comment-events
  +--> consumes social-friendship-events
  +--> consumes social.events
  +--> MongoDB database: notifications
  +--> JavaMailSender + Thymeleaf
```

### Internal Communication Model

- Frontend to services: JWT Bearer token on public `/api/**` endpoints.
- Synchronous service-to-service calls: `/internal/**` endpoints protected by the `X-Internal-Secret` header.
- Realtime browser communication: STOMP over WebSocket through `chat-service` on `/chat/ws/chat`.
- Asynchronous service-to-service communication: Kafka topics `task-events`, `workspace-events`, `auth-events`, `social-comment-events`, `social-friendship-events`, and `social.events`.

## Technology Stack

| Layer | Technologies |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.x, Spring Security, JJWT 0.11.5, Spring Data JPA, Spring Data MongoDB, Spring Kafka, Spring Cache, Flyway, Validation, Lombok, Actuator, Thymeleaf, Maven Wrapper |
| Frontend | Angular 21, TypeScript 5.9 in strict mode, RxJS 7.8, Tailwind CSS 4 |
| Infrastructure | Docker, Docker Compose, PostgreSQL 16, MongoDB, Redis 7 Alpine, MinIO, Apache Kafka (Confluent 7.6), Zookeeper, Prometheus, Grafana |

## Repository Structure

```text
mobflow/
├── auth-service/           # Authentication, account confirmation, JWT issuance
├── user-service/           # User profiles, avatar upload, profile caching
├── workspace-service/      # Workspaces, membership, invite lifecycle, join codes
├── task-service/           # Boards, lists, tasks, drag-and-drop ordering, analytics
├── social-service/         # Friend graph, social interactions, task comments
├── chat-service/           # Private conversations, realtime messaging, read receipts
├── notification-service/   # Kafka consumer, notification persistence, email delivery
├── web-app/                # Angular frontend source code
├── docker-compose.yaml     # Full local orchestration for infra and applications
├── observability/          # Prometheus and Grafana provisioning
├── .env                    # Centralized runtime configuration for all containers
├── .env.example            # Reference template for the local runtime configuration
├── init-db.sql             # PostgreSQL initialization script for relational services
├── nginx.conf              # Shared Nginx configuration
├── web-app.conf            # Frontend/static/proxy server block configuration
└── Dockerfile.nginx        # Multi-stage Angular build + Nginx runtime image
```

## Prerequisites

- Docker Desktop `24+` or Docker Engine with Docker Compose V2

Java, Maven, Node.js, and npm do not need to be installed locally to run the full platform. All application images, including the Angular frontend, are built inside Docker.

If you want to work on the Angular frontend outside Docker, use Node.js `20.19+` and npm `11+`.

OpenSSL is used in the setup steps below to generate local secrets. Use an equivalent secure generator if OpenSSL is not available on your machine.

## Environment Configuration

All containers use the root `.env` file. Start by copying `.env.example` to `.env`. The example file contains the complete set of variables required by Docker Compose and the application containers.

```dotenv
# PostgreSQL connection shared settings
DB_HOST=postgres
DB_PORT=5432
DB_USER=mobflow
DB_PASSWORD=mobflow_secret

# Dedicated PostgreSQL databases, one per service
AUTH_DB=mobflow_auth
USER_DB=mobflow_user
WORKSPACE_DB=mobflow_workspace
TASK_DB=mobflow_task

# Stateless JWT configuration shared by every service
# JWT_SECRET must be a Base64-encoded 256-bit key
JWT_SECRET=replace_with_base64_256_bit_secret
JWT_EXPIRATION=86400000

# Shared secret for synchronous service-to-service /internal/** calls
INTERNAL_SECRET=replace_with_internal_secret

# Redis cache used by user-service
REDIS_HOST=redis
REDIS_PORT=6379

# MinIO object storage used by user-service avatar uploads
MINIO_ENDPOINT=http://minio:9000
MINIO_PUBLIC_URL=http://localhost:9000
MINIO_ROOT_USER=minio
MINIO_ROOT_PASSWORD=minio123
MINIO_BUCKET=mobflow-avatars

# MongoDB shared settings used by Mongo-backed services
MONGO_HOST=mongodb
MONGO_PORT=27017
MONGO_USER=mobflow-mongo
MONGO_PASSWORD=mongo-secret

# Kafka bootstrap server used by event producers and consumers
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Grafana local login used by Docker Compose
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=admin

# Event topics used across the platform
AUTH_EVENTS_TOPIC=auth-events
TASK_EVENTS_TOPIC=task-events
WORKSPACE_EVENTS_TOPIC=workspace-events
SOCIAL_COMMENT_EVENTS_TOPIC=social-comment-events
SOCIAL_FRIENDSHIP_EVENTS_TOPIC=social-friendship-events
SOCIAL_EVENTS_TOPIC=social.events

# Service base URLs used for synchronous internal requests
AUTH_SERVICE_URL=http://auth-service:8080
WORKSPACE_SERVICE_URL=http://workspace-service:8082
USER_SERVICE_URL=http://user-service:8081
TASK_SERVICE_URL=http://task-service:8083
SOCIAL_SERVICE_URL=http://social-service:8085

# SMTP configuration used by notification-service email delivery
MAIL_HOST=mailhog
MAIL_PORT=1025
MAIL_USERNAME=no-reply@mobflow.dev
MAIL_PASSWORD=
MAIL_SMTP_AUTH=false
MAIL_SMTP_STARTTLS=false

# Shared application settings
APP_BASE_URL=http://localhost
APP_CORS_ALLOWED_ORIGINS=http://localhost
APP_MAIL_MAX_ATTEMPTS=3
TASK_DUE_SOON_CRON=0 0 8 * * *
```

## PostgreSQL Initialization

`init-db.sql` must create the four isolated PostgreSQL databases used by the relational services.

```sql
CREATE DATABASE mobflow_auth;
CREATE DATABASE mobflow_user;
CREATE DATABASE mobflow_workspace;
CREATE DATABASE mobflow_task;
```

This file is mounted into the PostgreSQL container during local startup so each relational service can run Flyway migrations against its own database boundary. The script is executed only when PostgreSQL initializes an empty data directory for the first time.

## Running the Platform

### Local Setup

1. Verify that Docker is installed and running.

```bash
docker version
```

2. Clone the repository and enter the project directory.

```bash
git clone https://github.com/LuizAndradeDev/mobflow.git
cd mobflow
```

3. Ensure no other local application is using the ports required by Mobflow: `80`, `1025`, `3000`, `5432`, `6379`, `8025`, `8080` to `8086`, `9000`, `9001`, `9090`, `9092`, and `27017`.

4. Create your local `.env` file from the committed template.

```bash
cp .env.example .env
```

5. Generate a value for `JWT_SECRET` and place it in the `.env` file.

```bash
openssl rand -base64 32
```

6. Generate a value for `INTERNAL_SECRET` and place it in the `.env` file.

```bash
openssl rand -hex 32
```

`INTERNAL_SECRET` must be the same for all backend services because synchronous `/internal/**` requests are authorized with the `X-Internal-Secret` header.

7. Start PostgreSQL first.

```bash
docker compose up -d postgres
```

8. Confirm that the databases defined in `init-db.sql` were created correctly.

```bash
docker compose exec postgres sh -lc 'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d postgres -c "\l"'
```

The expected databases are `mobflow_auth`, `mobflow_user`, `mobflow_workspace`, and `mobflow_task`.

If those databases are missing on a reused PostgreSQL volume, recreate the PostgreSQL data volume before retrying so `init-db.sql` is applied again.

9. Start the rest of the platform after PostgreSQL is ready. Docker Compose will build the application images and start the dependent infrastructure containers.

```bash
docker compose up --build -d
```

The platform is then available on `http://localhost`, served by the edge Nginx container. Nginx delivers the production Angular build, handles SPA route refreshes, and proxies HTTP and WebSocket traffic to the backend containers.

Supporting local tools are also exposed by Docker Compose:

| Tool | URL | Purpose |
| --- | --- | --- |
| MailHog | `http://localhost:8025` | Inspect local email delivery, including account confirmation emails. |
| MinIO Console | `http://localhost:9001` | Inspect avatar objects stored by `user-service`. |
| Prometheus | `http://localhost:9090` | Inspect scrape targets and query backend metrics. |
| Grafana | `http://localhost:3000` | View the provisioned `Mobflow Backend Overview` dashboard. Default local login is `admin` / `admin` unless overridden in `.env`. |

## Observability

Every backend service exposes Spring Boot Actuator endpoints for health, liveness, readiness, info, metrics, and Prometheus scraping. Public operational endpoints are intentionally limited to the standard Actuator surface:

| Service | Actuator Base URL |
| --- | --- |
| `auth-service` | `http://localhost:8080/actuator` |
| `user-service` | `http://localhost:8081/actuator` |
| `workspace-service` | `http://localhost:8082/actuator` |
| `task-service` | `http://localhost:8083/tasks/actuator` |
| `notification-service` | `http://localhost:8084/actuator` |
| `social-service` | `http://localhost:8085/social/actuator` |
| `chat-service` | `http://localhost:8086/chat/actuator` |

Useful endpoints under each base URL:

```text
/health
/health/liveness
/health/readiness
/info
/metrics
/prometheus
```

Prometheus is configured in `observability/prometheus/prometheus.yml` and scrapes all seven backend services. Grafana is provisioned from `observability/grafana/` with Prometheus as the default datasource and a starter backend dashboard.

HTTP logs are emitted in a consistent key-value console format with service name, request method, path, status, and correlation ID. Incoming requests may pass `X-Correlation-ID`; when the header is missing or invalid, the service generates one and returns it in the response. Internal `RestClient` calls propagate the same header to downstream services.

## Account Creation

1. Open the application in a browser at `http://localhost`.

2. Go to the register page.

3. Fill in the registration form with a `username`, `email`, and `password`.

4. Submit the form.

5. After submission, the application displays a notice indicating that the email address must be confirmed.

6. Open MailHog at `http://localhost:8025`.

7. Open the received confirmation email and click the confirmation link.

8. After the account is confirmed, return to the application and log in normally.

### Optional: Start the Frontend in Development Mode

```bash
cd web-app
npm install
npm start
```

Development mode is optional and intended only for frontend iteration. It is not required to run Mobflow locally.

### Verification Commands

```bash
docker compose ps
curl http://localhost/health
curl http://localhost:8084/actuator/health
curl http://localhost:8086/chat/actuator/health
curl http://localhost:8080/actuator/prometheus
curl -H 'X-Correlation-ID: demo-request-001' http://localhost:8080/actuator/health -i
```

Use container logs when a service does not become ready:

```bash
docker compose logs -f <service-name>
```

To stop the platform while keeping local volumes:

```bash
docker compose down
```

To remove local volumes and force PostgreSQL to run `init-db.sql` again on the next startup:

```bash
docker compose down -v
```

### Backend Test Suite

The repository includes a backend-only test runner for the seven Spring Boot services:

```bash
scripts/test-backend.sh
```

The script runs `mvn test` for `auth-service`, `user-service`, `workspace-service`, `task-service`, `notification-service`, `social-service`, and `chat-service`. Running it outside Docker requires Java 21 and Maven locally. Docker must also be running because integration tests use Testcontainers and embedded infrastructure where appropriate.

## Service Catalog

| Service | Port | Database / State | Description |
| --- | --- | --- | --- |
| `auth-service` | `8080` | PostgreSQL `mobflow_auth` | Issues JWTs, stores credentials, confirms accounts by token, and exposes the authenticated profile identity. |
| `user-service` | `8081` | PostgreSQL `mobflow_user`, Redis, MinIO | Manages profile metadata, avatar uploads, and cached profile reads consumed by both frontend and internal services. |
| `workspace-service` | `8082` | PostgreSQL `mobflow_workspace` | Owns workspace creation, member roles, invite lifecycle, and public join-code access. |
| `task-service` | `8083` | PostgreSQL `mobflow_task` | Owns boards, task lists, tasks, drag-and-drop ordering, workspace summaries, and authenticated analytics endpoints. |
| `social-service` | `8085` | MongoDB `social` | Owns friendships, friend requests, task comments, comment mentions, and social notification event publication. |
| `chat-service` | `8086` | MongoDB `chat` | Owns private conversations, paginated message history, read receipts, WebSocket delivery, and chat notification publication. |
| `notification-service` | `8084` | MongoDB `notifications` | Persists in-app notifications, computes unread counters, marks notifications as read, and sends email notifications from Kafka events. |
| `nginx` | `80` | Static Angular assets + reverse proxy | Serves the production Angular build, handles SPA route refreshes, and proxies HTTP/WebSocket traffic to backend services. |
| `prometheus` | `9090` | Local time-series storage | Scrapes Micrometer metrics from every backend service. |
| `grafana` | `3000` | Local dashboard storage | Provides the provisioned Mobflow backend observability dashboard. |
| `web-app` | `n/a (built into nginx)` | Browser state | Angular client with landing, onboarding, workspace, task, analytics, profile, and settings flows. |

## Authentication Model

Mobflow uses stateless JWT authentication. The `auth-service` authenticates credentials and signs a token whose standard subject is the username and whose custom `authId` claim is the canonical user identifier shared across the platform. Every backend service validates the token independently using the same `JWT_SECRET`, extracts `authId`, and uses that value as the actor identity for authorization decisions and data ownership.

Account creation is a confirmation-gated flow:

1. A new account is created in `auth-service` with `enabled=false`.
2. A UUID confirmation token with a 24-hour expiration window is generated and published as an `EMAIL_CONFIRMATION` event.
3. The `notification-service` sends the confirmation email.
4. Once the confirmation endpoint is called with the token, the account is enabled and can authenticate normally.

## Event-Driven Messaging

Kafka is used for cross-service notifications. Producers serialize self-contained event payloads with `KafkaTemplate<String, String>` and `ObjectMapper`. The `notification-service` consumes `String` payloads, deserializes them manually, and discards malformed events after logging them instead of rethrowing.

### Topics and Event Types

| Topic | Produced By | Event Types |
| --- | --- | --- |
| `task-events` | `task-service` | `TASK_CREATED`, `TASK_ASSIGNED`, `TASK_UPDATED`, `TASK_DELETED`, `TASK_COMPLETED`, `TASK_DUE_SOON` |
| `workspace-events` | `workspace-service` | `WORKSPACE_INVITE`, `WORKSPACE_INVITE_ACCEPTED`, `WORKSPACE_INVITE_DECLINED`, `WORKSPACE_MEMBER_ADDED`, `WORKSPACE_MEMBER_REMOVED`, `WORKSPACE_ROLE_CHANGED` |
| `auth-events` | `auth-service` | `EMAIL_CONFIRMATION` |
| `social-comment-events` | `social-service` | `COMMENT_CREATED`, `USER_MENTIONED` |
| `social-friendship-events` | `social-service` | `FRIEND_REQUEST_SENT` |
| `social.events` | `chat-service` | `CHAT_MESSAGE_RECEIVED` |

All event payloads are self-contained. The `notification-service` does not call upstream services to enrich notification content after consuming an event.

## Service Documentation

- [auth-service README](auth-service/README.md)
- [user-service README](user-service/README.md)
- [workspace-service README](workspace-service/README.md)
- [task-service README](task-service/README.md)
- [social-service README](social-service/README.md)
- [chat-service README](chat-service/README.md)
- [notification-service README](notification-service/README.md)
