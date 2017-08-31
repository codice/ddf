//"Jenkins Pipeline is a suite of plugins which supports implementing and integrating continuous delivery pipelines into Jenkins. Pipeline provides an extensible set of tools for modeling delivery pipelines "as code" via the Pipeline DSL."
//More information can be found on the Jenkins Documentation page https://jenkins.io/doc/
pipeline {
    agent none
    options {
        buildDiscarder(logRotator(numToKeepStr:'25'))
    }
    triggers {
        cron('H H(17-19) * * *')
    }
    environment {
        DOCS = 'distribution/docs'
        ITESTS= 'distribution/test/itests/test-itests-ddf'
    }
    stages {
        stage('Setup') {
            steps{
                slackSend color: 'good', message: "STARTED: ${JOB_NAME} ${BUILD_NUMBER} ${BUILD_URL}"
            }
        }
        stage('Parallel Build') {
            // TODO DDF-2971 refactor this stage from scripted syntax to declarative syntax to match the rest of the stages - https://issues.jenkins-ci.org/browse/JENKINS-41334
            steps{
                parallel(
                    linux: {
                        node('linux-large') {
                            retry(3) {
                                checkout scm
                            }
                            timeout(time: 3, unit: 'HOURS') {
                                withMaven(maven: 'M35', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings') {
                                    sh 'mvn clean install -B -T 1C -pl !$ITESTS'
                                    sh 'mvn install -B -Dmaven.test.redirectTestOutputToFile=true -pl $ITESTS -nsu'
                                }
                            }
                        }
                    },
                    windows: {
                        node('proxmox-windows') {
                            retry(3) {
                                checkout scm
                            }
                            timeout(time: 3, unit: 'HOURS') {
                                withMaven(maven: 'M35', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings') {
                                    bat 'mvn clean install -B -T 1C -pl !%ITESTS%'
                                    bat 'mvn install -B -Dmaven.test.redirectTestOutputToFile=true -pl %ITESTS% -nsu'
                                }
                            }
                        }
                    }
                )
            }
        }
        stage('Static Analysis') {
            steps {
                parallel(
                        owasp: {
                            node('linux-large') {
                                retry(3) {
                                    checkout scm
                                }
                                withMaven(maven: 'M35', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings') {
                                    sh 'mvn install -q -B -Powasp -DskipTests=true -DskipStatic=true -pl !$DOCS'
                                }
                            }
                        },
                        sonarqube: {
                            node('linux-large') {
                                retry(3) {
                                    checkout scm
                                }
                                withMaven(maven: 'M35', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings') {
                                    withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                                        sh 'mvn -q -B -Dfindbugs.skip=true -Dcheckstyle.skip=true org.jacoco:jacoco-maven-plugin:prepare-agent install sonar:sonar -Dsonar.host.url=https://sonarqube.com -Dsonar.login=$SONAR_TOKEN  -Dsonar.organization=codice -Dsonar.projectKey=ddf -pl !$DOCS,!$ITESTS'
                                    }
                                }
                            }
                        },
                        coverity: {
                            node('linux-medium') {
                                retry(3) {
                                    checkout scm
                                }
                                withMaven(maven: 'M35', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings') {
                                    withCredentials([string(credentialsId: 'ddf-coverity-token', variable: 'COVERITY_TOKEN')]) {
                                        withEnv(["PATH=${tool 'coverity-linux'}/bin:${env.PATH}"]) {
                                            configFileProvider([configFile(fileId: 'coverity-maven-settings', replaceTokens: true, variable: 'MAVEN_SETTINGS')]) {
                                                echo sh(returnStdout: true, script: 'env')
                                                sh 'cov-build --dir cov-int mvn -DskipTests=true -DskipStatic=true install -pl !$DOCS --settings $MAVEN_SETTINGS'
                                                sh 'tar czvf ddf.tgz cov-int'
                                                sh 'curl --form token=$COVERITY_TOKEN --form email=cmp-security-team@connexta.com --form file=@ddf.tgz --form version="master" --form description="Description: DDF CI Build" https://scan.coverity.com/builds?project=codice%2Fddf'
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        nodeJsSecurity: {
                            node('linux-small') {
                                retry(3) {
                                    checkout scm
                                }
                                script {
                                    def packageFiles = findFiles(glob: '**/package.json')
                                    for (int i = 0; i < packageFiles.size(); i++) {
                                        dir(packageFiles[i].path.split('package.json')[0]) {
                                            echo "Scanning ${packageFiles[i].name}"
                                            nodejs(configId: 'npmrc-default', nodeJSInstallationName: 'nodejs') {
                                                sh 'nsp check'
                                            }
                                        }
                                    }
                                }
                            }
                        }
                )
            }
        }
        stage('Deploy') {
            agent { label 'linux-small' }
            steps{
                withMaven(maven: 'M3', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings') {
                    retry(3) {
                        checkout scm
                    }
                    sh 'mvn javadoc:aggregate -DskipStatic=true -DskipTests=true'
                    sh 'mvn deploy -T 1C -DskipStatic=true -DskipTests=true -DretryFailedDeploymentCount=10'
                }
            }
        }
    }
    post {
        success {
            slackSend color: 'good', message: "SUCCESS: ${JOB_NAME} ${BUILD_NUMBER}"
        }
        failure {
            slackSend color: '#ea0017', message: "FAILURE: ${JOB_NAME} ${BUILD_NUMBER}. See the results here: ${BUILD_URL}"
        }
        unstable {
            slackSend color: '#ffb600', message: "UNSTABLE: ${JOB_NAME} ${BUILD_NUMBER}. See the results here: ${BUILD_URL}"
        }
    }
}
