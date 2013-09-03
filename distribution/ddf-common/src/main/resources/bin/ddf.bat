@echo off

set KARAF_TITLE=${command.prompt.title}

set DDF_HOME=%~dp0%..

set JAVA_OPTS=-server -XX:PermSize=128m -XX:MaxPermSize=512m -Xmx2048M -Dderby.system.home="..\data\derby"  -Dderby.storage.fileSyncTransactionLog=true -Dcom.sun.management.jmxremote -Dfile.encoding=UTF8 -Dddf.home=%DDF_HOME%
:: set JAVA_OPTS=-server -XX:PermSize=128m -XX:MaxPermSize=512m -Xmx2048M -Dfile.encoding=UTF8 -Djavax.net.ssl.keyStore=../etc/keystores/serverKeystore.jks -Djavax.net.ssl.keyStorePassword=changeit -Djavax.net.ssl.trustStore=../etc/keystores/serverTruststore.jks -Djavax.net.ssl.trustStorePassword=changeit -Dddf.home=%DDF_HOME%

call "%~dp0%"karaf.bat