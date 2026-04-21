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

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
checkstyle_config="$root_dir/config/checkstyle/checkstyle.xml"

for service in "${services[@]}"; do
  printf '\n==> Running Checkstyle for %s\n' "$service"
  (
    cd "$root_dir/$service"
    ./mvnw --batch-mode --no-transfer-progress \
      org.apache.maven.plugins:maven-checkstyle-plugin:3.6.0:check \
      -Dcheckstyle.config.location="$checkstyle_config" \
      -Dcheckstyle.consoleOutput=true
  )
done

printf '\nBackend quality checks completed successfully.\n'
