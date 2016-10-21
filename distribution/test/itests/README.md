# How to Debug Integration Tests

## Remote Debugging
Use the `isDebugEnabled` property to force the integration test to pause during startup and wait for a debugger to connect to port 5005.

```
mvn clean test -DisDebugEnabled=true
```

## SSH Into a Running Instance
It is possible to SSH into a running test instance.  This will allow you to use the shell to inspect the runtime state while the test probe is installed and running.

```
ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -p 9101 admin@localhost
```

## Debug a Single Test
The Pax Exam tests support Maven Surefire Plugin properties.  One useful property is the `test` property to select a single test class or method to execute.

```
mvn clean test -Dtest=TestFederation
mvn clean test -Dtest=TestFederation#<testMethodName>
```

This can be combined with the `isDebugEnabled` property.

## Investigating the Test Container
Use the `keepRuntimeFolder` property to keep the test container for test failure investigation.

```
mvn clean test -DkeepRuntimeFolder=true
```

The runtime folder used during the test will be available under `target/exam/<GUID>`.  It is possible to rerun the instance and verify that all bundles (excluding the test probe) are installed and working properly.  You can also inspect the logs under `target/exam/<GUID>/data/logs`.

## Adjusting the Log Level
By default, itests are run at a log level of `warn` for increased performance. If you want to change the logging level, use the flag `-DitestLoggingLevel=<level>`. Valid levels are defined by the [SLF4J API](http://www.slf4j.org/api/org/apache/commons/logging/Log.html).
