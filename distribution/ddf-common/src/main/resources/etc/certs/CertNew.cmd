REM Usage: CertNew some.computer.com
REM
REM Creates a certificate with subject some.computer.com, 
REM signs the certificate as the DDF Demo CA
REM and install the certificate in the server keystore.
REM
REM NOTE: Execute from the <DDF_HOME>/etc/certs directory.
REM NOTE: Defaults to Java Keystore file type.

@echo off

REM This next line captures the output of the dir command and stores it in a variable
for /f "delims=" %%a in ('dir /S /B ..\..\security-certificate-generator*.jar') do @set JARFILE=%%a

set PASSWORD="changeit"
set KEYFILE=../keystores/serverKeystore.jks
set KEYTYPE=JKS

CALL java -Djavax.net.ssl.keyStore=%KEYFILE% -Djavax.net.ssl.keyStorePassword=%PASSWORD% -Djavax.net.ssl.keyStoreType=%KEYTYPE% -jar %JARFILE% %1%

echo Finished generating certificate for %1%
@echo on