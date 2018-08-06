@ECHO off
SETLOCAL enabledelayedexpansion
SET ARGS=%*
SET DIRNAME=%~dp0%

PUSHD %DIRNAME%..
SET DDF_HOME=%CD%
POPD

SET GET_PROPERTY=%DIRNAME%get_property.bat
SET SOLR_EXEC=%DDF_HOME%\solr\bin\solr.cmd

:restart
REM Remove the restart file indicator so we can detect later if restart was requested
IF EXIST "%DIRNAME%restart.jvm" DEL "%DIRNAME%restart.jvm"

REM Get Solr start property
CALL %GET_PROPERTY% start.solr true

REM Get Karaf start property
CALL %GET_PROPERTY% start.ddf true

REM Start Solr if needed
IF "%start.solr%" == "true" (
	CALL %GET_PROPERTY% solr.http.port 8994
	CALL %GET_PROPERTY% solr.http.protocol https

	IF NOT "!solr.http.protocol!"=="http" IF NOT "!solr.http.protocol!"=="https" (
		ECHO Unkown Solr protocol %solr.http.protocol% found in system.properties file
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

	CALL %SOLR_EXEC% start -p !solr.http.port!

	IF "!solr.http.protocol!"=="http" ECHO **** USING INSECURE SOLR CONFIGURATION ****
	IF "!solr.http.protocol!"=="https" ECHO Using Solr secure configuration
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
  CALL %DDF_HOME%/solr/bin/solr.cmd stop -p %SOLR_PORT%
)
GOTO :EOF
