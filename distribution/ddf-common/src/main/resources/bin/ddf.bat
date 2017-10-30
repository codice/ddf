@ECHO off
SETLOCAL

SET ARGS=%*
SET DIRNAME=%~dp0%

:RESTART
REM Remove the restart file indicator so we can detect later if restart was requested
IF EXIST "%DIRNAME%\restart.jvm" (
  DEL "%DIRNAME%\restart.jvm"
)

REM Actually invoke ddf to gain restart support
CALL "%DIRNAME%\karaf.bat" %ARGS%
SET RC=%ERRORLEVEL%

IF "%1"== "daemon" (
  EXIT /B %RC%
) ELSE (
  REM Check if restart was requested by ddf_on_error.bat
  IF EXIST "%DIRNAME%\restart.jvm" (
    ECHO Restarting JVM...
    GOTO :RESTART
  ) ELSE (
    EXIT /B %RC%
  )
)
