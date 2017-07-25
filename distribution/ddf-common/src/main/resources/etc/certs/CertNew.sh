#! /bin/bash 
#
# Usage:
#   CertNew.sh [-cn <cn>|-dn <dn>] [-san <tag:name,tag:name,...>]
#
# where:
# <cn> represents a fully qualified common name (e.g. "<FQDN>", where <FQDN> could be something like cluster.yoyo.com)
# <dn> represents a distinguished name as a comma-delimited string (e.g. "c=US, st=California, o=Yoyodyne, l=San Narciso, cn=<FQDN>")
# <tag:name,tag:name,...> represents optional subject alternative names to be added to the generated certificate
#    (e.g. "DNS:<FQDN>,DNS:node1.<FQDN>,DNS:node2.<FQDN>"). The format for subject alternative names
#    is similar to the OpenSSL X509 configuration format. Supported tags are:
#      email - email subject
#      URI - uniformed resource identifier
#      RID - registered id
#      DNS - hostname
#      IP - ip address (V4 or V6)
#      dirName - directory name
#
# Create new certificate and certificate chain signed by Demo Certificate Authority.  
# The new certificate chain and private key are installed in the keystore.
# The alias will be the same as the common name.
# The localhost key will be deleted from the keystore.
# If no arguments specified on the command line, `hostname -f` is used as the
# the common-name for the certificate.
# Adds the specified subject alternative names if any.
#
# NOTE: Execute from the <DDF_HOME>/etc/certs directory.
# NOTE: Defaults to Java Keystore file type.

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
    shift
    shift
else
    PARAM1="-cn"
    PARAM2="$(hostname -f)"
fi
echo "--IGNORE SLF4J ERRORS"--
$(java -Djavax.net.ssl.keyStore="$KEYFILE" -Djavax.net.ssl.keyStorePassword="$PASSWORD" -Djavax.net.ssl.keyStoreType="$KEYTYPE" -jar "$JARFILE" "$PARAM1" "$PARAM2" "$@")


if [[ $? == 0 ]]; then
    echo "---SUCCESS---"
    KEYSTORECONTENTS=$(keytool -list -keystore "$KEYFILE" -storepass "$PASSWORD" -storetype "$KEYTYPE")
    printf "%s" "$KEYSTORECONTENTS"
fi
