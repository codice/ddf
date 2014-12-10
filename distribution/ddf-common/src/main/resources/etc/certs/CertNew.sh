#! /bin/bash

CA_SCRIPT=./wrapper.sh

if [ ! -e $CA_SCRIPT ]; then
  echo "Unable to find $CA_SCRIPT. Is OpenSSL installed?"
  exit
fi

read -p "Enter server or user name (common name): " CN

export OPENSSL_CONF=openssl-demo.cnf

# Create new CSR
$CA_SCRIPT -newreq <<EOF
$CN





${CN}@example.org
EOF

if [[ ! -r newkey.pem && ! -r newreq.pem ]]; then
  echo "Cert request failed." >&2
  exit 1
fi

# Sign CSR
$CA_SCRIPT -sign <<EOF
y
y
EOF

if [[ ! -r newcert.pem ]]; then
  echo "Cert signing failed." >&2
  exit 1
fi

# Convert key/cert to PKCS12 form
$CA_SCRIPT -pkcs12 $CN

if [[ ! -r newcert.p12 ]]; then
  echo "Failed to create PKCS12 cert." >&2
  exit 1
fi

mkdir -p $CN

for f in new*; do
  mv $f ${CN}/${f/new/$CN-}
done

# Import into a Java Keystore
keytool -importkeystore -srckeystore ${CN}/${CN}-cert.p12 -srcalias $CN -srcstoretype pkcs12 -destkeystore ${CN}/${CN}.jks -deststoretype jks -destalias ${CN} -deststorepass changeit -srcstorepass changeit

echo
echo "Certificates are created in ${CN}/"
