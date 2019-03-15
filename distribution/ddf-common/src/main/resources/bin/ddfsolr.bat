@ECHO OFF
SETLOCAL enabledelayedexpansion

SET COMMAND=%1
SET DIRNAME=%~dp0%
PUSHD %DIRNAME%..
SET DDF_HOME=%CD%
SET GET_PROPERTY=%DIRNAME%get_property.bat
SET MAKE_KEY=%DIRNAME%makekey.bat
SET SOLR_EXEC=%DDF_HOME%\solr\bin\solr.cmd
SET STOPKEY_FILE=%DIRNAME%STOPKEY
CALL %GET_PROPERTY% solr.http.port
CALL %GET_PROPERTY% solr.http.protocol
CALL %GET_PROPERTY% solr.mem 2g

IF NOT "!solr.http.protocol!"=="http" IF NOT "!solr.http.protocol!"=="https" (
    ECHO Unknown Solr protocol %solr.http.protocol% found in custom.system.properties file
    ECHO Expected 'http' or 'https' - Setting to the default https
    SET solr.http.protocol=https
)

IF "!solr.http.protocol!"=="https" (
    REM Use the same key and trust stores as DDF
    CALL %GET_PROPERTY% javax.net.ssl.keyStore etc\keystores\serverKeystore.jks
    CALL SET SOLR_SSL_KEY_STORE=%DDF_HOME%\!javax.net.ssl.keyStore!
    CALL %GET_PROPERTY% javax.net.ssl.keyStorePassword changeit
    CALL SET SOLR_SSL_KEY_STORE_PASSWORD=!javax.net.ssl.keyStorePassword!
    CALL %GET_PROPERTY% javax.net.ssl.keyStoreType jks
    CALL SET SOLR_SSL_KEY_STORE_TYPE=!javax.net.ssl.keyStoreType!
    CALL %GET_PROPERTY% javax.net.ssl.trustStore etc\keystores\serverTruststore.jks
    CALL SET SOLR_SSL_TRUST_STORE=%DDF_HOME%\!javax.net.ssl.trustStore!
    CALL %GET_PROPERTY% javax.net.ssl.trustStorePassword changeit
    CALL SET SOLR_SSL_TRUST_STORE_PASSWORD=!javax.net.ssl.trustStorePassword!
    CALL %GET_PROPERTY% javax.net.ssl.trustStoreType jks
    CALL SET SOLR_SSL_TRUST_STORE_TYPE=!javax.net.ssl.trustStoreType!
)

IF "%COMMAND%"=="" ECHO Missing command. Use start, restart, stop.

IF "%COMMAND%"=="start" (
    CALL :GEN_STOPKEY
    IF "!solr.http.protocol!"=="http" ECHO **** USING INSECURE SOLR CONFIGURATION ****
    CALL %SOLR_EXEC% start -p !solr.http.port! -m !solr.mem!
)


IF "%COMMAND%"=="stop" (
   SET /P STOP_KEY=<%STOPKEY_FILE%
   CALL %SOLR_EXEC% stop -p !solr.http.port!
   DEL %STOPKEY_FILE%
)

EXIT /B

:GEN_STOPKEY
REM Generate a random stop key for Solr and write it to a file
CALL %MAKE_KEY% > %STOPKEY_FILE%
SET /P STOP_KEY=<%STOPKEY_FILE%
EXIT /B

ENDLOCAL
