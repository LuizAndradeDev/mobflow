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
  +--> user-service (8081) --------> PostgreSQL: mobflow_users
  |                                   |
  |                                   +--> Redis 7 (profile cache)
  |                                   |
  |                                   +--> MinIO (avatar objects)
  |
  +--> workspace-service (8082) ----> PostgreSQL: mobflow_workspaces
  |          |
  |          +--> user-service /internal/users/by-username/{username}
  |          +--> user-service /internal/users/batch
  |          +--> Kafka topic: workspace.events
  |
  +--> task-service (8083, context-path /tasks) -> PostgreSQL: mobflow_tasks
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
├── nginx.conf              # Shared Nginx configuration
├── web-app.conf            # Frontend/static/proxy server block configuration
└── Dockerfile.nginx        # Multi-stage Angular build + Nginx runtime image
```

## Prerequisites

- Docker Desktop `24+`

Java, Maven, Node.js, and npm do not need to be installed locally to run the full platform. All application images, including the Angular frontend, are built inside Docker.

If you want to work on the Angular frontend outside Docker, use Node.js `20.19+` and npm `11+`.

## Environment Configuration

The Docker Compose file contains safe local defaults, so no `.env` file is required to boot the stack on a clean machine.

If someone wants to override ports, credentials, or service URLs, they can create an optional root `.env`. When no `.env` is present, Compose falls back to the built-in defaults below:

```dotenv
# PostgreSQL connection shared settings
POSTGRES_DB=postgres
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
JWT_SECRET=replace_with_base64_256_bit_secret
JWT_EXPIRATION=3600000

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

# MongoDB credentials used by notification-service
MONGO_USER=mobflow-mongo
MONGO_PASSWORD=mongo-secret
MONGO_HOST=mongodb
MONGO_PORT=27017

# Kafka bootstrap server used by event producers and consumers
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
AUTH_EVENTS_TOPIC=auth-events
TASK_EVENTS_TOPIC=task-events
WORKSPACE_EVENTS_TOPIC=workspace-events
SOCIAL_COMMENT_EVENTS_TOPIC=social-comment-events
SOCIAL_FRIENDSHIP_EVENTS_TOPIC=social-friendship-events

# Service base URLs used for synchronous internal requests
WORKSPACE_SERVICE_URL=http://mobflow-workspace:8082
USER_SERVICE_URL=http://mobflow-user:8081
TASK_SERVICE_URL=http://mobflow-task:8083
SOCIAL_SERVICE_URL=http://mobflow-social:8085

# SMTP configuration used by notification-service email delivery
MAIL_HOST=mailhog
MAIL_PORT=1025
MAIL_USERNAME=no-reply@mobflow.dev
MAIL_PASSWORD=

# Public base URL used in generated links such as email confirmation flows
APP_BASE_URL=http://localhost
```

## PostgreSQL Bootstrap

PostgreSQL startup is handled by a single idempotent bootstrap step:

1. `postgres` starts and is considered healthy only after it accepts authenticated connections.
2. `postgres-bootstrap` connects with the same admin credentials configured on the PostgreSQL container.
3. It creates `mobflow_auth`, `mobflow_user`, `mobflow_workspace`, and `mobflow_task` only when they do not already exist.
4. The relational Spring services start only after that bootstrap container exits successfully.

The bootstrap runs inline through `psql`, without mounted `.sh` or `.sql` files, so it is stable on Linux, macOS, and Windows.

Because the creation is idempotent, the same flow works on:

- first startup on a clean machine
- `docker compose up --build` after rebuilds
- restart with persistent Docker volumes already present

After the databases exist, each relational service runs its own Flyway migrations against its own schema boundary.

## Running the Platform

### Start the Full Stack

```bash
docker compose up --build
```

That single command is enough on a clean machine:

1. infrastructure containers start
2. PostgreSQL becomes healthy
3. `postgres-bootstrap` guarantees the required databases exist
4. Spring services start and run their own migrations
5. Nginx serves the frontend on `http://localhost`

No manual database creation, no extra bootstrap command, and no volume reset is required.

If someone previously initialized this project with older PostgreSQL credentials from an earlier setup version, Docker may still have an incompatible persisted volume. In that specific migration case only, reset the old local state with:

```bash
docker compose down -v
```

Then run `docker compose up --build` again.

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
docker compose logs postgres-bootstrap
curl http://localhost/health
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
