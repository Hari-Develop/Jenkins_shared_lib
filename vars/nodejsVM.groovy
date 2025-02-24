def call (Map configMap){
pipeline {
    agent {
        node {
            label "Jenkins_agent"
        }
    }
    environment {
        packageVersion = ''
    }
    options {
        disableConcurrentBuilds()
        ansiColor('xterm')
    }
    stages {
        stage('Get Application Version') {
            steps {
                script {
                    def packageJson = readJSON file: 'package.json'
                    packageVersion = packageJson.version
                    echo "Application version: ${packageVersion}"
                }
            }
        }
        stage('Build Application') {
            steps {
                sh 'npm install'
                sh 'npm run build'
            }
        }
        stage('Run Unit Tests') {
            steps {
                sh 'npm test'
            }
        }
        stage('Code Quality Analysis') {
            steps {
                sh 'sonar-scanner'
            }
        }
        stage('Build Docker Image') {
            steps {
                sh 'docker build -t harinadhareddy/nodejs-app:${packageVersion} .'
            }
        }
        stage('Push Docker Image') {
            steps {
                withDockerRegistry([credentialsId: 'docker-hub-auth', url: '']) {
                    sh 'docker push harinadhareddy/nodejs-app:${packageVersion}'
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                sh 'kubectl set image deployment/nodejs-app nodejs-app=harinadhareddy/nodejs-app:${packageVersion} --record'
            }
        }
        stage('Smoke Test') {
            steps {
                sh 'curl -f http://myapp.mydomain.com/health || exit 1'
            }
        }
    }
    post {
        always {
            echo "Pipeline execution completed"
        }
        failure {
            echo "Pipeline execution failed"
        }
        success {
            echo "Pipeline executed successfully"
        }
    }
}

}
