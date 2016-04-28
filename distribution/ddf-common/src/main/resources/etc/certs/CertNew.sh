#! /bin/bash 
#
# Usage:
#   CertNew.sh
#   CertNew.sh -cn <common-name>
#   CertNew.sh -dn <distinguished name>
#
# Create new certificate and certificate chain signed by Demo Certificate Authority.  
# The new certificate chain and private key are installed in the keystore.
# The alias will be the same as the common name.
# The localhost key will be deleted from the keystore.
# If no arguments specified on the command line, `hostname -f` is used as the
# the  the common-name for the certificate.

#Assume script is in <DDF>/etc/certs/ directory
cd `dirname $0`

#Set password, keystore location and other variables
PASSWORD="changeit"
KEYFILE="../keystores/serverKeystore.jks"
KEYTYPE="JKS"
JARPATTERN=security-certificate-generator*.jar

if [[ ! -e $KEYFILE ]]; then
    echo "Could not find $KEYFILE. Exiting."
    exit 1
fi
echo "Found keystore file at $KEYFILE"

JARFILE=$(find ../.. -name $JARPATTERN)

if [[ -z $JARFILE ]]; then
  echo "Could not find JAR file matching $JARPATTERN"
  exit 2
fi

if [[ $1 && $2 ]]; then
    PARAM1="$1"
    PARAM2="$2"
else
    PARAM1="-cn"
    PARAM2="$(hostname -f)"
fi

echo "--IGNORE SLF4J ERRORS"--
$(java -Djavax.net.ssl.keyStore="$KEYFILE" -Djavax.net.ssl.keyStorePassword="$PASSWORD" -Djavax.net.ssl.keyStoreType="$KEYTYPE" -jar "$JARFILE" "$PARAM1" "$PARAM2")


if [[ $? == 0 ]]; then
    echo "---SUCCESS---"
    KEYSTORECONTENTS=$(keytool -list -keystore "$KEYFILE" -storepass "$PASSWORD" -storetype "$KEYTYPE")
    printf "%s" "$KEYSTORECONTENTS"
fi
