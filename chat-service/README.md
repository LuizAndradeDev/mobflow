# chat-service

## Responsibility in the Architecture

`chat-service` owns private conversations, realtime messaging delivery, unread counters, and read receipts. It stores conversations and messages in MongoDB, exposes paginated REST endpoints for history/bootstrap, and uses STOMP over WebSocket so the Angular client can receive live updates without polling.

## Main Capabilities and Business Rules

- Creates or reuses private conversations between two authenticated users.
- Lists the current user's conversations with counterpart metadata and unread counts.
- Returns paginated message history per conversation.
- Marks conversations as read and propagates read receipts.
- Accepts inbound STOMP messages and distributes conversation/user queue events in real time.
- Publishes chat notification events consumed by `notification-service`.

Business rules enforced by the service:

- Private conversations are only created when `social-service` confirms an existing friendship.
- Conversation access is restricted to participants.
- Read receipts and unread counters are updated as part of the conversation lifecycle.
- Realtime delivery and REST history remain consistent over the same MongoDB persistence model.

## Service Stack

| Area | Technologies |
| --- | --- |
| Runtime | Java 21, Spring Boot 3.5.x |
| Security | Spring Security, JJWT 0.11.5 |
| Persistence | Spring Data MongoDB, MongoDB |
| Messaging | Spring Kafka, Spring WebSocket |
| Validation | Jakarta Validation |
| Build | Maven Wrapper |

## HTTP Endpoints

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/chat/api/conversations/private` | Bearer JWT | Creates or reuses a private conversation with another user. |
| `GET` | `/chat/api/conversations` | Bearer JWT | Lists conversations for the current user. |
| `GET` | `/chat/api/conversations/{conversationId}` | Bearer JWT | Returns a single conversation if the current user is a participant. |
| `GET` | `/chat/api/conversations/{conversationId}/messages` | Bearer JWT | Returns paginated message history for a conversation. |
| `POST` | `/chat/api/conversations/{conversationId}/read` | Bearer JWT | Marks unread messages as read and emits a read receipt. |

## WebSocket Contract

| Type | Destination | Purpose |
| --- | --- | --- |
| STOMP endpoint | `/chat/ws/chat` | WebSocket handshake endpoint exposed by the service. |
| Inbound app destination | `/app/chat.send` | Sends a message to an existing conversation. |
| User queue delivery | `/user/queue/chat` | Per-user updates such as new conversation state and message events. |
| Topic delivery | `/topic/conversations/{conversationId}` | Realtime events scoped to a single conversation. |

## Integrations

- `social-service`: validates friendship status before a private conversation is created or a message is sent.
- `notification-service`: consumes `social.events` and turns `CHAT_MESSAGE_RECEIVED` into inbox notifications.
- `web-app`: uses REST for bootstrap/history and STOMP for live updates.

## Kafka Topic

| Topic | Event Type | Purpose |
| --- | --- | --- |
| `social.events` | `CHAT_MESSAGE_RECEIVED` | Creates in-app notifications for newly received messages. |

## Run Locally

### Through Docker Compose

```bash
docker compose up --build chat-service
```

### Directly with the Maven Wrapper

Required environment variables include `JWT_SECRET`, `INTERNAL_SECRET`, `KAFKA_BOOTSTRAP_SERVERS`, `MONGO_USER`, `MONGO_PASSWORD`, and `SOCIAL_SERVICE_URL`.

```bash
cd chat-service
./mvnw spring-boot:run
```
