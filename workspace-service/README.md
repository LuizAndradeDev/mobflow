# workspace-service

## Responsibility in the Architecture

`workspace-service` owns team collaboration boundaries. It defines the lifecycle of workspaces, stores membership and member roles, controls who can invite or remove users, and exposes both public join-code access and invite-based onboarding. It is the authority that determines whether an `authId` is allowed to act inside a workspace and which role that principal holds. Downstream services, especially `task-service`, depend on this service for authorization context rather than duplicating membership state.

## Main Capabilities and Business Rules

The service handles collaboration scope and participant governance.

- Creates, updates, lists, and deletes workspaces.
- Manages membership with `OWNER`, `ADMIN`, and `MEMBER` roles.
- Supports joining a workspace through an 8-character public code.
- Supports invite workflows with pending, accepted, declined, and expired states.
- Publishes workspace lifecycle events to Kafka for downstream notifications.
- Exposes an internal membership-role endpoint used by `task-service`.

Core business rules:

- Only authorized members can modify workspace metadata.
- Owners and admins can invite users and manage role changes according to workspace policy.
- Invite expiration is seven days from issuance.
- Accepting an invite creates a `WorkspaceMember` record and publishes `WORKSPACE_MEMBER_ADDED`.
- The service resolves usernames to users through `user-service` internal endpoints instead of persisting profile data locally.

## Service Stack

| Area | Technologies |
| --- | --- |
| Runtime | Java 21, Spring Boot 3.5.x |
| Security | Spring Security, JJWT 0.11.5 |
| Persistence | Spring Data JPA, PostgreSQL 16 |
| Messaging | Spring Kafka |
| Validation | Jakarta Validation |
| HTTP Clients | Internal clients for `user-service` |
| Build | Maven Wrapper |

## Internal Package Structure

| Package | Purpose |
| --- | --- |
| `config` | Security, CORS, RestClient, and application configuration. |
| `controller` | Public workspace APIs and internal membership-role lookup API. |
| `service` | Workspace lifecycle, membership management, invite orchestration, and role validation. |
| `repository` | JPA access to workspaces, members, and invites. |
| `model/entities` | Persistent workspace, member, and invite models. |
| `model/dto/request` | Input contracts for create, update, invitation, and role changes. |
| `model/dto/response` | Workspace, member, invite, and role response contracts. |
| `model/enums` | Role and invite-status enumerations. |
| `security` | JWT validation and request authentication. |
| `client` | Calls to `user-service` internal APIs. |
| `kafka` | Event publication for workspace domain events. |
| `exception` | Structured exception handling for invalid membership and invite scenarios. |

