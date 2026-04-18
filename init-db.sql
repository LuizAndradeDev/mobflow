-- Databases required by the relational Mobflow services.
SELECT 'CREATE DATABASE mobflow_auth'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'mobflow_auth'
)\gexec

SELECT 'CREATE DATABASE mobflow_user'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'mobflow_user'
)\gexec

SELECT 'CREATE DATABASE mobflow_workspace'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'mobflow_workspace'
)\gexec

SELECT 'CREATE DATABASE mobflow_task'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'mobflow_task'
)\gexec
