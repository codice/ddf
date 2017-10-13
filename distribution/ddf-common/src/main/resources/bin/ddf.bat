@echo off
setlocal

set ARGS=%*
set DIRNAME=%~dp0%

# Actually invoke ddf to gain restart support
call "%DIRNAME%/karaf.bat" %ARGS%

