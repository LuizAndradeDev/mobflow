# API Gateway

The Mobflow API Gateway is a centralized entry point for all external requests to the microservices platform. Built with Spring Cloud Gateway, it provides request routing, authentication, rate limiting, observability, and cross-cutting concerns without containing any business logic.

## Responsibilities

- **Route Management**: Routes incoming requests to appropriate backend services based on URL patterns
- **JWT Authentication**: Validates OAuth2 JWT tokens for all protected endpoints
- **Rate Limiting**: Enforces user/IP-based request limits to protect backend services
- **Correlation ID**: Generates and propagates correlation IDs for request tracing
- **Header Propagation**: Extracts and forwards user context (user ID, roles) to downstream services
- **CORS**: Handles cross-origin requests from the Angular frontend
- **Observability**: Exposes Prometheus metrics for monitoring

## Architecture

```
Client (Browser)
    → Nginx (port 80)
    → API Gateway (port 8080)
    → Backend Microservices
        → auth-service (8080)
        → user-service (8081)
        → workspace-service (8082)
        → task-service (8083)
        → notification-service (8084)
        → social-service (8085)
        → chat-service (8086)
    → Kafka (event-driven)
```

## Routes

| Path Pattern | Service | Port |
|-------------|---------|------|
| `/api/auth/**` | auth-service | 8080 |
| `/api/users/**` | user-service | 8081 |
| `/api/workspaces/**` | workspace-service | 8082 |
| `/api/tasks/**` | task-service | 8083 |
| `/api/notifications/**` | notification-service | 8084 |
| `/api/comments/**` | social-service | 8085 |
| `/api/chat/**` | chat-service | 8086 |

## Security

### Public Routes
The following routes are publicly accessible without authentication:
- `/api/auth/login`
- `/api/auth/signup`
- `/api/auth/confirm-email`
- `/actuator/health`

### Protected Routes
All other `/api/**` routes require a valid JWT token.

### Header Propagation
The gateway extracts Claims from JWT and propagates these headers:
- `Authorization`: Bearer token
- `X-Correlation-Id`: Request correlation ID
- `X-User-Id`: User identifier from JWT
- `X-User-Roles`: User roles from JWT

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|------------|---------|
| `JWT_SECRET` | Base64-encoded JWT signing key | (from .env) |
| `APP_BASE_URL` | Application base URL | http://localhost |
| `APP_CORS_ALLOWED_ORIGINS` | CORS allowed origins | http://localhost |
| `rate-limit.requests-per-minute` | Rate limit | 100 |

### application.yml

Key configuration sections:

- `spring.cloud.gateway.routes`: Define service routing
- `gateway.jwt`: JWT validation configuration
- `rate-limit`: Rate limiting settings
- `management.endpoints.web.exposure.include`: Exposes `/actuator/prometheus`

## Observability

### Prometheus Metrics

The gateway exposes metrics at `/actuator/prometheus`:

```bash
curl http://localhost:8080/actuator/prometheus
```

Key metrics:
- `spring_cloud_gateway_requests_seconds_count`: Total request count
- `spring_cloud_gateway_requests_seconds_sum`: Total request time
- `spring_cloud_gateway_route_seconds`: Route-specific metrics

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

## Running

### Local Development

```bash
cd api-gateway
./mvnw spring-boot:run
```

### Docker

```bash
docker build -t mobflow/api-gateway ./api-gateway
docker run -p 8080:8080 mobflow/api-gateway
```

### Full Platform

```bash
docker-compose up -d
```

## Testing

```bash
./mvnw test
```

## Key Technical Decisions

1. **Spring Cloud Gateway**: Chosen for reactive, non-blocking architecture ideal for microservices
2. **OAuth2 Resource Server**: Leverages Spring Security for JWT validation
3. **In-Memory Rate Limiting**: Simple implementation suitable for development; production should use Redis
4. **No Business Logic**: Gateway only handles cross-cutting concerns
5. **Defense in Depth**: Backend services continue enforcing their own security

## Future Improvements

- Redis-based rate limiting for distributed deployments
- Circuit breaker pattern (Resilience4j)
- Request/response transformation
- API key management
- Bot detection
- DDOS protection