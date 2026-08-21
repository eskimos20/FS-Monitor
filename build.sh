#!/bin/bash

# FS-Monitor Build Script
# Bygger en körbar JAR-fil och rensar byggfilerna

set -e  # Avsluta om något kommando misslyckas

echo "🔨 Building FS-Monitor JAR file..."

# Bygg frontend först
echo "🎨 Building frontend..."
cd frontend
npm install
rm -rf dist
npm run build
cd ..

# Verifiera att frontend-bygget faktiskt producerade filer
if [ ! -d "frontend/dist" ] || [ -z "$(ls -A frontend/dist 2>/dev/null)" ]; then
    echo "❌ Frontend build failed or produced an empty dist/ folder. Aborting."
    exit 1
fi

if [ ! -d "frontend/dist/assets" ] || [ -z "$(ls -A frontend/dist/assets 2>/dev/null)" ]; then
    echo "❌ frontend/dist/assets is missing or empty. Aborting."
    exit 1
fi

# Kopiera frontend build till backend's static mapp
echo "📋 Copying frontend to backend static resources..."
rm -rf backend/src/main/resources/static
mkdir -p backend/src/main/resources/static
cp -r frontend/dist/* backend/src/main/resources/static/

echo "✅ Frontend copied to backend static resources ($(find backend/src/main/resources/static -type f | wc -l) files)"

# Gå till backend katalogen
cd backend

# Extrahera version från pom.xml
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "📌 Building version: $VERSION"

# Kör Maven build för att skapa JAR
echo "📦 Running Maven build..."
mvn clean package -DskipTests

# Kontrollera att JAR-filen skapades
if [ -f "target/fs-monitor-backend-${VERSION}.jar" ]; then
    echo "✅ JAR file created successfully"
    
    # Kopiera JAR-filen till root katalogen med nytt namn
    echo "📋 Copying JAR to root directory..."
    cp target/fs-monitor-backend-${VERSION}.jar ../FS-Monitor.jar
    
    echo "✅ JAR copied to: FS-Monitor.jar"
else
    echo "❌ Failed to create JAR file"
    exit 1
fi

# Gå tillbaka till root katalogen
cd ..

# Rensa byggfilerna
echo "🧹 Cleaning build files..."

# Rensa backend target katalog
if [ -d "backend/target" ]; then
    rm -rf backend/target
    echo "✅ Cleaned backend/target"
fi

# Rensa frontend node_modules (valfritt, kommentera bort om du vill behålla)
if [ -d "frontend/node_modules" ]; then
    echo "🤔 Do you want to clean frontend/node_modules? (y/N)"
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        rm -rf frontend/node_modules
        echo "✅ Cleaned frontend/node_modules"
    else
        echo "⏭️  Skipped frontend/node_modules cleanup"
    fi
fi

# Visa resultat
echo ""
echo "🎉 Build completed successfully!"
echo "📦 JAR file: FS-Monitor.jar"
echo "📏 Size: $(du -h FS-Monitor.jar | cut -f1)"
echo ""
echo "🚀 To run the application:"
echo "   java -jar FS-Monitor.jar"
echo ""
echo "🔧 To run with specific profile:"
echo "   java -jar FS-Monitor.jar --spring.profiles.active=prod"
echo ""
echo "📊 To run with custom port:"
echo "   java -jar FS-Monitor.jar --server.port=8080"
