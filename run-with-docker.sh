#!/bin/bash
# Script to run TinyURL backend locally with Docker containers for MySQL and Redis
# This mimics the Jenkins pipeline setup for local testing

set -e

# Source the environment variables
source env.properties

echo "🐳 Starting MySQL and Redis containers..."

# Create network if it doesn't exist
docker network create tinyurl-global-network --driver bridge 2>/dev/null || echo "Network already exists"

# Start MySQL container
docker run -d --name tinyurl-mysql --network tinyurl-global-network \
    -e MYSQL_ROOT_PASSWORD=admin \
    -e MYSQL_DATABASE=tinyurl \
    -e MYSQL_USER=root \
    -e MYSQL_PASSWORD=admin \
    -p 3306:3306 \
    mysql:8.0 2>/dev/null || echo "MySQL container already running"

# Start Redis container
docker run -d --name tinyurl-redis --network tinyurl-global-network \
    -p 6379:6379 \
    redis:7-alpine 2>/dev/null || echo "Redis container already running"

echo "⏳ Waiting for containers to be ready..."

# Wait for MySQL
echo "Waiting for MySQL..."
for i in {1..30}; do
    if docker exec tinyurl-mysql mysqladmin ping -h localhost --silent 2>/dev/null; then
        echo "✅ MySQL is ready!"
        break
    fi
    sleep 2
done

# Wait for Redis
echo "Waiting for Redis..."
for i in {1..15}; do
    if docker exec tinyurl-redis redis-cli ping | grep -q PONG 2>/dev/null; then
        echo "✅ Redis is ready!"
        break
    fi
    sleep 1
done

echo ""
echo "🚀 Containers are ready! Now you can run the Spring Boot application with:"
echo ""
echo "   cd tinyurl-api"
echo "   export MYSQL_URL=tinyurl-mysql:3306"
echo "   export REDIS_URL=redis://tinyurl-redis:6379"
echo "   mvn spring-boot:run"
echo ""
echo "Or run them all at once:"
echo "   cd tinyurl-api && MYSQL_URL=tinyurl-mysql:3306 REDIS_URL=redis://tinyurl-redis:6379 mvn spring-boot:run"
echo ""
echo "To stop containers:"
echo "   docker stop tinyurl-mysql tinyurl-redis"
echo "   docker rm tinyurl-mysql tinyurl-redis"
echo "   docker network rm tinyurl-global-network"