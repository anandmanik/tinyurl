# TinyURL Service

A complete URL shortening service built with Spring Boot backend and React frontend, featuring JWT authentication, Redis caching, and MySQL persistence.

## 🚀 Features

- **URL Shortening**: Convert long URLs into short, shareable links
- **User Management**: JWT-based authentication with user-specific URL management
- **Caching**: Redis-powered caching for optimal performance
- **Analytics**: Track URL usage and creation timestamps
- **Redirect Service**: Fast 301 permanent redirects with proper cache headers
- **Docker Support**: Full containerization for easy deployment
- **Modern UI**: Clean, responsive React frontend with Tailwind CSS

## 🏗️ Architecture

### Backend (Spring Boot)
- **Framework**: Spring Boot 3.2.0 with Java 25
- **Database**: MySQL 8.0 with Flyway migrations
- **Cache**: Redis 7 for performance optimization
- **Security**: JWT-based stateless authentication
- **API**: RESTful endpoints with comprehensive error handling

### Frontend (React)
- **Framework**: React 18 with TypeScript
- **Styling**: Tailwind CSS for modern, responsive design
- **Build Tool**: Vite for fast development and builds
- **State Management**: React hooks and context

### Infrastructure
- **Containerization**: Docker and Docker Compose
- **Database**: MySQL with persistent volumes
- **Cache**: Redis with TTL and LRU eviction
- **Networking**: Docker bridge network for service communication

## 📋 Prerequisites

- Docker and Docker Compose
- Node.js 18+ (for local development)
- Java 21+ (for local development)
- Maven 3.8+ (for local development)

## 🚀 Quick Start

### Using Docker (Recommended)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd tinyurl
   ```

2. **Start all services**
   ```bash
   docker-compose up -d
   ```

3. **Verify services are running**
   ```bash
   docker ps
   curl http://localhost:8080/api/healthz
   ```

4. **Access the application**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080
   - Health Check: http://localhost:8080/api/healthz

### Local Development

1. **Start infrastructure services**
   ```bash
   docker-compose up -d mysql redis
   ```

2. **Configure environment**
   ```bash
   cp env.properties.example env.properties
   # Edit env.properties with your local settings
   ```

3. **Start backend**
   ```bash
   cd tinyurl-api
   mvn spring-boot:run
   ```

4. **Start frontend**
   ```bash
   cd tinyurl-frontend
   npm install
   npm start
   ```

## 🔧 Configuration

### Environment Variables

The application uses environment-specific configuration:

- **Production/Docker**: Environment variables in `docker-compose.yml`
- **Local Development**: `env.properties` file
- **Testing**: Application-test.properties

#### Key Configuration Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `API_PORT` | Backend server port | 8080 |
| `FRONTEND_PORT` | Frontend dev server port | 3000 |
| `MYSQL_URL` | MySQL connection URL | localhost:3306 |
| `REDIS_URL` | Redis connection URL | redis://localhost:6379 |
| `JWT_SECRET` | JWT signing secret | (generated) |
| `BASE_URL` | Base URL for short links | http://localhost |

### Database Configuration

The application uses Flyway for database migrations:

```sql
-- Example migration (V1__init.sql)
CREATE TABLE urls (
    code VARCHAR(7) PRIMARY KEY,
    normalized_url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_created_at (created_at)
);

CREATE TABLE user_urls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(6) NOT NULL,
    code VARCHAR(7) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (code) REFERENCES urls(code) ON DELETE CASCADE,
    UNIQUE KEY unique_user_code (user_id, code),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
);
```

## 📡 API Documentation

### Authentication

All API endpoints (except health check and redirects) require JWT authentication:

```bash
# Get JWT token
curl -X POST http://localhost:8080/api/token \
  -H "Content-Type: application/json" \
  -d '{"userId": "user123"}'

# Use token in subsequent requests
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/urls
```

### Endpoints

#### Authentication
- `POST /api/token` - Generate JWT token

#### URL Management
- `POST /api/urls` - Create/retrieve short URL
- `GET /api/urls` - List user's URLs
- `DELETE /api/urls/{code}` - Remove URL association

#### Redirect Service
- `GET /{code}` - Redirect to original URL (no auth required)

#### Health Check
- `GET /api/healthz` - System health status

### Example API Usage

```bash
# Create short URL
curl -X POST http://localhost:8080/api/urls \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/very/long/url"}'

