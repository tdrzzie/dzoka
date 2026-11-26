#!/bin/sh
set -eu
# Fresh database only. These files are private; never print values or pass them as process arguments.
export DZOKA_INIT_APP_PASSWORD="$(cat /run/db-secrets/app)"
export DZOKA_INIT_MIGRATION_PASSWORD="$(cat /run/db-secrets/migrator)"
test -n "$DZOKA_INIT_APP_PASSWORD"
test -n "$DZOKA_INIT_MIGRATION_PASSWORD"
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<'SQL'
\getenv app_password DZOKA_INIT_APP_PASSWORD
\getenv migration_password DZOKA_INIT_MIGRATION_PASSWORD
CREATE ROLE dzoka_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS PASSWORD :'app_password';
CREATE ROLE dzoka_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS PASSWORD :'migration_password';
CREATE DATABASE dzoka OWNER dzoka_migrator;
SQL
unset DZOKA_INIT_APP_PASSWORD DZOKA_INIT_MIGRATION_PASSWORD
