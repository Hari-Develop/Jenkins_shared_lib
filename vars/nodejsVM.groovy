def call (Map configMap){
    pipeline{
        agent{
            node{
                label "Jenkins_agent"
            }
        }
        environment{
            pacakageVersion = ''
            nexusUrl = '172.31.95.119:8081'
        }
        options{
            disableConcurrentBuilds()
            ansiColor('xterm')
        }
        stages{
            stage('getting Version of the application'){
                steps{
                    script {

                        def packageJson = readJSON file: 'package.json'
                        pacakageVersion = packageJson.version
                        echo "the version of the application: $pacakageVersion"
                    }
                }
            }
            stage('installing dependences'){
                steps{
                    sh """
                        npm install
                    """
                }
            }
            stage('unit testing'){
                steps{
                    sh """
                        echo "unit test will run here"
                    """
                }
            }
            stage('sonar-scan'){
                steps{
                    sh """
                        sonar-scanner
                    """
                }
            }
            stage('Build'){
                steps{
                    sh """
                        zip -q -r catalogue-${pacakageVersion}.zip ./* -x ".git" -x ".sonar-project.properties"
                        ls -la
                    """
                }
            }
            stage('nexus artificat uploader'){
                steps{
                    nexusArtifactUploader(
                        nexusVersion: 'nexus3',
                        protocol: 'http',
                        nexusUrl: "${nexusUrl}",
                        groupId: 'com.roboshop',
                        version: "${pacakageVersion}",
                        repository: "catalogue",
                        credentialsId: 'nexus-auth',
                        artifacts: [
                            [artifactId: 'catalogue',
                            classifier: '',
                            file: 'catalogue-1.0.0.zip',
                            type: 'zip']
                        ]
                    )
                }
            }
        }
        post{
            always{
                echo "Pipeline is running"
            }
            failure{
                echo "pipeline is failed"
            }
            success{
                echo "pipeline is successfull"
            }
        }
    }
}
