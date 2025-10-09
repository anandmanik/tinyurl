#!/bin/bash
# Manual cleanup script for TinyURL services

echo "🧹 Manual TinyURL Cleanup Starting..."

# Stop and remove all TinyURL containers
echo "🛑 Stopping TinyURL containers..."
docker stop tinyurl-mysql tinyurl-redis tinyurl-backend tinyurl-frontend 2>/dev/null || true

echo "🗑️ Removing TinyURL containers..."
docker rm -f tinyurl-mysql tinyurl-redis tinyurl-backend tinyurl-frontend 2>/dev/null || true

# Remove any containers with tinyurl in the name
echo "🔍 Removing any additional tinyurl containers..."
docker ps -a --filter name=tinyurl --format "{{.ID}}" | xargs -r docker rm -f 2>/dev/null || true

# Remove TinyURL networks
echo "🌐 Removing TinyURL networks..."
docker network rm tinyurl-global-network 2>/dev/null || true
docker network rm tinyurl_tinyurl-network 2>/dev/null || true

# Remove TinyURL images (optional - uncomment if needed)
echo "🖼️ Removing TinyURL images..."
docker images --filter reference="tinyurl*" --format "{{.Repository}}:{{.Tag}}" | xargs -r docker rmi -f 2>/dev/null || true

# Clean up unused Docker resources
echo "🧽 Cleaning up unused Docker resources..."
docker system prune -f || true

# Show final status
echo "📊 Final status:"
echo "Containers:"
docker ps -a --filter name=tinyurl || echo "No TinyURL containers found"
echo ""
echo "Networks:"
docker network ls | grep tinyurl || echo "No TinyURL networks found"
echo ""
echo "Images:"
docker images | grep tinyurl || echo "No TinyURL images found"

echo "✅ Manual cleanup completed!"
echo "💡 You can now run: docker-compose up or trigger Jenkins build"