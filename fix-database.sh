#!/bin/bash

echo "🔧 Fixing database - removing cleanup columns..."

# H2 database connection details
DB_PATH="./data/fsmonitor"
DB_USER="sa"
DB_PASSWORD="password"

# Download H2 jar if not present (needed for command line access)
if [ ! -f "h2.jar" ]; then
    echo "📥 Downloading H2 database jar..."
    wget -q https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar -O h2.jar
fi

# Create temporary SQL file
SQL_FILE=$(mktemp)
cat > "$SQL_FILE" << 'EOF'
ALTER TABLE integrations DROP COLUMN IF EXISTS cleanup_enabled;
ALTER TABLE integrations DROP COLUMN IF EXISTS cleanup_age_value;
ALTER TABLE integrations DROP COLUMN IF EXISTS cleanup_age_unit;
EOF

# Run SQL commands
echo "🗃️  Executing SQL commands..."
java -cp h2.jar org.h2.tools.RunScript \
    -url "jdbc:h2:file:${DB_PATH}" \
    -user "${DB_USER}" \
    -password "${DB_PASSWORD}" \
    -script "$SQL_FILE"

RESULT=$?

# Cleanup
rm -f "$SQL_FILE"

if [ $RESULT -eq 0 ]; then
    echo "✅ Database fixed successfully!"
    echo "🔄 You can now restart the application"
else
    echo "❌ Failed to fix database"
    exit 1
fi
