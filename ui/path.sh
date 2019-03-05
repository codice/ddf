ddf-ui-path() {
  local NODE=$(pwd)/node
  local YARN=$(pwd)/node/yarn/dist/bin

  if [ ! -d $NODE ]; then
    >&2 echo "Directory $NODE not found."
    >&2 echo "You may need to run 'mvn install' before sourcing '$0'."
    return
  fi

  if [ ! -d $YARN ]; then
    >&2 echo "Directory $YARN not found."
    >&2 echo "You may need to run 'mvn install' before sourcing '$0'."
    return
  fi

  echo "export PATH='$NODE:$YARN:$PATH'"
  echo 'echo "node version: $(node -v)"'
  echo 'echo "yarn version: $(yarn -v)"'
}

eval "$(ddf-ui-path)"

