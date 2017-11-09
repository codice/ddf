@ECHO off
SETLOCAL ENABLEDELAYEDEXPANSION

SET DIRNAME=%~dp0%

SET HOW=%1
SET PID=%2

IF "%HOW%" == "wrapper" (
  IF "!PID!" == "" (
    ECHO Fatal error occurred with process; trying to terminate and restart it 1>&2
  ) else (
    ECHO Fatal error occurred with process !PID!; trying to terminate and restart it 1>&2
  )
  REM When the process is started via karaf's service wrapper, we need to notify the wrapper to restart
  REM the JVM process. To do so, we insert a 'restart' command in the restart file. The wrapper polls
  REM this file very 5 seconds for commands to process
  ECHO RESTART >> "%DIRNAME%restart.jvm"
  SET RC=0
) ELSE (
  REM When the process is started via karaf's script, our ddf.bat script which invokes karaf's script
  REM will check upon exit Karaf's script if the restart.jvm file is present and if it will restart
  REM karaf's script which will end up restarting the JVM
  TYPE NUL > "%DIRNAME%restart.jvm"
  IF "!PID!" == "" (
    IF EXIST "%DIRNAME%..\karaf.pid" (
      SET /P PID=<%DIRNAME%..\karaf.pid
    )
    IF "!PID!" == "" (
      REM not much we can do if we do not get a PID when running via the script
      ECHO Fatal error occurred with process; trying to terminate and restart it failed as we are unable to determine its process id 1>&2
      EXIT /B 1
    ) ELSE (
      ECHO Fatal error occurred with process !PID!; trying to terminate and restart it 1>&2
    )
  ) else (
    ECHO Fatal error occurred with process !PID!; trying to terminate and restart it 1>&2
  )
  REM Try to terminate the process normally to give it a chance
  TaskKill /PID !PID! >NUL 2>&1
  IF ERRORLEVEL 1 (
    SET RC=1
  ) ELSE (
    SET RC=0
  )
)

IF %RC%==0 (
  IF NOT "%PID%" == "" (
    REM Wait for the process to die
    CALL :WAIT_FOR_PID  
    IF ERRORLEVEL 1 (
      SET RC=1
    ) ELSE (
      SET RC=0
    )
  )
)
IF %RC%==1 (
  REM It didn't die according to plan so time to really kill it
  ECHO Forcibly terminating and restarting process %PID% 1>&2
  TaskKill /PID %PID% /F >NUL 2>&1
)

ECHO Process %PID% was terminated 1>&2
EXIT /B 0

:WAIT_FOR_PID
SET I=0

:WAIT_FOR_PID_LOOP
TaskList /FI "PID eq %PID%" /NH /FO:csv >NUL 2>&1
IF ERRORLEVEL 1 (
  EXIT /B 0
) ELSE (
  TIMEOUT /T 1 >NUL
  SET /A I=I+1
  IF %I%==120 (
    EXIT /B 1
  )
  GOTO :WAIT_FOR_PID_LOOP
)
