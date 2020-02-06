#!/bin/bash

# This entrypoint script will be called first to do some ddf specific things before handing off to the official solr docker-entrypoint script

if [ ! -z "${CORES}" ]; then
  printf "\nCreating the following cores:\n\t${CORES}\n"
  create-ddf-core $CORES
fi

# pass all arguments on to the official solr entrypoint
exec docker-entrypoint.sh "$@"