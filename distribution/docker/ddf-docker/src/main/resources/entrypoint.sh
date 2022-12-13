#!/bin/sh

echo "Starting ${APP_NAME}"
"$APP_HOME/bin/karaf" server "$@" >> /dev/null 2>&1 &

echo -n "Waiting for log file: ${APP_LOG} to be created..."
while [ ! -f ${APP_LOG} ]
do
  sleep 1
  echo -n "."
done

echo
echo "Log file found, continuing..."

tail -f $APP_LOG