## Endpoints

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/workspaces` | Bearer JWT | Creates a workspace and registers the creator as `OWNER`. |
| `GET` | `/api/workspaces` | Bearer JWT | Lists workspaces visible to the authenticated member. |
| `GET` | `/api/workspaces/{id}` | Bearer JWT | Returns a workspace with member-aware access control. |
| `GET` | `/api/workspaces/join/{code}` | Bearer JWT | Resolves a workspace by its public 8-character join code. |
| `POST` | `/api/workspaces/join/{code}` | Bearer JWT | Adds the authenticated user to the workspace referenced by the join code. |
| `PUT` | `/api/workspaces/{id}` | Bearer JWT | Updates workspace metadata. |
| `DELETE` | `/api/workspaces/{id}` | Bearer JWT | Deletes a workspace according to ownership rules. |
| `DELETE` | `/api/workspaces/{id}/leave` | Bearer JWT | Removes the authenticated user from the workspace membership set. |
| `POST` | `/api/workspaces/{id}/members` | Bearer JWT | Invites a user to the workspace by username. |
| `POST` | `/api/workspaces/{id}/invites` | Bearer JWT | Creates an invite record for the workspace. |
| `POST` | `/api/workspaces/invites/{inviteId}/accept` | Bearer JWT | Accepts an outstanding workspace invite and creates membership. |
| `POST` | `/api/workspaces/invites/{inviteId}/decline` | Bearer JWT | Declines an outstanding workspace invite. |
| `GET` | `/api/workspaces/{id}/members` | Bearer JWT | Lists members of a workspace with profile enrichment. |
| `DELETE` | `/api/workspaces/{id}/members/{memberAuthId}` | Bearer JWT | Removes a member from the workspace. |
| `PATCH` | `/api/workspaces/{id}/members/{memberAuthId}/role` | Bearer JWT | Changes a member role. |
| `GET` | `/internal/workspaces/{workspaceId}/members/{authId}/role` | `X-Internal-Secret` | Returns the member role for internal authorization checks in `task-service`. |

## Request and Response Examples

### Create Workspace

```http
POST /api/workspaces
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "name": "Mobflow Platform",
  "description": "Core product delivery workspace for platform engineering."
}
```

Example response:

```json
{
  "id": "5e74e8d2-cf5c-4f80-99e1-9468aa347fd3",
  "name": "Mobflow Platform",
  "description": "Core product delivery workspace for platform engineering.",
  "joinCode": "MBFLW123",
  "createdByAuthId": "0c34ed9e-bef9-4c17-9af6-b2c7a117a9b0"
}
```

### Invite a Member by Username

```http
POST /api/workspaces/5e74e8d2-cf5c-4f80-99e1-9468aa347fd3/members
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "username": "maria.costa"
}
```

Example response:

```json
{
  "id": "4a087d8d-43cc-4c19-b38a-c320a10efdf0",
  "workspaceId": "5e74e8d2-cf5c-4f80-99e1-9468aa347fd3",
  "inviteeAuthId": "7f45fb3d-0f7a-4c65-9ad2-df4e8da40cb7",
  "status": "PENDING",
  "token": "f0ec0886-beb6-4a90-949a-76e7d50ff56b",
  "expiresAt": "2026-04-23T13:00:00Z"
}
```

### Accept an Invite

```http
POST /api/workspaces/invites/4a087d8d-43cc-4c19-b38a-c320a10efdf0/accept
Authorization: Bearer <jwt>
```

Example response:

```json
{
  "workspaceId": "5e74e8d2-cf5c-4f80-99e1-9468aa347fd3",
  "memberAuthId": "7f45fb3d-0f7a-4c65-9ad2-df4e8da40cb7",
  "role": "MEMBER"
}
```

### Internal Role Check

```http
GET /internal/workspaces/5e74e8d2-cf5c-4f80-99e1-9468aa347fd3/members/0c34ed9e-bef9-4c17-9af6-b2c7a117a9b0/role
X-Internal-Secret: <internal-secret>
```

Example response:

```json
{
  "workspaceId": "5e74e8d2-cf5c-4f80-99e1-9468aa347fd3",
  "authId": "0c34ed9e-bef9-4c17-9af6-b2c7a117a9b0",
  "role": "OWNER"
}
```

## Security

All public workspace endpoints require a valid Bearer token. The JWT filter validates the token, extracts `authId`, and that value becomes the principal used for ownership checks, membership checks, and role transitions. The service does not trust usernames from the frontend as identity; it trusts only the token-derived `authId`.

The internal role-check endpoint under `/internal/**` is exposed without standard Spring authentication so other services can call it synchronously, but it is protected by a manually validated `X-Internal-Secret` header. This endpoint is used by `task-service` to authorize workspace-scoped task actions without replicating workspace membership tables.

## Database

- Database: PostgreSQL `mobflow_workspaces`
- Migration strategy: service-owned relational schema

### Main Entities

#### `workspace`

| Field | Purpose |
| --- | --- |
| `id` | Workspace UUID. |
| `name` | Workspace name shown in list and detail screens. |
| `description` | Workspace context shown to members. |
| `joinCode` | Public 8-character code used in self-service join flows. |

#### `workspace_member`

| Field | Purpose |
| --- | --- |
| `workspaceId` | References the workspace. |
| `authId` | Member identity shared across the platform. |
| `role` | `OWNER`, `ADMIN`, or `MEMBER`. |

#### `workspace_invite`

| Field | Purpose |
| --- | --- |
| `workspaceId` | Target workspace for the invitation. |
| `inviteeAuthId` | Invited user identity. |
| `token` | Token used to correlate the invite in notification metadata. |
| `status` | `PENDING`, `ACCEPTED`, `DECLINED`, or `EXPIRED`. |
| `expiresAt` | Invite expiration timestamp, seven days after creation. |

## Kafka Integration

`workspace-service` is a producer on `workspace.events`.

| Topic | Event Type | Trigger |
| --- | --- | --- |
| `workspace.events` | `WORKSPACE_INVITE` | A new invite is created for a user. |
| `workspace.events` | `WORKSPACE_MEMBER_ADDED` | A user joins through invite acceptance or another member-add flow. |
| `workspace.events` | `WORKSPACE_MEMBER_REMOVED` | A member is removed or leaves the workspace. |
| `workspace.events` | `WORKSPACE_ROLE_CHANGED` | A member role is updated. |

The service publishes self-contained event payloads so `notification-service` can create notifications without making enrichment calls back to the workspace domain.

## Run in Isolation

```bash
docker compose up --build workspace-service
```

The service requires PostgreSQL, Kafka, and the dependent `user-service` for internal user resolution.
