# TinyURL Service - Docker Setup

This document describes how to run the TinyURL service using Docker and Docker Compose.

## Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- At least 4GB RAM available for containers

## Architecture

The Docker setup includes:
- **Frontend**: React app served by Nginx (port 3000)
- **Backend**: Spring Boot API (port 8082)
- **Database**: MySQL 8.0 (port 3306)
- **Cache**: Redis 7 (port 6379)

## Quick Start

1. **Clone and navigate to the project:**
   ```bash
   cd tinyurl
   ```

2. **Build and start all services:**
   ```bash
   docker-compose up --build
   ```

3. **Access the application:**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8082
   - Health Check: http://localhost:8082/healthz

## Development Commands

### Build specific services
```bash
# Build backend only
docker-compose build backend

# Build frontend only
docker-compose build frontend
```

### Start services
```bash
# Start in background
docker-compose up -d

# Start with logs
docker-compose up

# Start specific service
docker-compose up backend
```

### View logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
```

### Stop services
```bash
# Stop all
docker-compose down

# Stop and remove volumes
docker-compose down -v

# Stop and remove images
docker-compose down --rmi all
```

## Database Management

### Access MySQL
```bash
docker exec -it tinyurl-mysql mysql -u root -padmin
```

### Access Redis
```bash
docker exec -it tinyurl-redis redis-cli
```

### Reset database
```bash
docker-compose down -v mysql
docker-compose up mysql
```

## Environment Configuration

### Backend Configuration
- Database: Configured via environment variables in docker-compose.yml
- Redis: Connected to redis service
- JWT: Uses environment-provided secret
- Base URL: Configurable via BASE_URL environment variable

### Frontend Configuration
- API URL: Points to backend service
- Build: Optimized production build
- Nginx: Serves static files and proxies API requests

## Health Checks

All services include health checks:
- **MySQL**: `mysqladmin ping`
- **Redis**: `redis-cli ping`
- **Backend**: `curl /healthz`
- **Frontend**: `curl /` (nginx status)

## Troubleshooting

### Common Issues

1. **Port conflicts:**
   ```bash
   # Check what's using the ports
   lsof -i :3000
   lsof -i :8082
   lsof -i :3306
   lsof -i :6379
   ```

2. **Database connection issues:**
   ```bash
   # Check MySQL logs
   docker-compose logs mysql

   # Restart MySQL
   docker-compose restart mysql
   ```

3. **Backend startup issues:**
   ```bash
   # Check backend logs
   docker-compose logs backend

   # Rebuild backend
   docker-compose build backend --no-cache
   ```

4. **Frontend build issues:**
   ```bash
   # Check build logs
   docker-compose logs frontend

   # Rebuild frontend
   docker-compose build frontend --no-cache
   ```

### Clean Reset
```bash
# Complete reset
docker-compose down -v --rmi all
docker system prune -a
docker-compose up --build
```

## Production Considerations

For production deployment:

1. **Security:**
   - Change default passwords
   - Use secrets management
   - Enable HTTPS
   - Configure firewall rules

2. **Performance:**
   - Use production-grade MySQL configuration
   - Configure Redis persistence
   - Set up load balancing
   - Configure CDN for static assets

3. **Monitoring:**
   - Add health check endpoints
   - Configure log aggregation
   - Set up metrics collection
   - Configure alerting

## Network Configuration

Services communicate via the `tinyurl-network` bridge network:
- Frontend → Backend: `http://backend:8082`
- Backend → MySQL: `jdbc:mysql://mysql:3306/tinyurl`
- Backend → Redis: `redis://redis:6379`

## Data Persistence

Persistent volumes:
- `mysql_data`: MySQL database files
- `redis_data`: Redis persistence files

Data survives container restarts but is removed with `docker-compose down -v`.