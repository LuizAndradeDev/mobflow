# notification-service

## Responsibility in the Architecture

`notification-service` is the event-consumption and delivery edge of the platform. It turns domain events produced by the other services into persistent notification records and, when the event channel requires it, email deliveries rendered with Thymeleaf templates and sent through `JavaMailSender`. It deliberately does not own upstream business state and does not call upstream services for enrichment. Instead, it relies on self-contained Kafka payloads so notification creation stays decoupled from the availability of the producing services.

## Main Capabilities and Business Rules

The service is responsible for durable notification history and user-facing notification state.

- Consumes `task.events`, `workspace.events`, and `auth.events`.
- Persists in-app notifications to MongoDB.
- Exposes notification lists and unread counters for the frontend.
- Marks individual notifications as read.
- Marks all notifications as read in a single operation.
- Sends emails using JavaMailSender and Thymeleaf when the event channel requires email delivery.

Operational rules:

- Event payloads are self-contained; no enrichment callback is performed after consumption.
- Malformed Kafka messages are logged and discarded instead of being retried through exception propagation.
- Read state is tracked with both a boolean flag and a `readAt` timestamp.
- Notification priority and channel are explicit fields rather than implicit conventions.

## Service Stack

| Area | Technologies |
| --- | --- |
| Runtime | Java 21, Spring Boot 3.5.x |
| Security | Spring Security, JJWT 0.11.5 |
| Persistence | Spring Data MongoDB, MongoDB |
| Messaging | Spring Kafka |
| Email | JavaMailSender, Thymeleaf |
| Validation | Jakarta Validation |
| Build | Maven Wrapper |

## Internal Package Structure

| Package | Purpose |
| --- | --- |
| `config` | Security, Kafka, mail, and application configuration. |
| `controller` | Public notification APIs for list, unread count, and read operations. |
| `service` | Notification persistence, read-state transitions, and email dispatch orchestration. |
| `repository` | MongoDB access for notifications. |
| `model/entities` | MongoDB notification document model. |
| `dto/response` | Notification list, unread count, and error response contracts. |
| `model/enums` | Notification type, channel, and priority enums. |
| `security` | JWT validation and request authentication. |
| `kafka` | Consumers for `task.events`, `workspace.events`, and `auth.events`. |
| `exception` | Error handling for invalid notification access and processing failures. |

## Endpoints

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/api/notifications` | Bearer JWT | Returns notifications for the authenticated user. |
| `GET` | `/api/notifications/unread-count` | Bearer JWT | Returns the unread notification count for the authenticated user. |
| `PATCH` | `/api/notifications/{id}/read` | Bearer JWT | Marks a specific notification as read. |
| `PATCH` | `/api/notifications/read-all` | Bearer JWT | Marks all notifications for the authenticated user as read. |

## Request and Response Examples

### List Notifications

```http
GET /api/notifications
Authorization: Bearer <jwt>
```

Example response:

```json
[
  {
    "id": "661e90a4d0f8c84e9cd3ab11",
    "recipientId": "7f45fb3d-0f7a-4c65-9ad2-df4e8da40cb7",
    "title": "Workspace invitation",
    "body": "Luiz Andrade invited you to join Mobflow Platform.",
    "type": "WORKSPACE_INVITE",
    "channel": "IN_APP",
    "priority": "HIGH",
    "read": false,
    "metadata": {
      "workspaceId": "5e74e8d2-cf5c-4f80-99e1-9468aa347fd3",
      "inviteToken": "f0ec0886-beb6-4a90-949a-76e7d50ff56b"
    },
    "sentAt": "2026-04-16T13:10:00Z",
    "readAt": null
  }
]
```

### Unread Count

```http
GET /api/notifications/unread-count
Authorization: Bearer <jwt>
```

Example response:

```json
{
  "count": 4
}
```

### Mark One Notification as Read

```http
PATCH /api/notifications/661e90a4d0f8c84e9cd3ab11/read
Authorization: Bearer <jwt>
```

Example response:

```json
{
  "id": "661e90a4d0f8c84e9cd3ab11",
  "recipientId": "7f45fb3d-0f7a-4c65-9ad2-df4e8da40cb7",
  "title": "Workspace invitation",
  "body": "Luiz Andrade invited you to join Mobflow Platform.",
  "type": "WORKSPACE_INVITE",
  "channel": "IN_APP",
  "priority": "HIGH",
  "read": true,
  "metadata": {
    "workspaceId": "5e74e8d2-cf5c-4f80-99e1-9468aa347fd3",
    "inviteToken": "f0ec0886-beb6-4a90-949a-76e7d50ff56b"
  },
  "sentAt": "2026-04-16T13:10:00Z",
  "readAt": "2026-04-16T13:16:22Z"
}
```

## Security

All HTTP endpoints require a valid Bearer token. The service validates the shared JWT, extracts `authId`, and uses that value as `recipientId` when listing and mutating notifications. This prevents one user from reading or updating another user's documents even though the service itself does not manage account data.

There are no `/internal/**` endpoints in `notification-service`. Its integration model is event-driven rather than synchronous service-to-service querying.

## Database

- Database: MongoDB `notifications`
- Collection: `notifications`
- Persistence model: document-oriented, optimized for append-heavy notification creation and recipient-scoped reads

### Main Document Fields

| Field | Purpose |
| --- | --- |
| `recipientId` | User identity that owns the notification. |
| `recipientEmail` | Email destination when delivery includes email. |
| `title` | Notification headline rendered in the UI or email. |
| `body` | Human-readable message body. |
| `type` | Domain event classification. |
| `channel` | `IN_APP` or `EMAIL`. |
| `priority` | `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL`. |
| `read` | Boolean state used by unread counters. |
| `metadata` | `Map<String, String>` carrying correlation data such as workspace IDs or invite tokens. |
| `readAt` | Timestamp for read acknowledgment. |
| `sentAt` | Timestamp for notification creation or delivery. |

## Kafka Integration

`notification-service` is a consumer for all platform event topics.

| Topic | Event Types Consumed | Result |
| --- | --- | --- |
| `task.events` | `TASK_CREATED`, `TASK_ASSIGNED`, `TASK_UPDATED`, `TASK_DELETED`, `TASK_COMPLETED`, `TASK_DUE_SOON` | Creates in-app notifications and email notifications where appropriate. |
| `workspace.events` | `WORKSPACE_INVITE`, `WORKSPACE_MEMBER_ADDED`, `WORKSPACE_MEMBER_REMOVED`, `WORKSPACE_ROLE_CHANGED` | Creates workspace-related notification documents and invite notifications. |
| `auth.events` | `EMAIL_CONFIRMATION` | Sends account confirmation email and persists supporting notification state. |

Consumer behavior:

- Uses `StringDeserializer`.
- Deserializes event payloads manually.
- Wraps processing in `try/catch`.
- Logs malformed payloads and discards them without rethrowing.

## Run in Isolation

```bash
docker compose up --build notification-service
```

The service requires MongoDB, Kafka, and SMTP configuration to execute the full event-consumption and email-delivery pipeline.
