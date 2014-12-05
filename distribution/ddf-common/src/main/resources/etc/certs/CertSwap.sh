#! /bin/bash

if [ $# -lt 2 ]; then
  echo "Usage: $0 <srckeystore> <srcalias>"
  exit
fi

CERT_STORE=$1
ALIAS=$2

SERVER_KEYSTORE=../keystores/serverKeystore.jks
SAVE=1

if [ -e ${SERVER_KEYSTORE}.old ]; then
  read -p "${SERVER_KEYSTORE}.old exists. Overwrite save? (y/n) " YN
  if [[ "$YN" != "y" && "$YN" != "Y" ]]; then
    SAVE=0
  fi
fi

if [ $SAVE -eq 1 ]; then
  echo "Saving $SERVER_KEYSTORE to ${SERVER_KEYSTORE}.old"
  mv $SERVER_KEYSTORE ${SERVER_KEYSTORE}.old
  cp ${SERVER_KEYSTORE}.old $SERVER_KEYSTORE
fi

keytool -importkeystore -srckeystore $CERT_STORE -srcstoretype jks -srcalias $ALIAS -destkeystore $SERVER_KEYSTORE -deststoretype jks -destalias localhost -deststorepass changeit #-srcstorepass changeit
