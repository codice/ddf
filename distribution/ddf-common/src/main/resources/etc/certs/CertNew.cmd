REM Usage: CertNew -cn some.computer.com
REM    or  CertNew -dn "cn=some.computer.com,o=some company,ou=some department"
REM The first usage creates a certificate with a subject/common name of some.computer.com.
REM The second usage creates a certificate with the FQDN provided and extracts the common name
REM
REM Create new certificate and certificate chain signed by Demo Certificate Authority.
REM The new certificate chain and private key are installed in the keystore.
REM The alias will be the same as the common name.
REM The localhost key will be deleted from the keystore.
REM
REM NOTE: Execute from the <DDF_HOME>/etc/certs directory.
REM NOTE: Defaults to Java Keystore file type.

@echo off

REM This next line captures the output of the dir command and stores it in a variable
for /f "delims=" %%a in ('dir /S /B ..\..\security-certificate-generator*.jar') do @set JARFILE=%%a

set PASSWORD="changeit"
set KEYFILE=../keystores/serverKeystore.jks
set KEYTYPE=JKS

CALL java -Djavax.net.ssl.keyStore=%KEYFILE% -Djavax.net.ssl.keyStorePassword=%PASSWORD% -Djavax.net.ssl.keyStoreType=%KEYTYPE% -jar %JARFILE% %*

echo Finished generating certificate for %*
@echo on