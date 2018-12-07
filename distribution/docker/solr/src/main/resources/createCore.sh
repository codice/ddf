#!/bin/bash

# createCores.sh
# This script can be used to pre-create solr cores for use with DDF. If a specific core name already exists no action will be performed for that core.
# ARGS: coreNames...
# 
# By default this script assumes that there are template files for creating ddf solr cores in the ./configs directory. To override this set the CONFIG_SRC environment variable
# By default this script assumes that cores should be created in the ./cores directory. To override this set the CORE_DEST environment variable

SRC=${CONFIG_SRC:=configs}
DEST=${CORE_DEST:=cores/}

if [ $# -eq 0 ]; then
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
