#!/usr/bin/env bash
set -euo pipefail

services=(
  "auth-service"
  "user-service"
  "workspace-service"
  "task-service"
  "notification-service"
  "social-service"
  "chat-service"
)

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

for service in "${services[@]}"; do
  printf '\n==> Running tests for %s\n' "$service"
  (cd "$root_dir/$service" && mvn test)
done

printf '\nBackend test suite completed successfully.\n'
