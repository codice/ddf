@ECHO off
SETLOCAL enabledelayedexpansion
SET ARGS=%*
SET DIRNAME=%~dp0%

PUSHD %DIRNAME%..
SET DDF_HOME=%CD%
POPD

SET GET_PROPERTY=%DIRNAME%get_property.bat

REM Exit if JAVA_HOME or JRE_HOME is not set
IF "%JAVA_HOME%" == "" IF "%JRE_HOME%" == ""  (
    ECHO JAVA_HOME nor JRE_HOME is set. Set JAVA_HOME or JRE_HOME to proceed - exiting.
    EXIT /B
)

:RESTART
REM Remove the restart file indicator so we can detect later if restart was requested
IF EXIST "%DIRNAME%restart.jvm" DEL "%DIRNAME%restart.jvm"

REM Invoke ddf to gain restart support
CALL "%DIRNAME%karaf.bat" %ARGS%

REM Check if a restart.jvm file was created to request a restart
IF EXIST "%DIRNAME%restart.jvm" (
    ECHO Restarting JVM...
    GOTO :RESTART
) ELSE (
    EXIT /B %RC%
)

EXIT /B

GOTO :EOF
