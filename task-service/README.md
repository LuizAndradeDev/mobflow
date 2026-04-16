# task-service

## Responsibility in the Architecture

`task-service` owns the execution model of work inside a workspace. It manages boards, task lists, tasks, ordering metadata for drag-and-drop interactions, and authenticated analytics derived from task data. It does not persist workspace membership itself; instead, it asks `workspace-service` whether a given `authId` is allowed to operate within a workspace and uses `user-service` only to enrich assignee presentation data. This keeps task state isolated while preserving workspace-driven authorization.

## Main Capabilities and Business Rules

The service provides the workspace execution surface used by the kanban and analytics screens.

- Creates and manages boards scoped to a workspace.
- Creates and reorders task lists within a board by `position`.
- Creates, updates, moves, and deletes tasks.
- Supports task status values `TODO`, `IN_PROGRESS`, and `COMPLETED`.
- Supports task priority values `LOW`, `MEDIUM`, `HIGH`, and `URGENT`.
- Tracks `assigneeAuthId`, `dueDate`, `completedByAuthId`, and `completedAt`.
- Exposes workspace summary endpoints for dashboard-style task previews.
- Exposes authenticated analytics endpoints for user and workspace views.
- Publishes task lifecycle events to Kafka.

Business rules and boundaries:

- All public endpoints are under the context path `/tasks`, so the HTTP surface is `/tasks/api/**`.
- Workspace access is verified through `workspace-service` rather than local membership tables.
- Task ordering is explicit and persisted through `position` fields to support drag-and-drop UX.
- Completion metadata is captured when tasks transition to `COMPLETED`.
- Assignee display names and avatars are resolved from `user-service` through internal batch endpoints.

## Service Stack

| Area | Technologies |
| --- | --- |
| Runtime | Java 21, Spring Boot 3.5.x |
| Security | Spring Security, JJWT 0.11.5 |
| Persistence | Spring Data JPA, PostgreSQL 16, Flyway |
| Messaging | Spring Kafka |
| HTTP Clients | Internal clients for `workspace-service` and `user-service` |
| Validation | Jakarta Validation |
| Build | Maven Wrapper |

## Internal Package Structure

| Package | Purpose |
| --- | --- |
| `config` | Security, CORS, Redis, RestClient, and application configuration. |
| `controller` | Public board, list, task, analytics, and summary APIs plus internal endpoints. |
| `service` | Board lifecycle, task list ordering, task lifecycle, analytics, and workspace summary orchestration. |
| `repository` | JPA access to boards, lists, and tasks. |
| `model/entities` | Persistent board, task list, and task entities. |
| `model/dto/request` | Input contracts for create, update, move, and reorder operations. |
| `model/dto/response` | Board, list, task, analytics, and summary response contracts. |
| `model/enums` | Task status and priority enums. |
| `security` | JWT validation and request authentication. |
| `client` | Internal HTTP clients for workspace membership checks and user batch enrichment. |
| `kafka` | Task event producers. |
| `exception` | Domain-specific validation and authorization exceptions. |