# Response
{
  "code": "abc123d",
  "shortUrl": "http://localhost/abc123d",
  "url": "https://example.com/very/long/url",
  "createdAt": "2023-10-05T12:00:00Z",
  "existed": false
}
```

## 🏃‍♂️ Development

### Project Structure

```
tinyurl/
├── tinyurl-api/                 # Spring Boot backend
│   ├── src/main/java/
│   │   └── com/amtinyurl/
│   │       ├── controller/      # REST controllers
│   │       ├── service/         # Business logic
│   │       ├── entity/          # JPA entities
│   │       ├── repository/      # Data access
│   │       ├── security/        # JWT and security
│   │       └── dto/            # Data transfer objects
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   ├── application-docker.properties
│   │   └── db/migration/       # Flyway migrations
│   └── Dockerfile
├── tinyurl-frontend/           # React frontend
│   ├── src/
│   │   ├── components/         # React components
│   │   ├── hooks/             # Custom hooks
│   │   ├── types/             # TypeScript types
│   │   └── utils/             # Utility functions
│   ├── public/
│   ├── Dockerfile
│   └── nginx.conf
├── docker-compose.yml          # Docker orchestration
├── env.properties             # Local development config
└── README.md
```

### Key Design Decisions

1. **Global URL Deduplication**: URLs are normalized and deduplicated globally, with user associations tracked separately
2. **JWT Authentication**: Stateless, permanent tokens with case-insensitive userId normalization
3. **Base36 Short Codes**: 7-character lowercase codes with collision retry (max 3 attempts)
4. **Redis Caching**: Bidirectional caching (code→URL, URL→code) with 5-minute TTL
5. **MySQL Storage**: Two-table design (urls, user_urls) with proper foreign key constraints

### URL Normalization

The service normalizes URLs for deduplication:
- Lowercase scheme and host
- Preserve path and query parameters exactly
- Remove default ports (80 for HTTP, 443 for HTTPS)

### Short Code Generation

- **Algorithm**: Base36 encoding (0-9, a-z)
- **Length**: 7 characters (36^7 = ~78 billion combinations)
- **Collision Handling**: Up to 3 retry attempts with new random codes
- **Format**: Lowercase alphanumeric only

## 🐳 Docker Deployment

### Services

The application consists of four Docker services:

1. **MySQL** (`mysql:8.0`)
   - Port: 3306
   - Volume: `mysql_data`
   - Health check: mysqladmin ping

2. **Redis** (`redis:7-alpine`)
   - Port: 6379
   - Volume: `redis_data`
   - Health check: redis-cli ping

3. **Backend** (`tinyurl-backend`)
   - Port: 8080
   - Health check: curl /api/healthz
   - Depends on: MySQL, Redis

4. **Frontend** (`tinyurl-frontend`)
   - Port: 3000 (mapped to 80 inside container)
   - Nginx reverse proxy
   - Depends on: Backend

### Production Deployment

For production deployment, update the following:

1. **Environment Variables**
   ```yaml
   environment:
     - BASE_URL=https://yourdomain.com
     - JWT_SECRET=your-secure-secret-key
   ```

2. **SSL Termination**
   Add SSL termination at load balancer or reverse proxy level

3. **Database Security**
   - Use strong passwords
   - Enable SSL connections
   - Restrict network access

4. **Monitoring**
   - Add monitoring for health endpoints
   - Set up log aggregation
   - Configure alerts for service failures

## 🧪 Testing

### Backend Tests

```bash
cd tinyurl-api
mvn test
```

### Frontend Tests

```bash
cd tinyurl-frontend
npm test
```

### Integration Testing

```bash
# Start all services
docker-compose up -d

# Run integration tests
curl http://localhost:8080/api/healthz
curl http://localhost:3000/
```

## 📊 Monitoring

### Health Checks

The backend provides comprehensive health checks:

```bash
curl http://localhost:8080/api/healthz
```

Response:
```json
{
  "checks": {
    "mysql": "ok",
    "redis": "ok"
  },
  "status": "ok"
}
```

### Logs

Application logs are structured for easy parsing:

```bash
# View backend logs
docker logs tinyurl-backend

# View frontend logs
docker logs tinyurl-frontend

# Follow logs in real-time
docker-compose logs -f
```

## 🔒 Security

### Authentication
- JWT tokens with configurable expiration
- Stateless authentication (no server-side sessions)
- Case-insensitive user ID normalization

### Input Validation
- URL format validation
- Short code format validation (7-character alphanumeric)
- Request size limits

### Security Headers
- CORS configuration for frontend integration
- Standard security headers in responses

## 🚨 Troubleshooting

### Common Issues

1. **Redis Connection Failed**
   ```bash
   # Check Redis URL environment variable
   docker exec tinyurl-backend env | grep REDIS_URL
   # Should show: REDIS_URL=redis://redis:6379
   ```

2. **MySQL Connection Failed**
   ```bash
   # Check MySQL health
   docker exec tinyurl-mysql mysqladmin ping -h localhost
   ```

3. **Frontend Not Loading**
   ```bash
   # Check if backend is accessible
   curl http://localhost:8080/api/healthz
   ```

4. **Short URLs Not Working**
   - Verify redirect controller is not prefixed with `/api`
   - Check WebConfig excludes RedirectController from API prefix

### Debug Commands

```bash
# Check all container status
docker ps

# View container logs
docker logs <container-name>

# Access container shell
docker exec -it <container-name> /bin/bash

# Test network connectivity
docker exec tinyurl-backend timeout 5 bash -c "</dev/tcp/redis/6379"
```

## 📈 Performance

### Caching Strategy
- Redis TTL: 5 minutes for URL mappings
- Cache hit ratio tracking in logs
- LRU eviction for memory management

### Database Optimization
- Indexed columns for fast lookups
- Connection pooling with HikariCP
- Prepared statements for security

### Frontend Optimization
- Static asset optimization with Vite
- Nginx compression and caching
- Responsive images and lazy loading

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- React team for the frontend library
- Redis and MySQL teams for robust data storage solutions
- Docker team for containerization technology