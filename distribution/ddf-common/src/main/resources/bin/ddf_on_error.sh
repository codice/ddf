#!/bin/sh

HOW=$1
PID=$2

#
# Waits for a process identified using its PID to die.
#
# param   $1 - the pid of the process to wait for
# returns 0 if the process died; 1 if we timed out before it died
#
waitForPid() {
   local i=0;

   until kill -0 $1; do
     sleep 1
     i=$((i+1))
     if [ $i -eq 120 ]; then
       return 1
     fi
   done
   return 0
}

echo "Fatal error occurred with process $PID; trying to terminate and restart it" >&2

if [ $HOW = "wrapper" ]; then
  # When the process is started via karaf's service wrapper, we need to notify the wrapper to restart
  # the JVM process. To do so, we insert a 'restart' command in the restart file. The wrapper polls
  # this file very 5 seconds for commands to process
  echo "RESTART" > bin/restart.jvm
  # Simply continue and let the wrapper trigger the restart
else
  # When the process is started via karaf's script, our ddf.sh script which invokes karaf's script
  # will check upon exit Karaf's script if the restart.jvm file is present and if it will restart
  # karaf's script which will end up restarting the JVM
  touch bin/restart.jvm
  # Try to terminate the process normally to give it a chance
  kill $PID
fi

# Wait for the process to die
waitForPid $PID
if [ $? -ne 0 ]; then
    # It didn't die according to plan so time to really kill it
    echo "Forcibly terminating and restarting process $PID" >&2
    kill -9 $PID
fi

echo "Process $PID was terminated" >&2
