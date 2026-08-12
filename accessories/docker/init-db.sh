#!/bin/bash
# ==============================================================================
# PostgreSQL Initialization Script for EDS
# ------------------------------------------------------------------------------
# This script is executed by the PostgreSQL container during first start.
# It creates the EDS database, user, and schema.
# ==============================================================================

set -e

SQL_DIR="/docker-entrypoint-initdb.d/sql"

# Superuser the postgres image initialized with (defaults to 'postgres' if the
# user did not override POSTGRES_USER in .env).
SUPERUSER="${POSTGRES_USER:-postgres}"
# Role the EDS application will connect as. Defaults to 'eds_user' for
# backward compatibility with existing deployments.
EDS_USER="${EDS_DB_USERNAME:-eds_user}"
# Database the EDS application schema is created in. Defaults to 'eds'.
EDS_DB="${EDS_DB_NAME:-eds}"

echo "=== EDS Database Initialization ==="

if [ -z "${EDS_DB_PASSWORD:-}" ]; then
    echo "ERROR: EDS_DB_PASSWORD must be set in the environment." >&2
    exit 1
fi

# Step 1: Create the EDS role and database. Role name, password and database
# name are passed in via psql variables so none are hardcoded in 02-init.sql.
# The script is idempotent (it skips the role and the database when they already
# exist), so ON_ERROR_STOP=1 is used to surface real failures instead of letting
# a broken bootstrap fall through to the schema step.
echo "Creating EDS user and database..."
psql -v ON_ERROR_STOP=1 \
     -v eds_user="${EDS_USER}" \
     -v eds_password="${EDS_DB_PASSWORD}" \
     -v eds_db="${EDS_DB}" \
     -U "${SUPERUSER}" -d postgres -f "${SQL_DIR}/02-init.sql"

# Step 2: Create the schema (tables, indexes, initial data)
echo "Creating EDS schema..."
psql -v ON_ERROR_STOP=1 -U "${EDS_USER}" -d "${EDS_DB}" -f "${SQL_DIR}/03-create.sql"

# Step 3: Apply updates
echo "Applying update 1 (EDS 1.0 -> 1.1)..."
psql -v ON_ERROR_STOP=0 -U "${EDS_USER}" -d "${EDS_DB}" -f "${SQL_DIR}/04-update-1.sql"

echo "Applying update 2 (EDS 1.1 -> 1.2)..."
psql -v ON_ERROR_STOP=0 -U "${EDS_USER}" -d "${EDS_DB}" -f "${SQL_DIR}/04-update-2.sql"

echo "Applying update 3 (EDS 1.2 -> 2.0)..."
psql -v ON_ERROR_STOP=0 -U "${EDS_USER}" -d "${EDS_DB}" -f "${SQL_DIR}/04-update-3.sql"

echo "=== EDS Database Initialization Complete ==="
