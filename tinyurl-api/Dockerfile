# Multi-stage build for TinyURL API
FROM eclipse-temurin:25-jdk AS build

# Install Maven
RUN apt-get update && apt-get install -y maven

# Set working directory
WORKDIR /app

# Copy POM file
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Create a non-root user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Expose the port that the application runs on
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/healthz || exit 1

# Run the application
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "/app/app.jar"]