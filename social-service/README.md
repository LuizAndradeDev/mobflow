# social-service

## Responsibility in the Architecture

`social-service` owns the collaboration features that sit adjacent to core task execution: the friend graph between users, the friend-request lifecycle, task comments, and `@username` mention extraction. It persists that social data in MongoDB and publishes domain events so `notification-service` can create inbox items without coupling the frontend or the task domain directly to notification rules.

## Main Capabilities and Business Rules

- Sends, lists, accepts, and declines friend requests between authenticated users.
- Resolves the current user friend list for the social/chat sidebar experience.
- Creates, lists, updates, and deletes task comments.
- Extracts mentions from comment text and resolves mentioned users through internal service calls.
- Publishes notification-ready Kafka events for comments, mentions, and friend requests.
- Exposes an internal friendship-validation endpoint consumed by `chat-service` before a private conversation is created.

Business rules enforced by the service:

- A friend request cannot be accepted or declined by an unrelated user.
- Chat eligibility depends on an existing friendship validated through `/internal/social/**`.
- Comment operations are scoped to authenticated users and validated against task/workspace membership.
- Notification payloads are self-contained before they leave the service.

## Service Stack

| Area | Technologies |
| --- | --- |
| Runtime | Java 21, Spring Boot 3.5.x |
| Security | Spring Security, JJWT 0.11.5 |
| Persistence | Spring Data MongoDB, MongoDB |
| Messaging | Spring Kafka |
| Validation | Jakarta Validation |
| Build | Maven Wrapper |

## Endpoints

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/friends/request` | Bearer JWT | Sends a friend request to another user by username. |
| `GET` | `/api/friends/requests` | Bearer JWT | Lists incoming and outgoing friend requests for the current user. |
| `POST` | `/api/friends/{requestId}/accept` | Bearer JWT | Accepts a pending friend request. |
| `POST` | `/api/friends/{requestId}/decline` | Bearer JWT | Declines a pending friend request. |
| `GET` | `/api/friends` | Bearer JWT | Lists accepted friends for the current user. |
| `POST` | `/api/tasks/{taskId}/comments` | Bearer JWT | Creates a comment on a task. |
| `GET` | `/api/tasks/{taskId}/comments` | Bearer JWT | Lists paginated comments for a task. |
| `PUT` | `/api/comments/{commentId}` | Bearer JWT | Updates an existing comment authored by the current user. |
| `DELETE` | `/api/comments/{commentId}` | Bearer JWT | Deletes an existing comment authored by the current user. |
| `GET` | `/internal/social/friendships/{authId}/friends/{targetAuthId}` | `X-Internal-Secret` | Validates whether two users are already friends. |

## Integrations

- `task-service`: validates task context before comments are created or listed.
- `user-service`: resolves display data for mentions and social views.
- `workspace-service`: validates membership and workspace-level permissions when needed.
- `notification-service`: consumes `social-comment-events` and `social-friendship-events`.
- `chat-service`: calls the internal friendship-validation endpoint.

## Kafka Topics

| Topic | Event Types | Purpose |
| --- | --- | --- |
| `social-comment-events` | `COMMENT_CREATED`, `USER_MENTIONED` | Drives task-comment and mention notifications. |
| `social-friendship-events` | `FRIEND_REQUEST_SENT` | Drives friend request notifications. |

## Run Locally

### Through Docker Compose

```bash
docker compose up --build social-service
```

### Directly with the Maven Wrapper

Required environment variables include `JWT_SECRET`, `INTERNAL_SECRET`, `KAFKA_BOOTSTRAP_SERVERS`, `MONGO_USER`, `MONGO_PASSWORD`, `AUTH_SERVICE_URL`, `TASK_SERVICE_URL`, `USER_SERVICE_URL`, and `WORKSPACE_SERVICE_URL`.

```bash
cd social-service
./mvnw spring-boot:run
```
