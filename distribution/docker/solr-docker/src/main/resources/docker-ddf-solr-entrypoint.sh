#!/bin/bash
set -e

# This entrypoint script will be called first to do some ddf specific things before handing off to the official solr docker-entrypoint script

if [ -n "${CORES:-}" ]; then
  if [ -n "${ZK_HOST:-}" ]; then
    printf 'CORES is for user-managed mode and cannot be combined with ZK_HOST.\n' >&2
    exit 1
  fi
  init-var-solr
  printf "\nCreating the following cores:\n\t${CORES}\n"
  create-ddf-core $CORES
  # Solr 10 defaults to cloud mode; pre-created local cores require user-managed mode.
  case "${1:-}" in
    solr-foreground|solr-fg) set -- "$@" --user-managed ;;
  esac
fi

# pass all arguments on to the official solr entrypoint
exec docker-entrypoint.sh "$@"
