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
            }
            stage('Run Unit Tests') {
                steps {
                    dir('nodejs-app') {
                        sh 'npm test'
                    }
                }
            }
            stage('Code Quality Analysis') {
                steps {
                    sh 'sonar-scanner'
                }
            }
            stage('Build Docker Image') {
                steps {
                    sh 'cd nodejs-app && docker build -t harinadhareddy/nodejs-app:latest .'
                }
            }
            stage('Push Docker Image') {
                steps {
                    withDockerRegistry([credentialsId: 'docker-hub-auth', url: '']) {
                        sh 'docker push harinadhareddy/nodejs-app:latest'
                    }
                }
            }
            stage('Deploy Pre-requisites') {
                steps {
                    sh '''
                        kubectl apply -f ${WORKSPACE}/nodejs-app/config/nodejs-config.yaml
                        kubectl apply -f ${WORKSPACE}/nodejs-app/config/nodejs-secret.yaml
                        kubectl apply -f ${WORKSPACE}/nodejs-app/config/PostgreSQL-ConfigMap.yaml
                        kubectl apply -f ${WORKSPACE}/nodejs-app/config/postgreSQL-Secret.yaml
                    '''
                }
            }

            stage('Deploy Node.js Application') {
                steps {
                    sh '''
                        kubectl apply -f ${WORKSPACE}/nodejs-app/deployment/deployment.yaml
                        kubectl apply -f ${WORKSPACE}/nodejs-app/deployment/ingress.yaml
                        kubectl apply -f ${WORKSPACE}/nodejs-app/deployment/nodjs-Service.yaml
                        kubectl apply -f ${WORKSPACE}/nodejs-app/deployment/PostgreSQL-Service.yaml
                        kubectl apply -f ${WORKSPACE}/nodejs-app/deployment/PostgreSQL-StatefulSet.yaml
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
