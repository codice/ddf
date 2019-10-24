# Writing Integration Tests

## Cleaning up after each test
There are several operations a test can perform that will permanently change the state of the running container. **It's the responsibility of the tests themselves to clean up after performing these state-changing operations.**
The most common state-changing operations and the best way to clean up after them are as follows:

* **Creating or updating a configuration:** Save the returned Config properties to a static field on the itest class and then update the configuration with those properties in the `AfterExam`.
* **Creating or updating a Managed Service:** Save off the pid of the managed service, and then stop the Managed Service using that.
* **Ingesting or updating a record:** Resetting or deleting the record
* **Starting or stopping a configuration:** Reset it by starting or stopping the configuration.

# How to Debug Integration Tests

## Remote Debugging
Use the `isDebugEnabled` property to force the integration test to pause during startup and wait for a debugger to connect to port 5005.

```
mvn clean verify -DisDebugEnabled=true
```

## Debugging Pax Exam Setup
Pax Exam uses two JVMs: the first is used to perform Pax Exam configuration, and the second is launched by Pax Exam to perform the actual integration tests. This second JVM is the one you connect to on port 5005.

As a result, if you want to debug code that is run during Pax Exam setup itself (for example, inside the `@org.ops4j.pax.exam.Configuration` configuration method), you need to startup the tests in your IDE and use a local debugger to catch Pax Exam before it spawns the second JVM process.

In short:
* Use remote debugging to debug tests themselves or anything related to production code
* Use local debugging to debug the code that sets up and configures the testing environment

## Solr
Solr runs in its own OS process separate from PAX-EXAM and the application. Things to know include:

* The default port for Solr is hardcoded to `9994`. This value can be changed if needed in the test-itest-ddf pom under the solr.port property.
* The Solr administration UI is available at `http://localhost:9994/solr`. Use the default 
username (`admin`)and default password (`admin`)to log into the Solr administration UI.
* The Maven exec-maven-plugin executes the Solr start command before the itests begin. The plugin also executes the Solr stop command when the itests have terminated.
* The files for the Solr instance are located inside of the `test-itest-ddf/target/solr` folder and **not** in the exam folder. There is not a per exam-folder copy of Solr. Therefore it is not
 recommended to run itests concurrently because the tests may corrupt each other's data.
* Because not every itest cleans up after itself, it is possible to leak data between separate runs of the itests, **if** the test process terminated abnormally. In such cases, use `mvn clean` to delete the target folder. This ensures future itests have a clean Solr installation.

## SSH Into a Running Instance
It is possible to SSH into a running test instance. This will allow you to use the shell to inspect the runtime state while the test probe is installed and running. The SSH port is dynamic and can be found in `target/exam/<GUID>/etc/org.apache.karaf.shell.cfg`.

```
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p 20003 admin@localhost
```

## Running Tests
The Pax Exam tests support Maven Surefire Plugin properties. One useful property is the `it.test` property to select a single test class or method to execute.

```
mvn clean verify -Dit.test=TestFederation
mvn clean verify -Dit.test=TestFederation#<testMethodName>
```

Multiple test classes can also be executed sequentially using comma separation.

```
mvn clean verify -Dit.test=TestFederation,TestSecurity
```

This can be combined with the `isDebugEnabled` property.

## Investigating the Test Container
Use the `keepRuntimeFolder` property to keep the test container for test failure investigation.

```
mvn clean verify -DkeepRuntimeFolder=true
```

The runtime folder used during the test will be available under `target/exam/<GUID>`. It is possible to rerun the instance and verify that all bundles (excluding the test probe) are installed and working properly. You can also inspect the logs under `target/exam/<GUID>/data/logs`.

## Adjusting the Log Level
By default, itests are run at a log level of `WARN` for increased performance.
If you want to change the logging level, use any combination of the below flags:
* `-DglobalLogLevel=<level>` affects the root logger (all packages)
* `-DitestLogLevel=<level>` affects the packages `ddf` and `org.codice`
* `-DsecurityLogLevel=<level>` affects the packages `ddf.security.expansion.impl.RegexExpansion` and `ddf.security.service.impl.AbstractAuthorizingRealm`

Valid levels are defined by the [SLF4J API](http://www.slf4j.org/api/org/apache/commons/logging/Log.html).

## Run unstable tests
By default, all tests that include a call to `unstableTest` will not be run. To include them as part of a build, add the `-DincludeUnstableTests=true` property to the Maven command.

## Generate missing security permissions
When adding new functionality that requires security permissions that have
not been added to `security/default.policy`, running with the `DgeneratePolicyFile=true`
flag and the `-DkeepRuntimeFolder=true` flag will generate the missing
policies in the `generated.policy` file in the exam folder.