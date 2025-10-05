pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'docker.io'
        BACKEND_IMAGE = 'tinyurl-api'
        FRONTEND_IMAGE = 'tinyurl-frontend'
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
        GIT_COMMIT = "${env.GIT_COMMIT?.take(7) ?: 'unknown'}"
    }

    tools {
        maven 'Maven-3.9'
        nodejs 'NodeJS-18'
    }

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

        stage('Build & Test') {
            parallel {
                stage('Backend Pipeline') {
                    steps {
                        script {
                            docker.image('eclipse-temurin:25-jdk').inside('-v /var/run/docker.sock:/var/run/docker.sock') {
                                stage('Backend: Build') {
                                    sh '''
                                        cd tinyurl-api
                                        ./mvnw clean compile -DskipTests
                                    '''
                                }

                                stage('Backend: Test') {
                                    sh '''
                                        cd tinyurl-api
                                        ./mvnw test
                                    '''

                                    publishTestResults testResultsPattern: 'tinyurl-api/target/surefire-reports/*.xml'

                                    publishHTML([
                                        allowMissing: false,
                                        alwaysLinkToLastBuild: true,
                                        keepAll: true,
                                        reportDir: 'tinyurl-api/target/site/jacoco',
                                        reportFiles: 'index.html',
                                        reportName: 'Backend Coverage Report'
                                    ])
                                }

                                stage('Backend: Package') {
                                    sh '''
                                        cd tinyurl-api
                                        ./mvnw package -DskipTests
                                    '''

                                    archiveArtifacts artifacts: 'tinyurl-api/target/*.jar', fingerprint: true
                                }

                                stage('Backend: Docker Build') {
                                    sh '''
                                        cd tinyurl-api
                                        docker build -t ${BACKEND_IMAGE}:${BUILD_NUMBER} .
                                        docker tag ${BACKEND_IMAGE}:${BUILD_NUMBER} ${BACKEND_IMAGE}:latest
                                    '''
                                }
                            }
                        }
                    }
                }

                stage('Frontend Pipeline') {
                    steps {
                        script {
                            docker.image('node:18-alpine').inside('-v /var/run/docker.sock:/var/run/docker.sock') {
                                stage('Frontend: Install Dependencies') {
                                    sh '''
                                        cd tinyurl-frontend
                                        npm ci
                                    '''
                                }

                                stage('Frontend: Lint') {
                                    sh '''
                                        cd tinyurl-frontend
                                        npm run lint
                                    '''
                                }

                                stage('Frontend: Test') {
                                    sh '''
                                        cd tinyurl-frontend
                                        npm run test:ci
                                    '''

                                    publishTestResults testResultsPattern: 'tinyurl-frontend/coverage/junit.xml'

                                    publishHTML([
                                        allowMissing: false,
                                        alwaysLinkToLastBuild: true,
                                        keepAll: true,
                                        reportDir: 'tinyurl-frontend/coverage/lcov-report',
                                        reportFiles: 'index.html',
                                        reportName: 'Frontend Coverage Report'
                                    ])
                                }

                                stage('Frontend: Build') {
                                    sh '''
                                        cd tinyurl-frontend
                                        npm run build
                                    '''

                                    archiveArtifacts artifacts: 'tinyurl-frontend/dist/**/*', fingerprint: true
                                }

                                stage('Frontend: Docker Build') {
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
                        # Start services with existing docker-compose.yml
                        docker-compose up -d

                        # Wait for services to be ready
                        sleep 30

                        # Run integration tests
                        docker-compose exec -T tinyurl-api ./mvnw test -Dtest=**/*IntegrationTest
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
                            ./mvnw org.owasp:dependency-check-maven:check
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
                        # Deploy using existing docker-compose.yml
                        docker-compose up -d

                        # Health check
                        sleep 15
                        curl -f http://localhost:8080/actuator/health || exit 1
                        curl -f http://localhost:3000 || exit 1
                    '''
                }
            }
        }
    }

    post {
        always {
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