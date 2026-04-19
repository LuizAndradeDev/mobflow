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
  |          +--> Kafka topic: workspace.events
  |
  +--> task-service (8083, context-path /tasks) -> PostgreSQL: mobflow_task
             |
             +--> workspace-service /internal/workspaces/{id}/members/{authId}/role
             +--> user-service /internal/users/batch
             +--> Kafka topic: task.events
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
  +--> Kafka topic: auth.events

notification-service (8084)
  |
  +--> consumes task.events
  +--> consumes workspace.events
  +--> consumes auth.events
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
- Asynchronous service-to-service communication: Kafka topics `task.events`, `workspace.events`, `auth.events`, `social-comment-events`, `social-friendship-events`, and `social.events`.

## Technology Stack

| Layer | Technologies |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.x, Spring Security, JJWT 0.11.5, Spring Data JPA, Spring Data MongoDB, Spring Kafka, Spring Cache, Flyway, Validation, Lombok, Actuator, Thymeleaf, Maven Wrapper |
| Frontend | Angular 21, TypeScript 5.9 in strict mode, RxJS 7.8, Tailwind CSS 4 |
| Infrastructure | Docker, Docker Compose, PostgreSQL 16, MongoDB, Redis 7 Alpine, MinIO, Apache Kafka (Confluent 7.6), Zookeeper |

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
├── .env                    # Centralized runtime configuration for all containers
├── .env.example            # Reference template for the local runtime configuration
├── init-db.sql             # PostgreSQL initialization script for relational services
├── nginx.conf              # Shared Nginx configuration
├── web-app.conf            # Frontend/static/proxy server block configuration
└── Dockerfile.nginx        # Multi-stage Angular build + Nginx runtime image
```

## Prerequisites

- Docker Desktop `24+`

Java, Maven, Node.js, and npm do not need to be installed locally to run the full platform. All application images, including the Angular frontend, are built inside Docker.

If you want to work on the Angular frontend outside Docker, use Node.js `20.19+` and npm `11+`.

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
cd <repository-folder>
```

3. Ensure no other local application is using the ports required by Mobflow: `80`, `5432`, `6379`, `8025`, `8080` to `8086`, `9000`, `9001`, `9092`, and `27017`.

4. Create your local `.env` file from the committed template.

```bash
cp .env.example .env
```

5. Generate a Base64-encoded 256-bit secret and replace the placeholder value of `JWT_SECRET`.

6. Define a value for `INTERNAL_SECRET=replace_with_internal_secret`. This secret is used by synchronous internal service-to-service calls through `/internal/**` endpoints.

7. Open a command prompt, navigate to the project directory, and then start PostgreSQL first.

```bash
docker compose up -d postgres
```

8. Confirm that the databases defined in `init-db.sql` were created correctly.

```bash
docker compose exec postgres sh -lc 'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d postgres -c "\l"'
```

The expected databases are `mobflow_auth`, `mobflow_user`, `mobflow_workspace`, and `mobflow_task`.

If those databases are missing on a reused PostgreSQL volume, recreate the PostgreSQL data volume before retrying so `init-db.sql` is applied again.

9. Start the rest of the application after PostgreSQL is ready.

```bash
docker compose up --build -d \
  mongodb \
  zookeeper \
  kafka \
  mailhog \
  redis \
  minio \
  auth-service \
  user-service \
  workspace-service \
  task-service \
  notification-service \
  social-service \
  chat-service \
  nginx
```

The platform is then available on `http://localhost`, served by the edge Nginx container. Nginx delivers the production Angular build, handles SPA route refreshes, and proxies HTTP and WebSocket traffic to the backend containers.

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
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/tasks/actuator/health
curl http://localhost:8084/actuator/health
```

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
| `web-app` | `n/a (built into nginx)` | Browser state | Angular client with landing, onboarding, workspace, task, analytics, profile, and settings flows. |

## Authentication Model

Mobflow uses stateless JWT authentication. The `auth-service` authenticates credentials and signs a token whose standard subject is the username and whose custom `authId` claim is the canonical user identifier shared across the platform. Every backend service validates the token independently using the same `JWT_SECRET`, extracts `authId`, and uses that value as the actor identity for authorization decisions and data ownership.

Account creation is a two-step flow:

1. A new account is created in `auth-service` with `enabled=false`.
2. A UUID confirmation token with a 24-hour expiration window is generated and published as an `EMAIL_CONFIRMATION` event.
3. The `notification-service` sends the confirmation email.
4. Once the confirmation endpoint is called with the token, the account is enabled and can authenticate normally.

## Event-Driven Messaging

Kafka is used for cross-service notifications. Producers serialize self-contained event payloads with `KafkaTemplate<String, String>` and `ObjectMapper`. The `notification-service` consumes `String` payloads, deserializes them manually, and discards malformed events after logging them instead of rethrowing.

### Topics and Event Types

| Topic | Produced By | Event Types |
| --- | --- | --- |
| `task.events` | `task-service` | `TASK_CREATED`, `TASK_ASSIGNED`, `TASK_UPDATED`, `TASK_DELETED`, `TASK_COMPLETED`, `TASK_DUE_SOON` |
| `workspace.events` | `workspace-service` | `WORKSPACE_INVITE`, `WORKSPACE_MEMBER_ADDED`, `WORKSPACE_MEMBER_REMOVED`, `WORKSPACE_ROLE_CHANGED` |
| `auth.events` | `auth-service` | `EMAIL_CONFIRMATION` |
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
