pipeline {
    agent none
    environment {
        projectName = 'DDF'
    }
    stages {
        stage('Setup') {
            steps{
                notifySlack('good', "STARTED: ${JOB_NAME} ${BUILD_NUMBER} ${BUILD_URL}")
            }
        }
        stage('Parallel Build') {
            //TODO Parallel Stage Syntax may soon be changing for declaritive Jenkinsfiles - https://issues.jenkins-ci.org/browse/JENKINS-41334
            steps{
                parallel(
                    linux: {
                        node('linux-small') {
                            timeout(time: 3, unit: 'HOURS') {
                                checkout scm
                                withMaven(maven: 'M3', mavenSettingsConfig: 'codice-maven-settings') {
                                    sh 'mvn clean install'
                                }
                            }
                        }
                    }, windows: {
                        node('proxmox-windows'){
                            timeout(time: 3, unit: 'HOURS') {
                                checkout scm
                                withMaven(maven: 'M3', mavenSettingsConfig: 'codice-maven-settings') {
                                    bat 'mvn clean install'
                                }
                            }
                        }
                    }
                )
            }
        }
        stage('Static Analysis') {
            agent { label 'linux-small' }
            steps{
                timeout(time: 2, unit: 'MINUTES') {
                    sh """echo Static Analysis run of "$projectName" """
                    //TODO 6147 Add SA tools
                }
            }
            post {
                failure {
                    sh """Static Analysis failure"""
                }
            }
        }
        stage('Deploy') {
            agent { label 'linux-small' }
            //TODO 6151 Decide on conditional logic
            when {
                //TODO flow control for release/CICD
                expression { environment.JOB_NAME == 'CICD' }
            }
            steps{
                timeout(time: 2, unit: 'MINUTES') {
                    sh """echo Deploy the artifacts of "$projectName" """
                }
            }
        }
    }
    post {
        success {
            notifySlack('good', "SUCCESS: ${JOB_NAME} ${BUILD_NUMBER}")
        }
        failure {
            notifySlack('#ea0017', "FAILURE: ${JOB_NAME} ${BUILD_NUMBER}. See the results here: ${BUILD_URL}")
        }
        unstable {
            notifySlack('#ffb600', "UNSTABLE: ${JOB_NAME} ${BUILD_NUMBER}. See the results here: ${BUILD_URL}")
        }
    }
}

def notifySlack(String color, String message) {
    slackSend color: color, message: message
    //TODO Add email hooks
}