## Endpoints

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/tasks/api/workspaces/{workspaceId}/boards` | Bearer JWT | Lists boards visible within a workspace. |
| `GET` | `/tasks/api/workspaces/{workspaceId}/boards/{boardId}` | Bearer JWT | Returns a board with its lists and tasks. |
| `POST` | `/tasks/api/workspaces/{workspaceId}/boards` | Bearer JWT | Creates a board in a workspace. |
| `PUT` | `/tasks/api/workspaces/{workspaceId}/boards/{boardId}` | Bearer JWT | Updates board metadata such as name and color. |
| `DELETE` | `/tasks/api/workspaces/{workspaceId}/boards/{boardId}` | Bearer JWT | Deletes a board and its scoped structures. |
| `POST` | `/tasks/api/workspaces/{workspaceId}/boards/{boardId}/lists` | Bearer JWT | Creates a task list inside a board. |
| `PUT` | `/tasks/api/workspaces/{workspaceId}/boards/{boardId}/lists/{listId}` | Bearer JWT | Updates a task list. |
| `DELETE` | `/tasks/api/workspaces/{workspaceId}/boards/{boardId}/lists/{listId}` | Bearer JWT | Deletes a task list. |
| `PATCH` | `/tasks/api/workspaces/{workspaceId}/boards/{boardId}/lists/reorder` | Bearer JWT | Persists drag-and-drop ordering for lists. |
| `GET` | `/tasks/api/workspaces/{workspaceId}/lists/{listId}/tasks` | Bearer JWT | Lists tasks in a specific task list. |
| `GET` | `/tasks/api/workspaces/{workspaceId}/tasks/{taskId}` | Bearer JWT | Returns a single task with workspace-scoped authorization. |
| `POST` | `/tasks/api/workspaces/{workspaceId}/lists/{listId}/tasks` | Bearer JWT | Creates a task in a task list. |
| `PUT` | `/tasks/api/workspaces/{workspaceId}/tasks/{taskId}` | Bearer JWT | Updates task content, assignee, due date, priority, or status. |
| `PATCH` | `/tasks/api/workspaces/{workspaceId}/tasks/{taskId}/move` | Bearer JWT | Moves a task across lists or positions. |
| `DELETE` | `/tasks/api/workspaces/{workspaceId}/tasks/{taskId}` | Bearer JWT | Deletes a task. |
| `GET` | `/tasks/api/task-analytics/user/{authId}` | Bearer JWT | Returns global analytics for a user. |
| `GET` | `/tasks/api/task-analytics/workspace/{workspaceId}/user/{authId}` | Bearer JWT | Returns workspace-scoped analytics for a user. |
| `POST` | `/tasks/api/task-analytics/user/{authId}/workspaces` | Bearer JWT | Returns analytics for a user across a provided list of workspaces. |
| `GET` | `/tasks/api/workspace-summaries/{workspaceId}` | Bearer JWT | Returns a summarized workspace task snapshot for dashboard usage. |
| `POST` | `/tasks/api/workspace-summaries/batch` | Bearer JWT | Returns workspace summaries for multiple workspace IDs. |
| `GET` | `/tasks/internal/tasks/summary/{workspaceId}` | `X-Internal-Secret` | Internal summary endpoint used by trusted backend callers. |
| `POST` | `/tasks/internal/tasks/summaries` | `X-Internal-Secret` | Internal batch summary endpoint used by trusted backend callers. |

## Request and Response Examples

### Create a Board

```http
POST /tasks/api/workspaces/5e74e8d2-cf5c-4f80-99e1-9468aa347fd3/boards
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "name": "Sprint Execution",
  "color": "#0f766e"
}
```

Example response:

```json
{
  "id": "e7f1e5fd-d4ef-4b6d-a792-0dfc70c15bd6",
  "workspaceId": "5e74e8d2-cf5c-4f80-99e1-9468aa347fd3",
  "name": "Sprint Execution",
  "color": "#0f766e",
  "position": 0,
  "lists": []
}
```

### Create a Task

```http
POST /tasks/api/workspaces/5e74e8d2-cf5c-4f80-99e1-9468aa347fd3/lists/75e9199f-6648-4f0d-9ed8-42d96ff475c5/tasks
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "title": "Finalize notification retry policy",
  "description": "Validate retry semantics for email delivery failures before release.",
  "priority": "HIGH",
  "assigneeAuthId": "7f45fb3d-0f7a-4c65-9ad2-df4e8da40cb7",
  "dueDate": "2026-04-20T18:00:00Z"
}
```

Example response:

```json
{
  "id": "8f3e9237-5df1-42b7-bfbb-643ff3d3a7f6",
  "workspaceId": "5e74e8d2-cf5c-4f80-99e1-9468aa347fd3",
  "listId": "75e9199f-6648-4f0d-9ed8-42d96ff475c5",
  "title": "Finalize notification retry policy",
  "description": "Validate retry semantics for email delivery failures before release.",
  "priority": "HIGH",
  "status": "TODO",
  "assigneeAuthId": "7f45fb3d-0f7a-4c65-9ad2-df4e8da40cb7",
  "dueDate": "2026-04-20T18:00:00Z",
  "position": 0
}
```

### Move a Task

```http
PATCH /tasks/api/workspaces/5e74e8d2-cf5c-4f80-99e1-9468aa347fd3/tasks/8f3e9237-5df1-42b7-bfbb-643ff3d3a7f6/move
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "targetListId": "b7f1594a-ecf2-4fc7-882c-6bc3363bb7b7",
  "targetPosition": 1
}
```

### User Analytics

```http
GET /tasks/api/task-analytics/user/0c34ed9e-bef9-4c17-9af6-b2c7a117a9b0
Authorization: Bearer <jwt>
```

Example response:

```json
{
  "authId": "0c34ed9e-bef9-4c17-9af6-b2c7a117a9b0",
  "assignedTasks": 12,
  "completedTasks": 9,
  "createdTasks": 17,
  "overdueTasks": 2
}
```

## Security

Every public endpoint requires a Bearer token. The service validates the shared JWT, extracts `authId`, and uses that actor identity in task creation, completion metadata, analytics access checks, and membership validation. It does not use session state and does not delegate JWT validation to another component.

Workspace authorization is intentionally externalized. Before performing workspace-scoped operations, the service calls `workspace-service` on `/internal/workspaces/{workspaceId}/members/{authId}/role` using `X-Internal-Secret`. This preserves a single source of truth for membership while keeping task persistence independent.

The service also exposes `/tasks/internal/**` endpoints protected by the same internal secret mechanism for trusted backend callers. These endpoints are not meant for the browser.

## Database

- Database: PostgreSQL `mobflow_tasks`
- Migration strategy: Flyway

### Main Entities

#### `board`

| Field | Purpose |
| --- | --- |
| `id` | Board UUID. |
| `workspaceId` | Workspace ownership boundary. |
| `name` | Board label rendered by the frontend. |
| `color` | Visual identifier used in board cards and headers. |
| `position` | Explicit ordering within the workspace. |

#### `task_list`

| Field | Purpose |
| --- | --- |
| `id` | Task list UUID. |
| `boardId` | Parent board. |
| `name` | Column name in kanban views. |
| `position` | Explicit ordering within the board. |

#### `task`

| Field | Purpose |
| --- | --- |
| `id` | Task UUID. |
| `workspaceId` | Denormalized workspace boundary for authorization and querying. |
| `listId` | Parent task list. |
| `title` | Primary task label. |
| `description` | Extended task detail. |
| `status` | `TODO`, `IN_PROGRESS`, or `COMPLETED`. |
| `priority` | `LOW`, `MEDIUM`, `HIGH`, or `URGENT`. |
| `assigneeAuthId` | Assigned user identifier. |
| `dueDate` | Deadline used in overdue logic and reminders. |
| `completedByAuthId` | Principal that completed the task. |
| `completedAt` | Completion timestamp. |
| `position` | Explicit ordering within the list. |

### Flyway Notes

- Initial migrations create the board, task list, and task schema.
- Later migrations add completion tracking fields such as `completed_by_auth_id` and `completed_at`.

## Kafka Integration

`task-service` is a producer on `task.events`.

| Topic | Event Type | Trigger |
| --- | --- | --- |
| `task.events` | `TASK_CREATED` | A task is created. |
| `task.events` | `TASK_ASSIGNED` | A task assignee is added or changed. |
| `task.events` | `TASK_UPDATED` | Task content or status changes. |
| `task.events` | `TASK_DELETED` | A task is deleted. |
| `task.events` | `TASK_COMPLETED` | A task transitions to `COMPLETED`. |
| `task.events` | `TASK_DUE_SOON` | A due-soon reminder event is emitted. |

Events are serialized manually as JSON strings and are designed to be self-contained for `notification-service`.

## Run in Isolation

```bash
docker compose up --build task-service
```

The service requires PostgreSQL, Kafka, `workspace-service`, and `user-service` for its full authorization and enrichment flow.
