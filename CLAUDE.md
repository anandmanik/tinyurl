# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a TinyURL service implementation with a Java Spring Boot backend and React frontend. The project follows a comprehensive specification in `SPEC.md` and has a detailed implementation plan in `prompt_plan.md`.

## Key Project Files

- `idea.md` - Original project concept and features
- `SPEC.md` - Detailed technical specification for the TinyURL service
- `prompt_plan.md` - Step-by-step implementation plan with 10 phases
- `env.properties` - Environment configuration for local development
- `jenkins.properties` - Jenkins CI/CD pipeline configuration
- `Jenkinsfile` - Jenkins pipeline with Docker-in-Docker builds
- `Jenkinsfile.cleanup` - Separate cleanup pipeline for services

## Architecture

- **Backend**: Java Spring Boot microservice with Maven
- **Frontend**: React + Tailwind CSS with Vite
- **Database**: MySQL for persistent storage
- **Cache**: Redis for performance optimization
- **Authentication**: Stateless JWT-based user identification

## Configuration

### Local Development
System settings are managed via `env.properties`:
- API_PORT=8082 (Spring Boot server)
- FRONTEND_PORT=3000 (React dev server)
- MySQL connection settings
- Redis connection URL
- JWT secret for token signing
- BASE_URL for short URL generation

### CI/CD Pipeline
Jenkins configuration is managed via `jenkins.properties`:
- Container and network names
- Database credentials for testing
- Application ports and URLs
- Logging levels for debugging
- Docker image cleanup settings

### Application Properties
Spring Boot uses environment variable substitution with defaults:
- `server.port=${API_PORT:8082}`
- `spring.datasource.url=jdbc:mysql://${MYSQL_URL:localhost:3306}/tinyurl`
- Supports both local development and containerized deployment

## Implementation Progress Tracking

The implementation follows the phases outlined in `prompt_plan.md`. When working on implementation:

1. Always read the relevant phase in `prompt_plan.md` before starting
2. Mark completed steps as done in `prompt_plan.md`
3. Write and run tests for each implemented feature
4. Check backend.log and frontend.log files for server output

## Development Workflow

### Local Development
- Backend and frontend servers are run separately by the user
- Server logs are redirected to `backend.log` and `frontend.log` in project root
- All commits must be confirmed before pushing
- Tests must pass before moving to next implementation phase

### CI/CD Pipeline
- Jenkins multibranch pipeline with GitHub integration
- Docker-in-Docker builds using Maven and Node containers
- Parallel backend/frontend builds with dependency caching
- Integration tests with health checks
- Automatic Docker image and container cleanup
- Services kept running after successful builds for manual verification
- Separate cleanup pipeline for complete service reset

## Key Implementation Notes

- **URL Normalization**: Lowercase scheme/host only, preserve path/query exactly
- **Short Codes**: 7-character lowercase Base36 with collision retry (max 3 attempts)
- **Caching**: Redis with 5-minute TTL for code↔URL mappings
- **Security**: JWT tokens are permanent, case-insensitive userId (6 alphanumeric)
- **Database**: Global URL deduplication with user-specific associations

## Important Instructions

Remember that the basic idea for this project is in idea.md, the spec is in spec.md and the prompt plan, which serves as the implementation plan is in prompt_plan.md. Always read these files to get a good idea about how the implementation is to be carried out. Also, remember that after the implementation, tests need to be written and run successfully before moving on to the next step. Once a step has been implemented, always mark it as done in the prompt_plan.md file.

## Current Implementation Status

The TinyURL service is **fully implemented** with:
- ✅ Complete backend API with JWT authentication
- ✅ React frontend with Tailwind CSS
- ✅ MySQL database with Flyway migrations
- ✅ Redis caching with bidirectional mapping
- ✅ Docker containerization and orchestration
- ✅ Jenkins CI/CD pipeline with comprehensive testing
- ✅ Health monitoring and debugging capabilities

## Notes

- Do not start the backend or frontend servers yourself. I will run them with stdout and stderr redirected to <project_root>/backend.log and <project_root>/frontend/frontend.log. You can read these files to understand what's going on.
- Whenever the user asks you to commit / push to git, always do it from the project's root directory and never commit or push without first confirming with me.
- Use env.properties for local development and jenkins.properties for CI/CD configuration
- The dev branch has Jenkins builds paused - remove the "Skip Dev Branch" stage to re-enable