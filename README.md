<p align="center">
  <a href="https://www.linkedin.com/in/luizsouzandrade/">
    <img src="https://img.shields.io/badge/LinkedIn-Luiz%20Souza%20Andrade-0A66C2?logo=linkedin&logoColor=white" alt="LinkedIn">
  </a>
  <a href="https://github.com/LuizAndradeDev/mobflow">
    <img src="https://img.shields.io/github/commit-activity/t/LuizAndradeDev/mobflow?label=Total%20Commits" alt="Total commits">
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-Attribution--NonCommercial-2ea44f" alt="License">
  </a>
</p>

<p align="center">
  <img src="content/mobflow_logo.png" alt="Mobflow logo" width="100" height="100">
</p>

<h1 align="center">Mobflow</h1>

<p align="center"><strong>A collaborative platform for teams</strong></p>

<p align="center">
  Mobflow is a portfolio-grade SaaS simulation that explores modern collaboration workflows with microservices, event-driven communication, and production-style local orchestration.
</p>

<details>
  <summary><strong>Table of Contents</strong></summary>

  - [Overview](#overview)
    - [Built With](#built-with)
    - [Demonstration](#demonstration)
  - [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Environment Configuration](#environment-configuration)
    - [Running the Platform](#running-the-platform)
    - [Local Tools](#local-tools)
    - [Account Creation](#account-creation)
  - [Architecture & Technical Details](#architecture--technical-details)
    - [System Flow](#system-flow)
    - [Layer Responsibilities](#layer-responsibilities)
    - [Service Catalog](#service-catalog)
    - [Authentication and Internal Communication](#authentication-and-internal-communication)
    - [Event-Driven Messaging](#event-driven-messaging)
    - [Observability](#observability)
    - [Validation Pipeline](#validation-pipeline)
  - [Additional Documentation](#additional-documentation)
  - [License](#license)
</details>

## Overview

Mobflow is a collaborative platform for teams that combines authentication, workspaces, task execution, social interactions, notifications, and realtime chat in a single product experience. It is built as a monorepo with independent backend services, an Angular frontend, and a Docker-based local platform that mirrors the concerns of a real SaaS system.

The project exists to understand and simulate how a production-oriented SaaS can be structured across multiple bounded contexts. Instead of presenting a simplified CRUD sample, Mobflow focuses on service boundaries, gateway routing, JWT-based security, asynchronous events with Kafka, polyglot persistence, local observability, and a developer workflow that reflects real platform engineering decisions.

### Built With

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-6DB33F?logo=springboot&logoColor=white)
![Spring Cloud Gateway](https://img.shields.io/badge/Spring%20Cloud-Gateway-6DB33F?logo=spring&logoColor=white)
![Angular](https://img.shields.io/badge/Angular-21-DD0031?logo=angular&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178C6?logo=typescript&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-Latest-47A248?logo=mongodb&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-7.6-231F20?logo=apachekafka&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-Unprivileged-009639?logo=nginx&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-Observability-E6522C?logo=prometheus&logoColor=white)
![Grafana](https://img.shields.io/badge/Grafana-Dashboards-F46800?logo=grafana&logoColor=white)

### Demonstration

#### 1. General Demonstration

Mobflow supports the full core flow from account registration and email confirmation to login, workspace access, task management, comments, and notifications.

Placeholder: add a walkthrough video showing account creation, authentication, task lifecycle, and notification flow.

#### 2. Chat Demonstration

The platform includes realtime private messaging with WebSocket delivery, conversation history, and read receipt support through the API Gateway and chat service.

Placeholder: add a GIF or short video showing realtime chat updates.

#### 3. Friendship System Demonstration

Mobflow includes a social layer where users can send and accept friend requests before accessing collaboration features that depend on user relationships.

Placeholder: add a short video showing the friendship request and acceptance flow.

#### 4. Comments and Mentions Demonstration

Task comments support collaborative discussion and `@mentions`, allowing users to trigger targeted notification events during task-related conversations.

Placeholder: add a short video showing comment creation, mention resolution, and notification generation.

## Getting Started

### Prerequisites

Required to run the full platform:

- Docker Engine or Docker Desktop with Docker Compose V2

Optional for local development outside containers:

- Java 21
- Maven 3.9+
- Node.js 24
- npm 11+
- OpenSSL or another secure secret generator

### Environment Configuration

Mobflow uses the root `.env` file as the shared runtime configuration for Docker Compose and the backend services.

1. Copy the reference file:

```bash
cp .env.example .env
```

2. Generate a Base64 JWT secret and place it in `JWT_SECRET`:

```bash
openssl rand -base64 32
```

3. Generate the shared internal service secret and place it in `INTERNAL_SECRET`:

```bash
openssl rand -hex 32
```

Key points:

- `JWT_SECRET` is used by all services that validate access tokens.
- `INTERNAL_SECRET` secures synchronous `/internal/**` calls between services through the `X-Internal-Secret` header.
- `.env.example` is the reference template; `.env` is your local runtime file.

### Running the Platform

1. Verify Docker is available:

```bash
docker version
```

2. Clone the repository and enter the project directory:

```bash
git clone https://github.com/LuizAndradeDev/mobflow.git
cd mobflow
```

3. Create and populate `.env` as described above.

4. Start PostgreSQL first on a clean setup:

```bash
docker compose up -d postgres
```

5. Verify that the isolated relational databases were created from `init-db.sql`:

```bash
docker compose exec postgres sh -lc 'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d postgres -c "\l"'
```

Expected databases:

- `mobflow_auth`
- `mobflow_user`
- `mobflow_workspace`
- `mobflow_task`

6. Start the rest of the platform:

```bash
docker compose up --build -d
```

7. Validate the main edge and gateway:

```bash
docker compose ps
curl http://localhost/health
curl http://localhost:8087/actuator/health
```

Useful maintenance commands:

```bash
docker compose down
docker compose down -v
docker compose logs -f <service-name>
```

Optional frontend development outside Docker:

```bash
cd web-app
npm install
npm start
```

### Local Tools

The following local tools are exposed by Docker Compose:

| Tool | URL | Purpose |
| --- | --- | --- |
| API Gateway Actuator | `http://localhost:8087/actuator/health` | Direct gateway diagnostics without going through the edge Nginx layer. |
| MailHog | `http://localhost:8025` | Email inspection for account confirmation and notification delivery. |
| MinIO API | `http://localhost:9000` | Object storage endpoint used by avatar upload flows. |
| MinIO Console | `http://localhost:9001` | Storage inspection and bucket management. |
| Prometheus | `http://localhost:9090` | Metrics querying and scrape target inspection. |
| Grafana | `http://localhost:3000` | Pre-provisioned dashboards for local observability. |

### Account Creation

1. Open the application in the browser.
2. Register with username, email, and password.
3. After registration, wait for the confirmation message.
4. Open MailHog at `http://localhost:8025`.
5. Open the confirmation email and follow the confirmation link.
6. Return to the application and log in with the confirmed account.

## Architecture & Technical Details

### System Flow

Mobflow currently follows this request and integration path:

```text
Client
  -> Nginx
    -> API Gateway
      -> Microservices
        -> Kafka
        -> PostgreSQL / MongoDB / Redis / MinIO
```

In practice:

- the browser loads the Angular application from Nginx;
- Nginx forwards `/api` HTTP traffic and chat WebSocket traffic to the API Gateway;
- the API Gateway centralizes external routing, JWT enforcement, and header propagation;
- backend services handle their bounded contexts and publish/consume Kafka events where asynchronous communication is required.

### Layer Responsibilities

#### Client and Edge

- `web-app` is the Angular client used by end users.
- `nginx` serves the production Angular bundle, handles SPA refreshes, and proxies API and WebSocket traffic.

#### Gateway Layer

- `api-gateway` is the single public backend entry point.
- It routes `/api/**` traffic to the appropriate services, validates JWT-based access, and propagates request context headers.

#### Core Domain Services

- `auth-service` handles registration, login, account confirmation, and token issuance.
- `user-service` manages profile data, avatars, caching, and profile reads.
- `workspace-service` owns workspaces, invites, membership, and roles.
- `task-service` owns boards, lists, tasks, ordering, and task analytics.

#### Collaboration Services

- `social-service` manages friendships, comments, and mentions.
- `chat-service` manages private messaging and realtime chat delivery.
- `notification-service` consumes platform events and delivers in-app and email notifications.

#### Messaging and Storage

- Kafka is the event backbone for cross-service notification flows.
- PostgreSQL is used for transactional relational domains.
- MongoDB is used for social, chat, and notification document models.
- Redis is used for caching.
- MinIO is used for object storage.

#### Observability

- Prometheus scrapes service metrics.
- Grafana exposes dashboards for local inspection.

### Service Catalog

| Service | Runtime Role | Primary State |
| --- | --- | --- |
| `api-gateway` | Public backend entry point, routing, auth enforcement, header propagation | Stateless |
| `auth-service` | Authentication, account confirmation, JWT issuance | PostgreSQL |
| `user-service` | Profiles, avatars, cached reads | PostgreSQL, Redis, MinIO |
| `workspace-service` | Workspaces, membership, invites, roles | PostgreSQL |
| `task-service` | Boards, lists, tasks, analytics | PostgreSQL |
| `social-service` | Friendships, comments, mentions | MongoDB |
| `chat-service` | Private chat, message history, WebSocket delivery | MongoDB |
| `notification-service` | Event consumption, notifications, email delivery | MongoDB |

### Authentication and Internal Communication

Mobflow uses stateless JWT authentication. The `auth-service` signs tokens, and the downstream services validate them independently using the shared JWT secret.

Internal synchronous service-to-service communication uses `/internal/**` endpoints protected by `X-Internal-Secret`. This keeps external access centralized through the API Gateway while preserving explicit service boundaries for internal validation and enrichment calls.

### Event-Driven Messaging

Kafka supports asynchronous communication between services. Current topics include:

- `auth-events`
- `task-events`
- `workspace-events`
- `social-comment-events`
- `social-friendship-events`
- `social.events`

These events drive notification workflows and decouple services that do not need synchronous orchestration for every user action.

### Observability

Mobflow exposes local observability through:

- Spring Boot Actuator endpoints on backend services
- Prometheus scraping for metrics collection
- Grafana dashboards for service visibility
- Correlation ID propagation across HTTP requests

The edge health endpoint is available at `http://localhost/health`, and the API Gateway actuator is available at `http://localhost:8087/actuator`.

### Validation Pipeline

The CI workflow is intentionally focused on validation rather than deployment. It currently covers:

- backend build and tests per service
- backend quality checks
- frontend tests and production build
- Docker image build validation
- repository security scanning

Docker image publishing is not part of the workflow, and Docker Compose startup is kept as a local validation concern instead of a CI requirement at this stage.

Useful local validation commands:

```bash
scripts/test-backend.sh
scripts/ci/check-backend-quality.sh
scripts/ci/build-docker-images.sh
scripts/ci/smoke-compose.sh
```

## Additional Documentation

- [API Gateway](api-gateway/README.md)
- [Auth Service](auth-service/README.md)
- [User Service](user-service/README.md)
- [Workspace Service](workspace-service/README.md)
- [Task Service](task-service/README.md)
- [Social Service](social-service/README.md)
- [Chat Service](chat-service/README.md)
- [Notification Service](notification-service/README.md)

## License

This project is licensed under the [Mobflow Attribution-NonCommercial License 1.0](LICENSE).
