@echo off
REM Usage:
REM   CertNew.sh (-cn <cn>|-dn <dn>) [-san "<tag:name,tag:name,...>"]
REM
REM where:
REM <cn> represents a fully qualified common name (e.g. "<FQDN>", where <FQDN> could be something like cluster.yoyo.com)
REM <dn> represents a distinguished name as a comma-delimited string (e.g. "c=US, st=California, o=Yoyodyne, l=San Narciso, cn=<FQDN>")
REM <tag:name,tag:name,...> represents optional subject alternative names to be added to the generated certificate
REM    (e.g. "DNS:<FQDN>,DNS:node1.<FQDN>,DNS:node2.<FQDN>"). The format for subject alternative names
REM    is similar to the OpenSSL X509 configuration format. Supported tags are:
REM      email - email subject
REM      URI - uniformed resource identifier
REM      RID - registered id
REM      DNS - hostname
REM      IP - ip address (V4 or V6)
REM      dirName - directory name
REM
REM Create new certificate and certificate chain signed by Demo Certificate Authority.
REM The new certificate chain and private key are installed in the keystore.
REM The alias will be the same as the common name.
REM The localhost key will be deleted from the keystore.
REM Adds the specified subject alternative names if any.
REM
REM NOTE: Execute from the <DDF_HOME>/etc/certs directory.
REM NOTE: Defaults to Java Keystore file type.

REM This next line captures the output of the dir command and stores it in a variable
for /f "delims=" %%a in ('dir /S /B ..\..\security-certificate-generator*.jar') do @set JARFILE=%%a

set PASSWORD="changeit"
set KEYFILE=../keystores/serverKeystore.jks
set KEYTYPE=JKS

CALL java -Djavax.net.ssl.keyStore=%KEYFILE% -Djavax.net.ssl.keyStorePassword=%PASSWORD% -Djavax.net.ssl.keyStoreType=%KEYTYPE% -jar %JARFILE% %*

echo Finished generating certificate for %*
@echo on