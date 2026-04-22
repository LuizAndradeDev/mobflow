#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

images=(
  "api-gateway:api-gateway/Dockerfile:api-gateway"
  "auth-service:auth-service/Dockerfile:auth-service"
  "user-service:user-service/Dockerfile:user-service"
  "workspace-service:workspace-service/Dockerfile:workspace-service"
  "task-service:task-service/Dockerfile:task-service"
  "notification-service:notification-service/Dockerfile:notification-service"
  "social-service:social-service/Dockerfile:social-service"
  "chat-service:chat-service/Dockerfile:chat-service"
  "web-app:web-app/Dockerfile:web-app"
  "edge-nginx:Dockerfile.nginx:."
)

for image in "${images[@]}"; do
  IFS=":" read -r name dockerfile context <<< "$image"
  printf '\n==> Building Docker image mobflow/%s:ci\n' "$name"
  docker build \
    --file "$root_dir/$dockerfile" \
    --tag "mobflow/$name:ci" \
    "$root_dir/$context"
done

printf '\nDocker image build validation completed successfully.\n'
