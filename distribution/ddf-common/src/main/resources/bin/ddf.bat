@ECHO off
SETLOCAL enabledelayedexpansion
SET ARGS=%*
SET DIRNAME=%~dp0%

PUSHD %DIRNAME%..
SET DDF_HOME=%CD%
POPD

SET GET_PROPERTY=%DIRNAME%get_property.bat
SET SOLR_EXEC=%DDF_HOME%\bin\ddfsolr.bat

:RESTART
REM Remove the restart file indicator so we can detect later if restart was requested
IF EXIST "%DIRNAME%restart.jvm" DEL "%DIRNAME%restart.jvm"

REM Get Solr start property
CALL %GET_PROPERTY% start.solr

REM Get Karaf start property
CALL %GET_PROPERTY% start.ddf

REM Start Solr if needed
IF "%start.solr%" == "true" (
    CALL %SOLR_EXEC% restart
)

REM Actually invoke ddf to gain restart support
IF "%start.ddf%" == "true" CALL "%DIRNAME%karaf.bat" %ARGS%

REM Check if restart was requested by ddf_on_error.bat
IF EXIST "%DIRNAME%restart.jvm" (
    ECHO Restarting JVM...
    CALL :STOP_SOLR
    GOTO :RESTART
) ELSE (
    CALL :STOP_SOLR
    EXIT /B %RC%
)

EXIT /B

:STOP_SOLR
IF "%SOLR_CLIENT%" == "HttpSolrClient" (
  ECHO Stopping Solr process on port %SOLR_PORT%
  CALL %SOLR_EXEC% stop
)
GOTO :EOF
