# user-service

## Responsibility in the Architecture

`user-service` owns the mutable profile surface that sits on top of the immutable identity created by `auth-service`. It stores user-facing data such as display name, bio, phone number, and avatar URL, and it exposes both public profile endpoints for the authenticated user and internal aggregation endpoints used by other services. It is also the only service allowed to write avatar objects to MinIO and the only service that caches profile reads in Redis.

## Main Capabilities and Business Rules

The service covers personal profile management and profile lookup required by the rest of the platform.

- Creates and updates the profile associated with an `authId`.
- Supports avatar upload to MinIO and persists the resulting public object URL.
- Returns the authenticated user profile through `/api/users/me`.
- Resolves profile data for other services through internal endpoints.
- Caches profile reads with `@Cacheable` and invalidates cache entries with `@CacheEvict`.

Business rules and boundaries:

- Credential data is not stored here; this service only owns profile metadata.
- The profile is keyed by `authId`, not by username.
- Other services use internal endpoints to resolve `displayName` and `avatarUrl` when rendering members, assignees, and related UI fragments.
- Profile cache correctness is maintained by evicting or refreshing state after updates.

## Service Stack

| Area | Technologies |
| --- | --- |
| Runtime | Java 21, Spring Boot 3.5.x |
| Security | Spring Security, JJWT 0.11.5 |
| Persistence | Spring Data JPA, PostgreSQL 16 |
| Cache | Spring Cache, Redis 7 |
| Object Storage | MinIO |
| Validation | Jakarta Validation |
| Build | Maven Wrapper |

## Internal Package Structure

| Package | Purpose |
| --- | --- |
| `config` | Security, Redis, MinIO, CORS, and rest client configuration. |
| `controller` | Public profile endpoints and internal lookup endpoints. |
| `services` | Profile lifecycle, avatar storage, and cache-aware orchestration. |
| `repository` | JPA access to profile persistence. |
| `model/entities` | Persistent user profile entity. |
| `model/dto/request` | Contracts for profile updates. |
| `model/dto/response` | Contracts for profile reads, batch resolution, and errors. |
| `security` | JWT validation and request authentication. |
| `exceptions` | Domain-specific error handling. |

## Endpoints

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/api/users/me` | Bearer JWT | Returns the profile of the authenticated user. |
| `PUT` | `/api/users/me` | Bearer JWT | Creates or updates the authenticated user's profile fields. |
| `PATCH` | `/api/users/me/avatar` | Bearer JWT | Uploads an avatar image to MinIO and updates `avatarUrl`. |
| `GET` | `/api/users/{authId}` | Bearer JWT | Returns a profile by `authId` for authenticated application flows. |
| `GET` | `/internal/users/by-username/{username}` | `X-Internal-Secret` | Resolves a user profile from a username during workspace invitation flows. |
| `POST` | `/internal/users/batch` | `X-Internal-Secret` | Resolves multiple users by `authId` for cross-service enrichment. |

## Request and Response Examples

### Create or Update Profile

```http
PUT /api/users/me
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "displayName": "Luiz Andrade",
  "bio": "Backend engineer focused on distributed systems and developer experience.",
  "phone": "+55-11-99999-0000"
}
```

Example response:

```json
{
  "authId": "0c34ed9e-bef9-4c17-9af6-b2c7a117a9b0",
  "displayName": "Luiz Andrade",
  "bio": "Backend engineer focused on distributed systems and developer experience.",
  "avatarUrl": "http://localhost:9000/mobflow-avatars/0c34ed9e-bef9-4c17-9af6-b2c7a117a9b0/avatar.png",
  "phone": "+55-11-99999-0000"
}
```

### Upload Avatar

```http
PATCH /api/users/me/avatar
Authorization: Bearer <jwt>
Content-Type: multipart/form-data
```

Form field:

```text
file=<binary image>
```

Example response:

```json
{
  "authId": "0c34ed9e-bef9-4c17-9af6-b2c7a117a9b0",
  "displayName": "Luiz Andrade",
  "bio": "Backend engineer focused on distributed systems and developer experience.",
  "avatarUrl": "http://localhost:9000/mobflow-avatars/0c34ed9e-bef9-4c17-9af6-b2c7a117a9b0/avatar-2026-04-16T10:15:30Z.png",
  "phone": "+55-11-99999-0000"
}
```

### Batch Internal Lookup

```http
POST /internal/users/batch
X-Internal-Secret: <internal-secret>
Content-Type: application/json
```

```json
[
  "0c34ed9e-bef9-4c17-9af6-b2c7a117a9b0",
  "7f45fb3d-0f7a-4c65-9ad2-df4e8da40cb7"
]
```

Example response:

```json
[
  {
    "authId": "0c34ed9e-bef9-4c17-9af6-b2c7a117a9b0",
    "displayName": "Luiz Andrade",
    "avatarUrl": "http://localhost:9000/mobflow-avatars/0c34ed9e-bef9-4c17-9af6-b2c7a117a9b0/avatar.png"
  },
  {
    "authId": "7f45fb3d-0f7a-4c65-9ad2-df4e8da40cb7",
    "displayName": "Maria Costa",
    "avatarUrl": "http://localhost:9000/mobflow-avatars/7f45fb3d-0f7a-4c65-9ad2-df4e8da40cb7/avatar.png"
  }
]
```

## Security

Public application endpoints require a valid Bearer token. The JWT filter validates the shared token signature, extracts `authId`, and makes that value available for service-layer ownership checks and profile retrieval. The service does not rely on session state.

`/internal/**` endpoints are `permitAll()` in Spring Security but are not anonymous in practice. They are guarded by a manually validated `X-Internal-Secret` header so only trusted backend services can use them. This pattern is used for workspace member enrichment and task assignee resolution.

## Database and Storage

- Relational database: PostgreSQL `mobflow_users`
- Cache: Redis
- Object storage: MinIO
- Migration strategy: application-managed relational schema with service-owned profile table

### Main Entity: `user_profile`

| Field | Purpose |
| --- | --- |
| `authId` | Cross-service user identifier sourced from JWT and `auth-service`. |
| `displayName` | Human-readable name rendered across the frontend. |
| `bio` | Short personal description shown on profile screens. |
| `avatarUrl` | Public URL of the stored avatar object. |
| `phone` | Optional contact field managed by the user. |

### Cache Strategy

- Profile reads are cached with `@Cacheable`.
- Updates and avatar changes evict stale profile entries with `@CacheEvict`.
- Redis is an optimization layer; PostgreSQL remains the source of truth.

## Kafka Integration

`user-service` does not produce or consume Kafka topics. It participates in the platform through synchronous internal APIs rather than event publication.

## Run in Isolation

```bash
docker compose up --build user-service
```

The container requires PostgreSQL, Redis, and MinIO to be available for full functionality.
