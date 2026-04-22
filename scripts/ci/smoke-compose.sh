#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
project_name="${COMPOSE_PROJECT_NAME:-mobflow-ci}"
env_file="$(mktemp)"
override_file="$(mktemp)"
compose_env_file="$root_dir/.env"
compose_env_backup=""
failed=0

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  cat <<'EOF'
Usage: scripts/ci/smoke-compose.sh

Builds the full Docker Compose stack, waits for infrastructure healthchecks,
then verifies the edge Nginx and backend Actuator health endpoints.
EOF
  rm -f "$env_file"
  rm -f "$override_file"
  exit 0
fi

cleanup() {
  local exit_code=$?
  if [[ "$failed" == "1" || "$exit_code" != "0" ]]; then
    compose logs --tail=200 || true
  fi
  compose down -v --remove-orphans || true
  if [[ -n "$compose_env_backup" && -f "$compose_env_backup" ]]; then
    mv "$compose_env_backup" "$compose_env_file"
  else
    rm -f "$compose_env_file"
  fi
  rm -f "$env_file" "$override_file"
}
trap cleanup EXIT

cat > "$env_file" <<'EOF'
DB_HOST=postgres
DB_PORT=5432
DB_USER=mobflow
DB_PASSWORD=mobflow_secret
AUTH_DB=mobflow_auth
USER_DB=mobflow_user
WORKSPACE_DB=mobflow_workspace
TASK_DB=mobflow_task
JWT_SECRET=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=
JWT_EXPIRATION=86400000
INTERNAL_SECRET=ci_internal_secret_0123456789abcdef
INTERNAL_HTTP_CONNECT_TIMEOUT=500ms
INTERNAL_HTTP_READ_TIMEOUT=2s
REDIS_HOST=redis
REDIS_PORT=6379
MINIO_ENDPOINT=http://minio:9000
MINIO_PUBLIC_URL=http://localhost:9000
MINIO_ROOT_USER=minio
MINIO_ROOT_PASSWORD=minio123
MINIO_BUCKET=mobflow-avatars
MONGO_HOST=mongodb
MONGO_PORT=27017
MONGO_USER=mobflow-mongo
MONGO_PASSWORD=mongo-secret
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
AUTH_EVENTS_TOPIC=auth-events
TASK_EVENTS_TOPIC=task-events
WORKSPACE_EVENTS_TOPIC=workspace-events
SOCIAL_COMMENT_EVENTS_TOPIC=social-comment-events
SOCIAL_FRIENDSHIP_EVENTS_TOPIC=social-friendship-events
SOCIAL_EVENTS_TOPIC=social.events
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=admin
AUTH_SERVICE_URL=http://auth-service:8080
WORKSPACE_SERVICE_URL=http://workspace-service:8082
USER_SERVICE_URL=http://user-service:8081
TASK_SERVICE_URL=http://task-service:8083
SOCIAL_SERVICE_URL=http://social-service:8085
MAIL_HOST=mailhog
MAIL_PORT=1025
MAIL_USERNAME=no-reply@mobflow.dev
MAIL_PASSWORD=
MAIL_SMTP_AUTH=false
MAIL_SMTP_STARTTLS=false
APP_BASE_URL=http://localhost
APP_CORS_ALLOWED_ORIGINS=http://localhost
APP_MAIL_MAX_ATTEMPTS=3
TASK_DUE_SOON_CRON='0 0 8 * * *'
EOF

if [[ -f "$compose_env_file" ]]; then
  compose_env_backup="$(mktemp)"
  cp "$compose_env_file" "$compose_env_backup"
fi
cp "$env_file" "$compose_env_file"

cat > "$override_file" <<EOF
services:
  mongodb:
    ports: !override []
  kafka:
    ports: !override []
  mailhog:
    ports: !override []
  postgres:
    ports: !override []
  redis:
    ports: !override []
  minio:
    ports: !override []
  auth-service:
    env_file: !override
      - "$env_file"
    ports: !override []
  user-service:
    env_file: !override
      - "$env_file"
    ports: !override []
  workspace-service:
    env_file: !override
      - "$env_file"
    ports: !override []
  task-service:
    env_file: !override
      - "$env_file"
    ports: !override []
  notification-service:
    env_file: !override
      - "$env_file"
    ports: !override []
  social-service:
    env_file: !override
      - "$env_file"
    ports: !override []
  chat-service:
    env_file: !override
      - "$env_file"
    ports: !override []
  prometheus:
    ports: !override []
  grafana:
    ports: !override []
  nginx:
    ports: !override []
EOF

compose() {
  docker compose --project-name "$project_name" --env-file "$env_file" -f docker-compose.yaml -f "$override_file" "$@"
}

wait_for_healthcheck() {
  local service="$1"
  local timeout_seconds="${2:-180}"
  local started_at
  started_at="$(date +%s)"

  printf 'Waiting for %s healthcheck' "$service"
  while true; do
    local container_id
    container_id="$(compose ps -q "$service" 2>/dev/null || true)"
    if [[ -n "$container_id" ]]; then
      local status
      status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id" 2>/dev/null || true)"
      if [[ "$status" == "healthy" || "$status" == "running" ]]; then
        printf ' ok\n'
        return 0
      fi
    fi

    if (( "$(date +%s)" - started_at > timeout_seconds )); then
      printf ' failed\n'
      failed=1
      echo "Timed out waiting for $service to become healthy"
      return 1
    fi
    printf '.'
    sleep 5
  done
}

wait_for_url() {
  local name="$1"
  local url="$2"
  local expected="$3"
  local timeout_seconds="${4:-240}"
  local started_at
  started_at="$(date +%s)"

  printf 'Waiting for %s at %s' "$name" "$url"
  while true; do
    local response
    response="$(compose exec -T nginx wget -qO- "$url" 2>/dev/null || true)"
    if [[ "$response" == *"$expected"* ]]; then
      printf ' ok\n'
      return 0
    fi

    if (( "$(date +%s)" - started_at > timeout_seconds )); then
      printf ' failed\n'
      failed=1
      echo "Timed out waiting for $name"
      return 1
    fi
    printf '.'
    sleep 5
  done
}

cd "$root_dir"

compose config --quiet
compose build
compose up -d

wait_for_healthcheck postgres 180
wait_for_healthcheck mongodb 180
wait_for_healthcheck redis 120
wait_for_healthcheck minio 180
wait_for_healthcheck kafka 240

wait_for_url "auth-service" "http://auth-service:8080/actuator/health" "UP"
wait_for_url "api-gateway" "http://api-gateway:8080/actuator/health" "UP"
wait_for_url "user-service" "http://user-service:8081/actuator/health" "UP"
wait_for_url "workspace-service" "http://workspace-service:8082/actuator/health" "UP"
wait_for_url "task-service" "http://task-service:8083/tasks/actuator/health" "UP"
wait_for_url "notification-service" "http://notification-service:8084/actuator/health" "UP"
wait_for_url "social-service" "http://social-service:8085/social/actuator/health" "UP"
wait_for_url "chat-service" "http://chat-service:8086/chat/actuator/health" "UP"
wait_for_url "nginx edge" "http://localhost/health" "OK"
wait_for_url "prometheus" "http://prometheus:9090/-/healthy" "Prometheus"
wait_for_url "grafana" "http://grafana:3000/api/health" "ok"

compose ps
printf '\nDocker Compose smoke test completed successfully.\n'
