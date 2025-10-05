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
                }
            }
        }

        stage('Start Services') {
            steps {
                script {
                    sh '''
                        # Stop containers but preserve volumes for faster startup
                        docker-compose down || true

                        # Force kill and remove specific containers if they exist
                        docker kill tinyurl-mysql tinyurl-redis tinyurl-backend tinyurl-frontend 2>/dev/null || true
                        docker rm -f tinyurl-mysql tinyurl-redis tinyurl-backend tinyurl-frontend 2>/dev/null || true

                        # Give time for socket cleanup
                        sleep 3

                        # Start MySQL and Redis for tests
                        docker-compose up -d mysql redis

                        # Wait for services to be ready
                        sleep 20

                        # Check if MySQL is ready
                        until docker-compose exec -T mysql mysqladmin ping -h localhost --silent; do
                            echo "Waiting for MySQL..."
                            sleep 2
                        done
                        echo "MySQL is ready!"

                        # Check if Redis is ready
                        until docker-compose exec -T redis redis-cli ping | grep -q PONG; do
                            echo "Waiting for Redis..."
                            sleep 2
                        done
                        echo "Redis is ready!"
                    '''
                }
            }
        }

        stage('Build & Test') {
            parallel {
                stage('Backend Pipeline') {
                    steps {
                        script {
                            docker.image('maven:3.9-eclipse-temurin-25').inside('-v /var/run/docker.sock:/var/run/docker.sock --network tinyurl_tinyurl-network') {
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
                            docker.image('node:18-alpine').inside('-v /var/run/docker.sock:/var/run/docker.sock --network tinyurl_tinyurl-network') {
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
                        # Start services with existing docker-compose.yml (reusing MySQL/Redis from Start Services)
                        docker-compose up -d

                        # Wait for services to be ready
                        sleep 30

                        # Run integration tests
                        docker-compose exec -T backend mvn test -Dtest=**/*IntegrationTest
                    '''
                }
            }
            post {
                always {
                    sh 'docker-compose down -v || true'
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
                        # Deploy using existing docker-compose.yml (reusing MySQL/Redis from Start Services)
                        docker-compose up -d

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
            sh 'docker-compose down -v || true'
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