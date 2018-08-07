@ECHO off
SETLOCAL enabledelayedexpansion
SET ARGS=%*
SET DIRNAME=%~dp0%

PUSHD %DIRNAME%..
SET DDF_HOME=%CD%
POPD

SET GET_PROPERTY=%DIRNAME%get_property.bat
SET SOLR_EXEC=%DDF_HOME%\bin\ddfsolr.bat

:restart
REM Remove the restart file indicator so we can detect later if restart was requested
IF EXIST "%DIRNAME%restart.jvm" DEL "%DIRNAME%restart.jvm"

REM Get Solr start property
CALL %GET_PROPERTY% start.solr true

REM Get Karaf start property
CALL %GET_PROPERTY% start.ddf true

REM Start Solr if needed
IF "%start.solr%" == "true" (
	CALL %SOLR_EXEC% start -p !solr.http.port!
)

REM Actually invoke ddf to gain restart support
IF "%start.ddf%" == "true" CALL "%DIRNAME%karaf.bat" %ARGS%

REM Check if restart was requested by ddf_on_error.bat
IF EXIST "%DIRNAME%restart.jvm" (
    ECHO Restarting JVM...
    IF "%start.solr%" == "true" CALL %SOLR_EXEC% stop
    GOTO :restart
) ELSE (
    IF "%start.solr%" == "true" CALL %SOLR_EXEC% stop
    EXIT /B
)
ENDLOCAL
