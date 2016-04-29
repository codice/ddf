@ECHO OFF
set DIRNAME=%~dp0%
set DDF_CLI_HOME=%DIRNAME%..
set JAVA=%JAVA_HOME%\bin\java

ECHO.
%JAVA% -Dddf.cli.home=%DDF_CLI_HOME% -jar %DDF_CLI_HOME%/lib/java-cli-example-jar-with-dependencies.jar %*