ddf-ui-path() {
  local NODE=$(pwd)/node
  local YARN=$(pwd)/node/yarn/dist/bin
  local TEMPFILE=$(mktemp)

  # Ensure that the local copy of node and yarn are installed and that
  # they are the correct versions. Avoid informing the user unless there
  # is an error. The reason for a temp file instead of capturing output
  # into a local variable is that we lose the status code.
  mvn com.github.eirslett:frontend-maven-plugin:1.6.CODICE:install-node-and-yarn \
    '-DnodeVersion=${node.version}' \
    '-DyarnVersion=${yarn.version}' > $TEMPFILE

  local MVN_STATUS=$?
  local LOG=$(cat $TEMPFILE)
  rm $TEMPFILE

  if [ $MVN_STATUS -ne 0 ]; then
    >&2 echo "Error running maven command to install local node/yarn."
    >&2 echo ""
    >&2 echo "Maven Log:\n$LOG"
    return
  fi

  echo "export PATH='$NODE:$YARN:$PATH'"
  echo 'echo "node version: $(node -v)"'
  echo 'echo "yarn version: $(yarn -v)"'
}

eval "$(ddf-ui-path)"

