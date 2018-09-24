@ECHO OFF
SETLOCAL enabledelayedexpansion
SET COMMAND=%1
SET DIRNAME=%~dp0%
PUSHD %DIRNAME%..
SET DDF_HOME=%CD%
SET GET_PROPERTY=%DIRNAME%get_property.bat
SET SOLR_EXEC=%DDF_HOME%\solr\bin\solr.cmd
CALL %GET_PROPERTY% solr.http.port
CALL %GET_PROPERTY% solr.http.protocol
CALL %GET_PROPERTY% solr.mem 2g

IF NOT "!solr.http.protocol!"=="http" IF NOT "!solr.http.protocol!"=="https" (
    ECHO Unkown Solr protocol %solr.http.protocol% found in custom.system.properties file
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

    REM Require two-way TLS. Change this value to false to disable client authentication.
    CALL SET SOLR_SSL_NEED_CLIENT_AUTH=true

    REM Not used. From Solr 7.4 docs:
    REM    Enable either SOLR_SSL_NEED_CLIENT_AUTH or SOLR_SSL_WANT_CLIENT_AUTH but not both at
    REM    the same time. They are mutually exclusive and Jetty will select one of them which
    REM    may not be what you expect.
    CALL SET SOLR_SSL_WANT_CLIENT_AUTH=false
)

IF "%COMMAND%"=="" ECHO Missing command. Use start, restart, stop.

IF "%COMMAND%"=="start" (
    IF "!solr.http.protocol!"=="http" ECHO **** USING INSECURE SOLR CONFIGURATION ****
    CALL %SOLR_EXEC% start -p !solr.http.port! -m !solr.mem!
)

IF "%COMMAND%"=="restart" (
    IF "!solr.http.protocol!"=="http" ECHO **** USING INSECURE SOLR CONFIGURATION ****
    CALL %SOLR_EXEC% restart -p !solr.http.port! -m !solr.mem!
)

IF "%COMMAND%"=="stop" CALL %SOLR_EXEC% stop -p !solr.http.port!

EXIT /B

ENDLOCAL
