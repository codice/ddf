//"Jenkins Pipeline is a suite of plugins which supports implementing and integrating continuous delivery pipelines into Jenkins. Pipeline provides an extensible set of tools for modeling delivery pipelines "as code" via the Pipeline DSL."
//More information can be found on the Jenkins Documentation page https://jenkins.io/doc/
pipeline {
    agent {
        node {
            label 'linux-large'
            customWorkspace "/jenkins/workspace/${JOB_NAME}/${BUILD_NUMBER}"
        }
    }
    options {
        buildDiscarder(logRotator(numToKeepStr:'25'))
        disableConcurrentBuilds()
        timestamps()
        skipDefaultCheckout()
    }
    triggers {
        /*
          Restrict nightly builds to master branch, all others will be built on change only.
          Note: The BRANCH_NAME will only work with a multi-branch job using the github-branch-source
        */
        cron(BRANCH_NAME == "master" ? "H H(17-19) * * *" : "")
    }
    environment {
        DOCS = 'distribution/docs'
        ITESTS = 'distribution/test/itests/test-itests-ddf'
        POMFIX = 'libs/libs-pomfix,libs/libs-pomfix-run'
        LARGE_MVN_OPTS = '-Xmx8192M -Xss128M -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC '
        DISABLE_DOWNLOAD_PROGRESS_OPTS = '-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn '
        LINUX_MVN_RANDOM = '-Djava.security.egd=file:/dev/./urandom'
        COVERAGE_EXCLUSIONS = '**/test/**/*,**/itests/**/*,**/*Test*,**/sdk/**/*,**/*.js,**/node_modules/**/*,**/jaxb/**/*,**/wsdl/**/*,**/nces/sws/**/*,**/*.adoc,**/*.txt,**/*.xml'
    }
    stages {
        stage('Setup') {
            steps {
                slackSend color: 'good', message: "STARTED: ${JOB_NAME} ${BUILD_NUMBER} ${BUILD_URL}"
            }
        }
        // Use the pomfix tool to validate that bundle dependencies are properly declared
        stage('Validate Poms') {
            steps {
                retry(3) {
                    checkout scm
                }
                withMaven(maven: 'M3', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LINUX_MVN_RANDOM}') {
                    sh 'mvn clean install -DskipStatic=true -DskipTests=true -B -pl $POMFIX $DISABLE_DOWNLOAD_PROGRESS_OPTS'
                }
            }
        }
        // The incremental build will be triggered only for PRs. It will build the differences between the PR and the target branch
        stage('Incremental Build') {
            when {
                allOf {
                    expression { env.CHANGE_ID != null }
                    expression { env.CHANGE_TARGET != null }
                }
            }
            parallel {
                stage ('Linux') {
                    steps {
                        timeout(time: 3, unit: 'HOURS') {
                            // TODO: Maven downgraded to work around a linux build issue. Falling back to system java to work around a linux build issue. re-investigate upgrading later
                            withMaven(maven: 'Maven 3.3.9', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS} ${LINUX_MVN_RANDOM}', options: [artifactsPublisher(disabled: true), dependenciesFingerprintPublisher(disabled: true, includeScopeCompile: false, includeScopeProvided: false, includeScopeRuntime: false, includeSnapshotVersions: false)]) {
                                sh '''
                                    unset JAVA_TOOL_OPTIONS
                                    mvn install -B -DskipStatic=true -DskipTests=true $DISABLE_DOWNLOAD_PROGRESS_OPTS
                                    mvn clean install -B -pl !$ITESTS -Dgib.enabled=true -Dgib.referenceBranch=/refs/remotes/origin/$CHANGE_TARGET $DISABLE_DOWNLOAD_PROGRESS_OPTS
                                    mvn install -B -pl $ITESTS -nsu $DISABLE_DOWNLOAD_PROGRESS_OPTS
                                '''
                            }
                        }
                    }
                }
                //Commenting out Windows builds until pipeline issues can be resolved
                /*stage ('Windows') {
                    agent { label 'server-2016-large' }
                    steps {
                        retry(3) {
                            checkout scm
                        }
                        timeout(time: 3, unit: 'HOURS') {
                            withMaven(maven: 'M35', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS}', options: [artifactsPublisher(disabled: true), dependenciesFingerprintPublisher(disabled: true, includeScopeCompile: false, includeScopeProvided: false, includeScopeRuntime: false, includeSnapshotVersions: false)]) {
                                bat 'mvn install -B -DskipStatic=true -DskipTests=true %DISABLE_DOWNLOAD_PROGRESS_OPTS%'
                                bat 'mvn clean install -B -pl !%ITESTS% -Dgib.enabled=true -Dgib.referenceBranch=/refs/remotes/origin/%CHANGE_TARGET% %DISABLE_DOWNLOAD_PROGRESS_OPTS%'
                                bat 'mvn install -B -pl %ITESTS% -nsu %DISABLE_DOWNLOAD_PROGRESS_OPTS%'
                            }
                        }
                    }
                }*/
            }
        }
        stage('Full Build Except Itests') {
            when { expression { env.CHANGE_ID == null } }
            parallel {
                stage ('Linux') {
                    steps {
                        // This timeout was reduced from 3 hours after this stage was modified to no longer run the itests. In the future, we may want to reduce it further if there is confidence that the build will finish faster.
                        timeout(time: 2, unit: 'HOURS') {
                            // TODO: Maven downgraded to work around a linux build issue. Falling back to system java to work around a linux build issue. re-investigate upgrading later
                            withMaven(maven: 'Maven 3.3.9', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS} ${LINUX_MVN_RANDOM}') {
                                sh 'mvn clean install -B -pl !$ITESTS $DISABLE_DOWNLOAD_PROGRESS_OPTS'
                            }
                        }
                    }
                }
                //Commenting out Windows builds until pipeline issues can be resolved
                /*stage ('Windows') {
                    agent { label 'server-2016-large'}
                    steps {
                        retry(3) {
                            checkout scm
                        }
                        timeout(time: 2, unit: 'HOURS') {
                            withMaven(maven: 'M35', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS}') {
                                bat 'mvn clean install -B -pl !%ITESTS% %DISABLE_DOWNLOAD_PROGRESS_OPTS%'
                            }
                        }
                    }
                }*/
            }
        }
        stage('Integration Tests Only Build') {
            when { expression { env.CHANGE_ID == null } }
            parallel {
                stage ('Linux') {
                    steps {
                        timeout(time: 2, unit: 'HOURS') {
                            // TODO: Maven downgraded to work around a linux build issue. Falling back to system java to work around a linux build issue. re-investigate upgrading later
                            withMaven(maven: 'Maven 3.3.9', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS} ${LINUX_MVN_RANDOM}') {
                                sh '''
                                    unset JAVA_TOOL_OPTIONS
                                    mvn install -B -pl $ITESTS -nsu $DISABLE_DOWNLOAD_PROGRESS_OPTS
                                '''
                            }
                        }
                    }
                }
                //Commenting out Windows builds until pipeline issues can be resolved
                /*stage ('Windows') {
                    agent { label 'server-2016-large'}
                    steps {
                        retry(3) {
                            checkout scm
                        }
                        timeout(time: 2, unit: 'HOURS') {
                            withMaven(maven: 'M35', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS}') {
                                bat 'mvn clean install -B -DskipStatic=true -DskipTests=true'
                                bat 'mvn install -B -pl %ITESTS% -nsu %DISABLE_DOWNLOAD_PROGRESS_OPTS%'
                            }
                        }
                    }
                }*/
            }
        }
        stage('Security Analysis') {
            parallel {
                stage ('Owasp') {
                    steps {
                        withMaven(maven: 'M35', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS} ${LINUX_MVN_RANDOM}') {
                            script {
                                // If this build is not a pull request, run owasp scan on the distribution. Otherwise run incremental scan
                                if (env.CHANGE_ID == null) {
                                    sh 'mvn verify -q -B -Powasp-dist -DskipTests=true -DskipStatic=true -pl !$DOCS $DISABLE_DOWNLOAD_PROGRESS_OPTS'
                                } else {
                                    sh 'mvn verify -q -B -Powasp -DskipTests=true -DskipStatic=true -pl !$DOCS -Dgib.enabled=true -Dgib.referenceBranch=/refs/remotes/origin/$CHANGE_TARGET $DISABLE_DOWNLOAD_PROGRESS_OPTS'
                                }
                            }
                        }
                    }
                }
                stage ('NodeJsSecurity') {
                    agent { label 'linux-small' }
                    steps {
                        retry(3) {
                            checkout scm
                        }
                        script {
                            def packageFiles = findFiles(glob: '**/package.json')
                            for (int i = 0; i < packageFiles.size(); i++) {
                                dir(packageFiles[i].path.split('package.json')[0]) {
                                    def packageFile = readJSON file: 'package.json'
                                    if (packageFile.scripts =~ /.*webpack.*/ || packageFile.containsKey("browserify")) {
                                        nodejs(configId: 'npmrc-default', nodeJSInstallationName: 'nodejs') {
                                            echo "Scanning ${packageFiles[i].path}"
                                            sh 'nsp check'
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        /*
          Deploy stage will only be executed for deployable branches. These include master and any patch branch matching M.m.x format (i.e. 2.10.x, 2.9.x, etc...).
          It will also only deploy in the presence of an environment variable JENKINS_ENV = 'prod'. This can be passed in globally from the jenkins master node settings.
        */
        stage('Deploy') {
            when {
                allOf {
                    expression { env.CHANGE_ID == null }
                    expression { env.BRANCH_NAME ==~ /((?:\d*\.)?\d*\.x|master)/ }
                    environment name: 'JENKINS_ENV', value: 'prod'
                }
            }
            steps{
                withMaven(maven: 'M3', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LINUX_MVN_RANDOM}') {
                    sh 'mvn deploy -B -DskipStatic=true -DskipTests=true -DretryFailedDeploymentCount=10 $DISABLE_DOWNLOAD_PROGRESS_OPTS'
                }
            }
        }
        stage('Quality Analysis') {
            parallel {
                stage ('SonarCloud') {
                    steps {
                        withMaven(maven: 'M35', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS} ${LINUX_MVN_RANDOM}') {
                            withCredentials([string(credentialsId: 'SonarQubeGithubToken', variable: 'SONARQUBE_GITHUB_TOKEN'), string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                                script {
                                    // If this build is not a pull request, run sonar scan. otherwise run incremental scan
                                    if (env.CHANGE_ID == null) {
                                        sh 'mvn -q -B -Dcheckstyle.skip=true org.jacoco:jacoco-maven-plugin:prepare-agent install sonar:sonar -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=$SONAR_TOKEN  -Dsonar.organization=codice -Dsonar.projectKey=ddf -Dsonar.exclusions=${COVERAGE_EXCLUSIONS} -pl !$DOCS,!$ITESTS $DISABLE_DOWNLOAD_PROGRESS_OPTS'
                                    } else {
                                        sh 'mvn -q -B -Dcheckstyle.skip=true org.jacoco:jacoco-maven-plugin:prepare-agent install sonar:sonar -Dsonar.github.pullRequest=${CHANGE_ID} -Dsonar.github.oauth=${SONARQUBE_GITHUB_TOKEN} -Dsonar.analysis.mode=preview -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=$SONAR_TOKEN -Dsonar.organization=codice -Dsonar.projectKey=ddf -Dsonar.exclusions=${COVERAGE_EXCLUSIONS} -pl !$DOCS,!$ITESTS -Dgib.enabled=true -Dgib.referenceBranch=/refs/remotes/origin/$CHANGE_TARGET $DISABLE_DOWNLOAD_PROGRESS_OPTS'
                                    }
                                }
                            }
                        }
                    }
                }
                stage ('Codecov') {
                    agent { label 'linux-medium' }
                    steps {
                        retry(3) {
                            checkout scm
                        }
                        withMaven(maven: 'M35', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS} ${LINUX_MVN_RANDOM}') {
                            withCredentials([string(credentialsId: 'DDF_CodeCov', variable: 'DDF_CODECOV_TOKEN')]) {
                                sh 'mvn clean install -B -pl !$ITESTS $DISABLE_DOWNLOAD_PROGRESS_OPTS'
                                sh 'curl -s https://codecov.io/bash | bash -s - -t ${DDF_CODECOV_TOKEN}'
                            }
                        }
                    }
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
        cleanup {
            echo '...Cleaning up workspace'
            cleanWs()
            sh 'rm -rf ~/.m2/repository'
            wrap([$class: 'MesosSingleUseSlave']) {
                sh 'echo "...Shutting down Jenkins slave: `hostname`"'
            }
        }
    }
}
