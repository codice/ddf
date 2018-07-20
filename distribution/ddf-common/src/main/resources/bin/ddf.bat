@ECHO off
SETLOCAL

SET ARGS=%*
SET DIRNAME=%~dp0%

PUSHD %DIRNAME%\..
SET DDF_HOME=%CD%
POPD

:RESTART
REM Remove the restart file indicator so we can detect later if restart was requested
IF EXIST "%DIRNAME%\restart.jvm" (
  DEL "%DIRNAME%\restart.jvm"
)

REM Get Solr port to run on
for /f "tokens=2 delims==" %%G in ('findstr /i "^\w*solr.http.port=" %DDF_HOME%\etc\system.properties') do (
    SET SOLR_PORT=%%G
)

IF "%SOLR_PORT%" == "" (
    ECHO Property solr.http.port not found in system.properties. Exiting.
    EXIT /B
)

REM Get Solr client property
for /f "tokens=2 delims==" %%G in ('findstr /i "^\w*solr.client=" %DDF_HOME%\etc\system.properties') do (
    SET SOLR_CLIENT=%%G
)

IF "%SOLR_CLIENT%" == "" (
    ECHO Property solr.client not found in system.properties. Exiting.
    EXIT /B
)

IF "%SOLR_CLIENT%" == "HttpSolrClient" (
    ECHO Starting Solr on port %SOLR_PORT%
    CALL %DDF_HOME%/solr/bin/solr.cmd start -p %SOLR_PORT% -Djetty.host=127.0.0.1
    IF NOT ERRORLEVEL 0 (
        ECHO WARNING! Solr start process returned non-zero error code, please check Solr logs
    )
)

REM Actually invoke ddf to gain restart support
CALL "%DIRNAME%\karaf.bat" %ARGS%
SET RC=%ERRORLEVEL%

REM Check if restart was requested by ddf_on_error.bat
IF EXIST "%DIRNAME%\restart.jvm" (
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
  CALL %DDF_HOME%/solr/bin/solr.cmd stop -p %SOLR_PORT%
)
GOTO :EOF
