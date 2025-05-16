pipeline:
    agent any

    stages {
        stage('Build') {
            steps {
                script {
                    echo 'Building...'
                    // Add your build commands here

                }
            }
        }
        stage('Test') {
            steps {
                script {
                    echo 'Testing...'

                }
            }
        }
        stage('Deploy') {
            steps {
                script {
                    echo 'Deploying...'
                    // Add your deployment commands here
                }
            }
        }
    }

    post {
        always {
            script {
                echo 'Cleaning up...'
                // Add cleanup commands here
            }
        }
    }
}