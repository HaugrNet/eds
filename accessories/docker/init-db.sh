#!/bin/bash
# ==============================================================================
# PostgreSQL Initialization Script for EDS
# ------------------------------------------------------------------------------
# This script is executed by the PostgreSQL Docker container during first start.
# It creates the EDS database, user, and schema.
# ==============================================================================

set -e

SQL_DIR="/docker-entrypoint-initdb.d/sql"

echo "=== EDS Database Initialization ==="

# Step 1: Create the eds_user and eds database
echo "Creating EDS user and database..."
psql -v ON_ERROR_STOP=0 -U postgres -d postgres -f "${SQL_DIR}/02-init.sql"

# Step 2: Create the schema (tables, indexes, initial data)
echo "Creating EDS schema..."
psql -v ON_ERROR_STOP=1 -U eds_user -d eds -f "${SQL_DIR}/03-create.sql"

# Step 3: Apply updates
echo "Applying update 1 (EDS 1.0 -> 1.1)..."
psql -v ON_ERROR_STOP=0 -U eds_user -d eds -f "${SQL_DIR}/04-update-1.sql"

echo "Applying update 2 (EDS 1.1 -> 1.2)..."
psql -v ON_ERROR_STOP=0 -U eds_user -d eds -f "${SQL_DIR}/04-update-2.sql"

echo "Applying update 3 (EDS 1.2 -> 2.0)..."
psql -v ON_ERROR_STOP=0 -U eds_user -d eds -f "${SQL_DIR}/04-update-3.sql"

echo "=== EDS Database Initialization Complete ==="
