#!/bin/bash
set -e

# createCores.sh
# This script can be used to pre-create solr cores for use with DDF. If a specific core name already exists no action will be performed for that core.
# ARGS: coreNames...
# 
# The image sets CONFIG_SRC to /seed and CORE_DEST to /var/solr/data.
# Override these environment variables to use a different config source or core directory.

SRC=${CONFIG_SRC:=configs}
DEST=${CORE_DEST:=cores/}

if [ $# -eq 0 ]; then
  printf "\nNo core arguent given\n"
  exit 1
fi

for core in "$@"
do
  if [ -f "${DEST}/${core}/core.properties" ]; then
    printf 'Core %s already exists; leaving its configuration unchanged.\n' "$core"
    continue
  fi

  printf "\nCreating core ${core}...\n\tUsing configs from ${SRC}\n\tStoring in ${DEST}\n"

  mkdir -p "${DEST}/${core}/conf"
  cp -r "${SRC}/." "${DEST}/${core}/conf/"
cat > "${DEST}/${core}/core.properties" << EOF
name=${core}
config=solrconfig.xml
schema=schema.xml
dataDir=data
ulogDir=data
EOF

done
