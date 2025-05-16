pipeline {
    agent any

    environment {
        // Tag the Docker image with the build number
        DOCKER_IMAGE = "order-service:${env.BUILD_NUMBER}"
        // Local Docker registry address
        REGISTRY      = "localhost:5000"
    }

    tools {
        // Assumes you have a Jenkins tool named "Python3.9"
        python 'Python3.9'
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
                success { echo "🟢 Build succeeded" }
                failure { error "🔴 Build failed" }
            }
        }

        stage('Test') {
            steps {
                sh '''
                  pip install -r requirements.txt dev-requirements.txt
                  pytest --junitxml=reports/junit.xml
                '''
                junit 'reports/junit.xml'
            }
        }

        stage('Code Quality') {
            steps {
                sh '''
                  pylint src/**/*.py > reports/pylint.txt || true
                '''
                recordIssues tools: [pylint(pattern: 'reports/pylint.txt')]
            }
        }

        stage('Security') {
            steps {
                sh '''
                  bandit -r src -f html -o reports/bandit.html
                '''
                publishHTML([
                  reportDir:    'reports',
                  reportFiles:  'bandit.html',
                  reportName:   'Bandit Security Report',
                  keepAll:      true,
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
                // Push the image into the local Docker registry
                sh '''
                  docker tag ${DOCKER_IMAGE} ${REGISTRY}/${DOCKER_IMAGE}
                  docker push ${REGISTRY}/${DOCKER_IMAGE}
                '''
            }
        }

        stage('Monitoring & Alerting') {
            steps {
                echo "✅ Monitoring integration would run here"
            }
        }
    }

    post {
        always {
            // Show running containers for debugging
            sh 'docker ps -a'
        }
        cleanup {
            // Tear down test deployment
            sh 'docker rm -f order-service-test || true'
        }
    }
}
