pipeline {
    agent any

    environment {
        // Tag the Docker image with the build number
        DOCKER_IMAGE         = "order-service:${env.BUILD_NUMBER}"
        DOCKER_HUB_NAMESPACE = "lcgalappaththi94"
        IMAGE_NAME           = "${DOCKER_HUB_NAMESPACE}/order-service"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh '''
                  docker build \
                    -t ${DOCKER_IMAGE} \
                    -f Infrastructure/Dockerfile \
                    .
                '''
            }
            post {
                success { echo '🟢 Build succeeded' }
                failure { error '🔴 Build failed' }
            }
        }

        stage('Test') {
            steps {
                script {
                    withCredentials([usernamePassword(
                    credentialsId: 'docker-hub-creds',
                    usernameVariable: 'DOCKERHUB_USER',
                    passwordVariable: 'DOCKERHUB_PASS'
                )]) {
                    sh '''
                      echo "$DOCKERHUB_PASS" | docker login --username "$DOCKERHUB_USER" --password-stdin
                    '''
                    }

                    docker.image('python:3.9-slim').inside("-u root -v $PWD:$PWD -w $PWD") {
                        sh 'pip install -r requirements.txt dev-requirements.txt'
                        sh 'pytest --junitxml=reports/junit.xml'
                    }
                }
            }
            post {
                always {
                    junit 'reports/junit.xml'
                }
            }
        }

        stage('Code Quality') {
            steps {
                script {
                    docker.image('python:3.9-slim').inside("-u root -v $PWD:$PWD -w $PWD") {
                        sh 'pylint src/**/*.py > reports/pylint.txt || true'
                    }
                }
                recordIssues tools: [pylint(pattern: 'reports/pylint.txt')]
            }
        }

        stage('Security') {
            steps {
                script {
                    docker.image('python:3.9-slim').inside("-u root -v $PWD:$PWD -w $PWD") {
                        sh 'bandit -r src -f html -o reports/bandit.html'
                    }
                }
                publishHTML([
                    reportDir:       'reports',
                    reportFiles:     'bandit.html',
                    reportName:      'Bandit Security Report',
                    alwaysLinkToLastBuild: true
                ])
            }
        }

        stage('Deploy to Test') {
            steps {
                sh '''
                  docker rm -f order-service-test || true
                  docker run -d \
                    --name order-service-test \
                    -p 8000:8000 \
                    ${DOCKER_IMAGE}
                '''
            }
        }

        stage('Release') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'docker-hub-creds',
                    usernameVariable: 'DOCKERHUB_USER',
                    passwordVariable: 'DOCKERHUB_PASS'
                )]) {
                    sh '''
                      echo "$DOCKERHUB_PASS" | docker login --username "$DOCKERHUB_USER" --password-stdin
                      docker tag ${DOCKER_IMAGE} ${IMAGE_NAME}:${env.BUILD_NUMBER}
                      docker push ${IMAGE_NAME}:${env.BUILD_NUMBER}
                    '''
                }
            }
        }

        stage('Monitoring & Alerting') {
            steps {
                echo '✅ Monitoring integration placeholder'
            }
        }
    }

    post {
        always {
            sh 'docker ps -a'
        }
        cleanup {
            sh 'docker rm -f order-service-test || true'
        }
    }
}
