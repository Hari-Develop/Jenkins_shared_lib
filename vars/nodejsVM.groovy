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
                        def packageJson = readJSON file: 'nodejs-app/package.json'
                        packageVersion = packageJson.version
                        echo "Application version: ${packageVersion}"
                    }
                }
            }
            stage('Build Application') {
                steps {
                    dir('nodejs-app') { 
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
            stage('Deploy Pre-requisites') {
                steps {
                    sh '''
                        kubectl apply -f config/configMap.yaml
                        kubectl apply -f config/secret.yaml
                        kubectl apply -f postgres/statefulset.yaml
                        kubectl apply -f postgres/service.yaml
                    '''
                }
        }
        stage('Deploy Node.js Application') {
                steps {
                    sh '''
                        kubectl apply -f deployment/deployment.yaml
                        kubectl apply -f deployment/service.yaml
                        kubectl apply -f deployment/ingress.yaml
                    '''
                }
        }
        }
        post {
            always {
                echo "Pipeline execution completed"
                deleteDir()
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
