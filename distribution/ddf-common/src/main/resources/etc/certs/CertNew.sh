#! /bin/bash

if [ -z "$OPENSSL" ]; then OPENSSL=openssl; fi
if [ -z "$DAYS" ] ; then DAYS="-days 365" ; fi	# 1 year
if [ -z "$CATOP" ] ; then CATOP=./demoCA ; fi

CACERT=./cacert.pem

REQ="$OPENSSL req"
CA="$OPENSSL ca"
PKCS12="openssl pkcs12"

export OPENSSL_CONF=openssl-demo.cnf
if [[ ! -w ${CATOP}/index.txt ]]; then
  touch ${CATOP}/index.txt
fi
if [[ ! -w ${CATOP}/serial ]]; then
  echo 01 > ${CATOP}/serial
fi

read -p "Enter server or user name (common name): " CN
if [[ -d ${CN} ]]; then
  echo "${CN} already has been generated. Use a different name or clean up ${CN}/
    and openssl's index and serial lists in ${CATOP}/ first."
  exit 1
fi
mkdir -p "$CN"

# Create new CSR
$REQ -new -keyout "${CN}/${CN}-key.pem" -out "${CN}/${CN}-req.pem" $DAYS <<EOF
$CN





${CN}@example.org
EOF

if [[ ! -r "${CN}/${CN}-key.pem" && ! -r "${CN}/${CN}-req.pem" ]]; then
  echo "Cert request failed." >&2
  exit 1
fi

# Sign CSR
$CA -policy policy_anything -passin "pass:secret" -out "${CN}/${CN}-cert.pem" -infiles "${CN}/${CN}-req.pem" <<EOF
y
y
EOF

if [[ ! -r "${CN}/${CN}-cert.pem" ]]; then
  echo "Cert signing failed." >&2
  exit 1
fi

cat "${CN}/${CN}-cert.pem"

# Convert key/cert to PKCS12 form
$PKCS12 -in "${CN}/${CN}-cert.pem" -inkey "${CN}/${CN}-key.pem" -certfile ${CATOP}/$CACERT \
        -out "${CN}/${CN}-cert.p12" -export -name "$CN" -passin "pass:changeit" -passout "pass:changeit"

if [[ ! -r "${CN}/${CN}-cert.p12" ]]; then
  echo "Failed to create PKCS12 cert." >&2
  exit 1
fi

# Import into a Java Keystore
keytool -importkeystore -srckeystore ${CN}/${CN}-cert.p12 -srcalias $CN -srcstoretype pkcs12 \
  -destkeystore ${CN}/${CN}.jks -deststoretype jks -destalias ${CN} -deststorepass changeit -srcstorepass changeit

echo
echo "Certificates are created in ${CN}/"
