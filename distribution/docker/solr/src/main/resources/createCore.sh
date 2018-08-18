#!/bin/bash

SRC=${CONFIG_SRC:=configs}
DEST=${CORE_DEST:=cores/}
CORE=${1}

if [ -z "${CORE}" ]; then
  printf "\nNo core arguent given\n"
  exit 1
fi

printf "\nCreating core ${CORE}...\n\tUsing configs from ${SRC}\n\tStoring in ${DEST}\n"

mkdir -p ${DEST}/${CORE}/conf
cp -r ${SRC}/* ${DEST}/${CORE}/conf/

cat > ${DEST}/${CORE}/core.properties << EOF
name=${CORE}
config=solrconfig.xml
schema=schema.xml
dataDir=data
ulogDir=data
EOF
