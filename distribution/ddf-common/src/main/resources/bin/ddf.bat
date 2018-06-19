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

REM Get Solr managed internally property
for /f "tokens=2 delims==" %%G in ('findstr /i "^\w*solr.managed=" %DDF_HOME%\etc\system.properties') do (
    SET SOLR_MANAGED_INTERNALLY=%%G
)

REM Get Solr client property
for /f "tokens=2 delims==" %%G in ('findstr /i "^\w*solr.client=" %DDF_HOME%\etc\system.properties') do (
    SET SOLR_CLIENT=%%G
)

IF "%SOLR_MANAGED_INTERNALLY%" == "true" (
    IF NOT "%SOLR_CLIENT%" == "HttpSolrClient" (
        ECHO ERROR! solr.managed is set to true but the solr.client is not HttpSolrClient
        ECHO Please set solr.managed to false if you are not using the HttpSolrClient and
        ECHO do not want DDF to be managing the Solr instance.
        REM Exit code 83, for ascii code S, for Solr!
        EXIT 83
    )
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
    CALL %DDF_HOME%/solr/bin/solr.cmd stop -p %SOLR_PORT%
    GOTO :RESTART
) ELSE (
    ECHO Stopping Solr process on port %SOLR_PORT%
    CALL %DDF_HOME%/solr/bin/solr.cmd stop -p %SOLR_PORT%
    EXIT /B %RC%
)
