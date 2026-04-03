#!/bin/bash

# --- 1. THE SINGLE PROMPT ---
echo "Choose your environment:"
echo "1) Local (Runs via Maven | Profile: test )"
echo "2) Dev   (Runs via Docker | Profile: dev  | Default)"
echo "3) Prod  (Runs via Docker | Profile: prod)"
read -p "Selection [1/2/3]: " choice

# --- 2. LOGIC MAPPING ---
case $choice in
    1)
        MODE="local"
        PROFILE="test"
        ;;
    3)
        MODE="docker"
        PROFILE="prod"
        ;;
    *)
        # Defaults to Docker Dev for option 2 or any other keypress
        MODE="docker"
        PROFILE="dev"
        ;;
esac

echo "------------------------------------------------"
echo "RUNNING: Mode: $MODE | Profile: $PROFILE"
echo "------------------------------------------------"

# --- 3. CLEANUP & BUILD ---
if [ "$MODE" == "local" ]; then
    echo "Stopping Docker containers to free up port 8080..."
    docker-compose down
    # Kill any zombie java processes on 8080
    lsof -ti:8080 | xargs kill -9 2>/dev/null
else
    echo "Preparing Docker build..."
    docker-compose down -v
fi

echo "Step 1: Packaging application..."
./mvnw clean package -DskipTests

# --- 4. EXECUTION ---
if [ "$MODE" == "local" ]; then
    echo "Step 2: Starting locally via Maven (Profile: $PROFILE)..."
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=$PROFILE
else
    echo "Step 2: Starting Docker (Profile: $PROFILE)..."
    SPRING_PROFILES_ACTIVE=$PROFILE docker-compose up --build -d app
    docker-compose logs -f app
fi