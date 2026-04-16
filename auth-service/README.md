# auth-service

## Responsibility in the Architecture

`auth-service` is the security boundary for account identity and credential lifecycle. It owns user credentials, password hashes, role assignment, account lock state, email-confirmation tokens, and JWT issuance. No other service persists login credentials or signs tokens. The service is therefore the authoritative source for whether a principal exists, whether it is enabled, and which `authId` must be propagated across the rest of the platform as the immutable user identifier.

## Main Capabilities and Business Rules

The service covers the full account entry flow for the platform:

- Registers a credential record with `enabled=false`.
- Generates a UUID email confirmation token valid for 24 hours.
- Authenticates users with username and password.
- Issues a stateless JWT containing `subject=username` and `authId=<UUID>`.
- Exposes the authenticated principal profile required by the frontend bootstrap flow.
- Tracks credential state such as role, failed login attempts, and account lock flags.

Business rules that shape the service:

- A newly created account cannot authenticate until email confirmation succeeds.
- Credential storage is centralized in a single PostgreSQL table.
- JWT signing is performed only here, but JWT validation is performed independently by every downstream service.
- The service publishes account-related events rather than sending emails directly.

## Service Stack

| Area | Technologies |
| --- | --- |
| Runtime | Java 21, Spring Boot 3.5.x |
| Security | Spring Security, JJWT 0.11.5 |
| Persistence | Spring Data JPA, PostgreSQL 16, Flyway |
| Messaging | Spring Kafka |
| Validation | Jakarta Validation |
| Build | Maven Wrapper |

## Internal Package Structure

| Package | Purpose |
| --- | --- |
| `config` | Security, application configuration, and infrastructure wiring. |
| `controller` | Public HTTP endpoints for signup, login, confirmation, and authenticated profile lookup. |
| `service` | Credential registration, authentication, confirmation, and JWT creation logic. |
| `repository` | JPA access to credential storage. |
| `model/entities` | Persistent credential model. |
| `model/dtos/request` | Input contracts for signup and login. |
| `model/dtos/response` | Output contracts for login, profile lookup, and structured errors. |
| `model/enums` | Security and error classification enums. |
| `security` | JWT helper and authentication filter support. |
| `kafka` | Event publishing for auth-related notifications. |
| `exception` | Application-specific exception handling. |

## Endpoints

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth/signup` | No | Creates a disabled account, stores credential data, and starts the email confirmation flow. |
| `GET` | `/api/auth/confirm-email?token={uuid}` | No | Validates a confirmation token and enables the corresponding account. |
| `POST` | `/api/auth/login` | No | Validates credentials and returns a signed JWT. |
| `GET` | `/api/auth/profile` | Bearer JWT | Returns the authenticated principal identity used by the frontend session bootstrap. |

## Request and Response Examples

### Sign Up

```http
POST /api/auth/signup
Content-Type: application/json
```

```json
{
  "username": "luiz.andrade",
  "email": "luiz@mobflow.dev",
  "password": "StrongPass#2026"
}
```

Example response:

```json
{
  "id": "0c34ed9e-bef9-4c17-9af6-b2c7a117a9b0",
  "username": "luiz.andrade",
  "email": "luiz@mobflow.dev",
  "role": "USER",
  "enabled": false,
  "failedLoginAttempts": 0,
  "accountNonLocked": true,
  "confirmationToken": "f0ec0886-beb6-4a90-949a-76e7d50ff56b"
}
```

### Confirm Email

```http
GET /api/auth/confirm-email?token=f0ec0886-beb6-4a90-949a-76e7d50ff56b
```

Example response:

```json
{
  "message": "Email confirmed successfully"
}
```

### Login

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "username": "luiz.andrade",
  "password": "StrongPass#2026"
}
```

Example response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJsdWl6LmFuZHJhZGUiLCJhdXRoSWQiOiIwYzM0ZWQ5ZS1iZWY5LTRjMTctOWFmNi1iMmM3YTExN2E5YjAiLCJleHAiOjE3NDU5MTAwMDB9.signature",
  "expiresIn": 86400000
}
```

### Authenticated Profile Lookup

```http
GET /api/auth/profile
Authorization: Bearer <jwt>
```

Example response:

```json
{
  "username": "luiz.andrade",
  "email": "luiz@mobflow.dev"
}
```

## Security

The service is stateless. `POST /api/auth/signup`, `POST /api/auth/login`, and `GET /api/auth/confirm-email` are public, while profile lookup requires a valid Bearer token. JWT tokens contain the username as the standard subject and the platform-wide `authId` as a custom claim. That claim is the identifier later consumed by `user-service`, `workspace-service`, `task-service`, and `notification-service`.

Unlike the other business services, `auth-service` does not expose `/internal/**` endpoints. It remains the token issuer rather than an internal query service. Security logic is concentrated in the JWT utilities and the authentication filter configured under `security`.

## Database

- Database: PostgreSQL `mobflow_auth`
- Migration strategy: Flyway
- Main table: `user_credential`

### Relevant Fields

| Field | Purpose |
| --- | --- |
| `id` | Canonical `authId` UUID propagated across the platform. |
| `username` | JWT subject and login identifier. |
| `email` | Target for confirmation and account communication. |
| `passwordHash` | Hashed password, never exposed in API responses. |
| `role` | Stored authorization role. |
| `enabled` | Indicates whether email confirmation completed successfully. |
| `failedLoginAttempts` | Supports login failure tracking. |
| `accountNonLocked` | Indicates whether the credential is currently locked. |
| `confirmationToken` | UUID used for email confirmation. |
| `confirmationTokenExpiresAt` | Expiration timestamp for the confirmation flow. |

### Flyway Migrations

- `V1`: creates `user_credential`
- `V2`: adds a unique index for `username`
- `V3`: adds `confirmation_token` and `confirmation_token_expires_at`

## Kafka Integration

`auth-service` is a producer on `auth.events`.

| Topic | Event Type | Trigger |
| --- | --- | --- |
| `auth.events` | `EMAIL_CONFIRMATION` | Published after registration so the notification pipeline can send the confirmation message. |

Event payloads are serialized manually with `ObjectMapper` and sent through `KafkaTemplate<String, String>`.

## Run in Isolation

```bash
docker compose up --build auth-service
```

The service depends on PostgreSQL and Kafka, so the compose startup will bring the required infrastructure up as needed.
