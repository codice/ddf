#!/bin/bash

SRC=${CONFIG_SRC:=configs}
DEST=${CORE_DEST:=cores/}
CORES="$@"

if [ -z "${CORES}" ]; then
  printf "\nNo core arguent given\n"
  exit 1
fi

for core in "$@"
do
  printf "\nCreating core ${core}...\n\tUsing configs from ${SRC}\n\tStoring in ${DEST}\n"

  mkdir -p ${DEST}/${core}/conf
  cp -r ${SRC}/* ${DEST}/${core}/conf/
cat > ${DEST}/${core}/core.properties << EOF
name=${core}
config=solrconfig.xml
schema=schema.xml
dataDir=data
ulogDir=data
EOF

done