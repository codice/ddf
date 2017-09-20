//"Jenkins Pipeline is a suite of plugins which supports implementing and integrating continuous delivery pipelines into Jenkins. Pipeline provides an extensible set of tools for modeling delivery pipelines "as code" via the Pipeline DSL."
//More information can be found on the Jenkins Documentation page https://jenkins.io/doc/
pipeline {
    agent none
    options {
        buildDiscarder(logRotator(numToKeepStr:'25'))
        disableConcurrentBuilds()
        timestamps()
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
        LINUX_MVN_RANDOM = '-Djava.security.egd=file:/dev/./urandom'
    }
    stages {
        stage('Setup') {
            steps {
                slackSend color: 'good', message: "STARTED: ${JOB_NAME} ${BUILD_NUMBER} ${BUILD_URL}"
            }
        }
        // Use the pomfix tool to validate that bundle dependencies are properly declared
        stage('Validate Poms') {
            agent { label 'linux-small' }
            steps {
                retry(3) {
                    checkout scm
                }
                withMaven(maven: 'M3', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LINUX_MVN_RANDOM}') {
                    sh 'mvn clean install -DskipStatic=true -DskipTests=true -pl $POMFIX'
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
            // TODO DDF-2971 refactor this stage from scripted syntax to declarative syntax to match the rest of the stages - https://issues.jenkins-ci.org/browse/JENKINS-41334
            steps {
                parallel(
                        linux: {
                            node('linux-large') {
                                retry(3) {
                                    checkout scm
                                }
                                timeout(time: 3, unit: 'HOURS') {
                                    // TODO: Maven downgraded to work around a linux build issue. Falling back to system java to work around a linux build issue. re-investigate upgrading later
                                    withMaven(maven: 'Maven 3.3.9', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS} ${LINUX_MVN_RANDOM}', options: [artifactsPublisher(disabled: true), dependenciesFingerprintPublisher(disabled: true, includeScopeCompile: false, includeScopeProvided: false, includeScopeRuntime: false, includeSnapshotVersions: false)]) {
                                        sh 'mvn install -pl !$DOCS -DskipStatic=true -DskipTests=true -T 1C'
                                        sh 'mvn clean install -B -T 1C -pl !$ITESTS -Dgib.enabled=true -Dgib.referenceBranch=/refs/remotes/origin/$CHANGE_TARGET'
                                        sh 'mvn install -B -pl $ITESTS -nsu'
                                    }
                                }
                            }
                        },
                        windows: {
                            node('proxmox-windows') {
                                bat 'git config --system core.longpaths true'
                                retry(3) {
                                    checkout scm
                                }
                                timeout(time: 3, unit: 'HOURS') {
                                    withMaven(maven: 'M35', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS}', options: [artifactsPublisher(disabled: true), dependenciesFingerprintPublisher(disabled: true, includeScopeCompile: false, includeScopeProvided: false, includeScopeRuntime: false, includeSnapshotVersions: false)]) {
                                        bat 'mvn install -pl !%DOCS% -DskipStatic=true -DskipTests=true -T 1C'
                                        bat 'mvn clean install -B -T 1C -pl !%ITESTS% -Dgib.enabled=true -Dgib.referenceBranch=/refs/remotes/origin/%CHANGE_TARGET%'
                                        bat 'mvn install -B -pl %ITESTS% -nsu'
                                    }
                                }
                            }
                        }
                )
            }
        }
        // The full build will be run against all regular branches
        stage('Full Build') {
            when { expression { env.CHANGE_ID == null } }
            // TODO DDF-2971 refactor this stage from scripted syntax to declarative syntax to match the rest of the stages - https://issues.jenkins-ci.org/browse/JENKINS-41334
                steps{
                    parallel(
                            linux: {
                                node('linux-large') {
                                    retry(3) {
                                        checkout scm
                                    }
                                    timeout(time: 3, unit: 'HOURS') {
                                        // TODO: Maven downgraded to work around a linux build issue. Falling back to system java to work around a linux build issue. re-investigate upgrading later
                                        withMaven(maven: 'Maven 3.3.9', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS} ${LINUX_MVN_RANDOM}') {
                                            sh 'mvn clean install -B -T 1C -pl !$ITESTS'
                                            sh 'mvn install -B -pl $ITESTS -nsu'
                                        }
                                    }
                                }
                            },
                            windows: {
                                node('proxmox-windows') {
                                    bat 'git config --system core.longpaths true'
                                    retry(3) {
                                        checkout scm
                                    }
                                    timeout(time: 3, unit: 'HOURS') {
                                        withMaven(maven: 'M35', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS}') {
                                            bat 'mvn clean install -B -T 1C -pl !%ITESTS%'
                                            bat 'mvn install -B -pl %ITESTS% -nsu'
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
                                withMaven(maven: 'M35', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS} ${LINUX_MVN_RANDOM}') {
                                    script {
                                        // If this build is not a pull request, run full owasp scan. Otherwise run incremntal scan
                                        if (env.CHANGE_ID == null) {
                                            sh 'mvn install -q -B -Powasp -DskipTests=true -DskipStatic=true -pl !$DOCS'
                                        } else {
                                            sh 'mvn install -q -B -Powasp -DskipTests=true -DskipStatic=true -pl !$DOCS -Dgib.enabled=true -Dgib.referenceBranch=/refs/remotes/origin/$CHANGE_TARGET'
                                        }
                                    }
                                }
                            }
                        },
                        sonarqube: {
                            node('linux-large') {
                                retry(3) {
                                    checkout scm
                                }
                                withMaven(maven: 'M35', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LARGE_MVN_OPTS} ${LINUX_MVN_RANDOM}') {
                                    withCredentials([string(credentialsId: 'SonarQubeGithubToken', variable: 'SONARQUBE_GITHUB_TOKEN'), string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                                        script {
                                            // If this build is not a pull request, run sonar scan. otherwise run incremental scan
                                            if (env.CHANGE_ID == null) {
                                                sh 'mvn -q -B -Dfindbugs.skip=true -Dcheckstyle.skip=true org.jacoco:jacoco-maven-plugin:prepare-agent install sonar:sonar -Dsonar.host.url=https://sonarqube.com -Dsonar.login=$SONAR_TOKEN  -Dsonar.organization=codice -Dsonar.projectKey=ddf -pl !$DOCS,!$ITESTS'
                                            } else {
                                                sh 'mvn -q -B -Dfindbugs.skip=true -Dcheckstyle.skip=true org.jacoco:jacoco-maven-plugin:prepare-agent install sonar:sonar -Dsonar.github.pullRequest=${CHANGE_ID} -Dsonar.github.oauth=${SONARQUBE_GITHUB_TOKEN} -Dsonar.analysis.mode=preview -Dsonar.host.url=https://sonarqube.com -Dsonar.login=$SONAR_TOKEN -Dsonar.organization=codice -Dsonar.projectKey=ddf -pl !$DOCS,!$ITESTS -Dgib.enabled=true -Dgib.referenceBranch=/refs/remotes/origin/$CHANGE_TARGET'
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        // Coverity will be skipped on all PR builds
                        coverity: {
                            node('linux-medium') {
                                script {
                                    if (env.BRANCH_NAME != 'master') {
                                        echo "Coverity is only run on master"
                                    } else {
                                        retry(3) {
                                            checkout scm
                                        }
                                        withMaven(maven: 'M35', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LINUX_MVN_RANDOM}') {
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
        /*
          Deploy stage will only be executed for deployable branches. These include master and any patch branch matching M.m.x format (i.e. 2.10.x, 2.9.x, etc...).
          It will also only deploy in the presence of an environment variable JENKINS_ENV = 'prod'. This can be passed in globally from the jenkins master node settings.
        */
        stage('Deploy') {
            agent { label 'linux-small' }
            when {
                allOf {
                    expression { env.CHANGE_ID == null }
                    expression { env.BRANCH_NAME ==~ /((?:\d*\.)?\d.x|master)/ }
                    environment name: 'JENKINS_ENV', value: 'prod'
                }
            }
            steps{
                withMaven(maven: 'M3', jdk: 'jdk8-latest', globalMavenSettingsConfig: 'default-global-settings', mavenSettingsConfig: 'codice-maven-settings', mavenOpts: '${LINUX_MVN_RANDOM}') {
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
