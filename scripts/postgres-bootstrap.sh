#!/usr/bin/env bash
set -euo pipefail

readonly db_host="${DB_HOST:-postgres}"
readonly db_port="${DB_PORT:-5432}"
readonly db_user="${DB_USER:?DB_USER is required}"
readonly db_password="${DB_PASSWORD:?DB_PASSWORD is required}"

readonly databases=(
  "${AUTH_DB:?AUTH_DB is required}"
  "${USER_DB:?USER_DB is required}"
  "${WORKSPACE_DB:?WORKSPACE_DB is required}"
  "${TASK_DB:?TASK_DB is required}"
)

export PGPASSWORD="${db_password}"

for database in "${databases[@]}"; do
  echo "Ensuring database '${database}' exists..."

  existing_database="$(
    psql \
      --host "${db_host}" \
      --port "${db_port}" \
      --username "${db_user}" \
      --dbname postgres \
      --tuples-only \
      --no-align \
      --command "SELECT datname FROM pg_database WHERE datname = '${database}'"
  )"

  if [[ "${existing_database}" == "${database}" ]]; then
    echo "Database '${database}' already exists."
    continue
  fi

  psql \
    --host "${db_host}" \
    --port "${db_port}" \
    --username "${db_user}" \
    --dbname postgres \
    --command "CREATE DATABASE \"${database}\" ENCODING 'UTF8' TEMPLATE template0"

  echo "Database '${database}' created."
done
