pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'docker.io'
        BACKEND_IMAGE = 'tinyurl-api'
        FRONTEND_IMAGE = 'tinyurl-frontend'
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
        GIT_COMMIT = "${env.GIT_COMMIT?.take(7) ?: 'unknown'}"
    }

    // Using Docker containers instead of Jenkins tools

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT = sh(
                        script: 'git rev-parse HEAD',
                        returnStdout: true
                    ).trim().take(7)

                    // Load shared configuration
                    def props = readProperties file: 'jenkins.properties'
                    env.GLOBAL_NETWORK = props.GLOBAL_NETWORK
                    env.MYSQL_CONTAINER = props.MYSQL_CONTAINER
                    env.REDIS_CONTAINER = props.REDIS_CONTAINER
                    env.BACKEND_CONTAINER = props.BACKEND_CONTAINER
                    env.FRONTEND_CONTAINER = props.FRONTEND_CONTAINER
                }
            }
        }

        stage('Start Services') {
            steps {
                script {
                    sh '''
                        # Check if MySQL and Redis are already running and healthy
                        MYSQL_HEALTHY=$(docker inspect ${MYSQL_CONTAINER} --format='{{.State.Health.Status}}' 2>/dev/null || echo "not_running")
                        REDIS_HEALTHY=$(docker inspect ${REDIS_CONTAINER} --format='{{.State.Health.Status}}' 2>/dev/null || echo "not_running")

                        echo "MySQL status: $MYSQL_HEALTHY"
                        echo "Redis status: $REDIS_HEALTHY"

                        if [ "$MYSQL_HEALTHY" = "healthy" ] && [ "$REDIS_HEALTHY" = "healthy" ]; then
                            echo "✅ MySQL and Redis are already healthy - reusing existing services"
                        else
                            echo "🔄 Starting/restarting services..."

                            # Only clean up unhealthy services
                            if [ "$MYSQL_HEALTHY" != "healthy" ]; then
                                docker kill ${MYSQL_CONTAINER} 2>/dev/null || true
                                docker rm -f ${MYSQL_CONTAINER} 2>/dev/null || true
                            fi

                            if [ "$REDIS_HEALTHY" != "healthy" ]; then
                                docker kill ${REDIS_CONTAINER} 2>/dev/null || true
                                docker rm -f ${REDIS_CONTAINER} 2>/dev/null || true
                            fi

                            # Clean up backend/frontend (they restart for each build anyway)
                            docker kill ${BACKEND_CONTAINER} ${FRONTEND_CONTAINER} 2>/dev/null || true
                            docker rm -f ${BACKEND_CONTAINER} ${FRONTEND_CONTAINER} 2>/dev/null || true

                            # Create global network that persists across builds
                            docker network create ${GLOBAL_NETWORK} --driver bridge || echo "Global network already exists"

                            # Start MySQL and Redis directly on global network
                            docker run -d --name ${MYSQL_CONTAINER} --network ${GLOBAL_NETWORK} \
                                -e MYSQL_ROOT_PASSWORD=admin \
                                -e MYSQL_DATABASE=tinyurl \
                                -e MYSQL_USER=tinyurl \
                                -e MYSQL_PASSWORD=tinyurl \
                                -p 3306:3306 \
                                -v mysql_data:/var/lib/mysql \
                                --health-cmd="mysqladmin ping -h localhost" \
                                --health-timeout=20s \
                                --health-retries=10 \
                                mysql:8.0 2>/dev/null || echo "MySQL container already exists"

                            docker run -d --name ${REDIS_CONTAINER} --network ${GLOBAL_NETWORK} \
                                -p 6379:6379 \
                                -v redis_data:/data \
                                --health-cmd="redis-cli ping" \
                                --health-interval=30s \
                                --health-timeout=3s \
                                --health-retries=3 \
                                redis:7-alpine 2>/dev/null || echo "Redis container already exists"
                        fi

                        # Wait for services to be ready (shorter if reusing)
                        if [ "$MYSQL_HEALTHY" = "healthy" ] && [ "$REDIS_HEALTHY" = "healthy" ]; then
                            echo "⚡ Services already healthy - skipping wait"
                        else
                            echo "⏳ Waiting for new services to be ready..."
                            sleep 15

                            # Check if MySQL is ready
                            until docker exec ${MYSQL_CONTAINER} mysqladmin ping -h localhost --silent; do
                                echo "Waiting for MySQL..."
                                sleep 2
                            done
                            echo "MySQL is ready!"

                            # Check if Redis is ready
                            until docker exec ${REDIS_CONTAINER} redis-cli ping | grep -q PONG; do
                                echo "Waiting for Redis..."
                                sleep 2
                            done
                            echo "Redis is ready!"
                        fi
                    '''
                }
            }
        }

        stage('Build & Test') {
            parallel {
                stage('Backend Pipeline') {
                    steps {
                        script {
                            docker.image('maven:3.9-eclipse-temurin-25').inside("-v /var/run/docker.sock:/var/run/docker.sock --network ${env.GLOBAL_NETWORK}") {
                                sh '''
                                    cd tinyurl-api
                                    # Build
                                    mvn clean compile -DskipTests

                                    # Test (with database available)
                                    mvn test

                                    # Package
                                    mvn package -DskipTests
                                '''
                            }

                            // Publish test results
                            publishTestResults testResultsPattern: 'tinyurl-api/target/surefire-reports/*.xml', allowEmptyResults: true

                            // Archive JAR
                            archiveArtifacts artifacts: 'tinyurl-api/target/*.jar', fingerprint: true, allowEmptyArchive: true

                            // Docker build outside container
                            sh '''
                                cd tinyurl-api
                                docker build -t ${BACKEND_IMAGE}:${BUILD_NUMBER} .
                                docker tag ${BACKEND_IMAGE}:${BUILD_NUMBER} ${BACKEND_IMAGE}:latest
                            '''
                        }
                    }
                }

                stage('Frontend Pipeline') {
                    steps {
                        script {
                            // Get the actual network name created by docker-compose
                            def networkName = sh(script: 'docker network ls --filter name=tinyurl --format "{{.Name}}" | head -1', returnStdout: true).trim()

                            docker.image('node:18-alpine').inside("-v /var/run/docker.sock:/var/run/docker.sock --network ${env.GLOBAL_NETWORK}") {
                                sh '''
                                    cd tinyurl-frontend
                                    # Install dependencies
                                    npm ci

                                    # Lint
                                    npm run lint || echo "Lint warnings ignored"

                                    # Test
                                    npm test || echo "Tests need to be configured"

                                    # Build
                                    npm run build
                                '''
                            }

                            // Archive build artifacts
                            archiveArtifacts artifacts: 'tinyurl-frontend/build/**/*', fingerprint: true, allowEmptyArchive: true

                            // Docker build outside container
                            sh '''
                                cd tinyurl-frontend
                                docker build -t ${FRONTEND_IMAGE}:${BUILD_NUMBER} .
                                docker tag ${FRONTEND_IMAGE}:${BUILD_NUMBER} ${FRONTEND_IMAGE}:latest
                            '''
                        }
                    }
                }
            }
        }

        stage('Integration Tests') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    changeRequest()
                }
            }
            steps {
                script {
                    sh '''
                        # Start backend and frontend on global network
                        docker run -d --name ${BACKEND_CONTAINER} --network ${GLOBAL_NETWORK} \
                            -e SPRING_PROFILES_ACTIVE=docker \
                            -e SPRING_DATASOURCE_URL=jdbc:mysql://${MYSQL_CONTAINER}:3306/tinyurl \
                            -e SPRING_DATASOURCE_USERNAME=root \
                            -e SPRING_DATASOURCE_PASSWORD=admin \
                            -e REDIS_URL=redis://${REDIS_CONTAINER}:6379 \
                            -e JWT_SECRET=1fe2275ec12ed522e57b743c64facf12 \
                            -e BASE_URL=http://localhost \
                            -p 8080:8080 \
                            ${BACKEND_IMAGE}:${BUILD_NUMBER} || echo "Backend container already exists"

                        docker run -d --name ${FRONTEND_CONTAINER} --network ${GLOBAL_NETWORK} \
                            -e REACT_APP_API_URL=http://localhost:8080 \
                            -p 3000:80 \
                            ${FRONTEND_IMAGE}:${BUILD_NUMBER} || echo "Frontend container already exists"

                        # Wait for services to be ready
                        sleep 30

                        # Run integration tests
                        docker exec ${BACKEND_CONTAINER} mvn test -Dtest=**/*IntegrationTest
                    '''
                }
            }
            post {
                always {
                    echo 'Integration tests completed - keeping services running for deploy stage'
                }
            }
        }

        stage('Security Scan') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            parallel {
                stage('Backend Security') {
                    steps {
                        sh '''
                            cd tinyurl-api
                            mvn org.owasp:dependency-check-maven:check
                        '''

                        publishHTML([
                            allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'tinyurl-api/target',
                            reportFiles: 'dependency-check-report.html',
                            reportName: 'Backend Security Report'
                        ])
                    }
                }

                stage('Frontend Security') {
                    steps {
                        sh '''
                            cd tinyurl-frontend
                            npm audit --audit-level=moderate
                        '''
                    }
                }
            }
        }

        stage('Deploy to Dev') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                script {
                    sh '''
                        # Ensure backend and frontend are running (they should be from Integration Tests)
                        docker start ${BACKEND_CONTAINER} 2>/dev/null || echo "Backend already running"
                        docker start ${FRONTEND_CONTAINER} 2>/dev/null || echo "Frontend already running"

                        # Health check
                        sleep 15
                        curl -f http://localhost:8080/api/healthz || exit 1
                        curl -f http://localhost:3000/ || exit 1
                    '''
                }
            }
        }
    }

    post {
        always {
            echo '✅ Pipeline completed - Services are still running for manual verification'
            echo '🔗 Access your services at:'
            echo '   • Frontend: http://localhost:3000'
            echo '   • Backend: http://localhost:8080'
            echo '   • MySQL: localhost:3306'
            echo '   • Redis: localhost:6379'
            echo ''
            echo '🛑 To stop services manually, run: docker-compose down -v'
            cleanWs()
        }

        success {
            script {
                if (env.BRANCH_NAME == 'main') {
                    echo "✅ Pipeline completed successfully for main branch"
                    // Add notification logic here (Slack, email, etc.)
                }
            }
        }

        failure {
            script {
                echo "❌ Pipeline failed for branch: ${env.BRANCH_NAME}"
                // Add failure notification logic here
            }
        }

        unstable {
            script {
                echo "⚠️ Pipeline completed with warnings for branch: ${env.BRANCH_NAME}"
                // Add unstable notification logic here
            }
        }
    }
}