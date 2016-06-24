@echo off
:: Copyright (c) Codice Foundation
::
:: This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
:: version 3 of the License, or any later version.
::
:: This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
:: See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
:: <http://www.gnu.org/licenses/lgpl.html>.

if "%1" == "" goto USAGE

setlocal
set JARSIGNER=jarsigner
setlocal
set ALIAS=localhost
setlocal
set HOST=127.0.0.1

:GETOPTS
if %1 == -d (
    setlocal
    set DIR=%~2& shift
)
if %1 == -z (
    setlocal
    set FILENAME=%~2& shift
)
if %1 == -k (
    setlocal
    set KEYSTORE=%~2& shift
)
if %1 == -a (
    setlocal
    set ALIAS=%~2& shift
)
if %1 == -h (
    setlocal
    set HOST=%~2& shift
)
if %1 == -c (
    setlocal
    set CQL=%~2& shift
)
shift
if not (%1)==() goto GETOPTS
if "%DIR%"=="" goto USAGE
if "%FILENAME%"=="" goto USAGE
if "%KEYSTORE%"=="" goto USAGE

echo Connecting to running DDF and exporting catalog to %FILENAME%...

if "%CQL%"=="" (
    echo Calling client with command:  catalog:dump --include-content %FILENAME% %DIR%
    call client.bat -h %HOST% "catalog:dump --include-content %FILENAME% %DIR% "
) else (
    echo Calling client with command:  catalog:dump --include-content %FILENAME% --cql ^"%CQL%^" %DIR%
    call client.bat -h %HOST% "catalog:dump --include-content %FILENAME% --cql ""%CQL%"" %DIR% "
)

where %JARSIGNER% >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Unable to find %JARSIGNER%, ensure that $JAVA_HOME and $JAVA_HOME/bin are set on the path.
    exit /b 1
) else (
    echo Running %JARSIGNER% on %FILENAME%, which will prompt you to enter the keystore password to sign the zip file...
    call %JARSIGNER% -keystore %KEYSTORE% %DIR%/%FILENAME% %ALIAS% -signedJar %DIR%signed%FILENAME%
)
goto :eof

:USAGE
echo SYNTAX
echo    ^<-d directory^> ^<-z zipname^> ^<-k keystore location^> [-a alias] [-h hostname] [-c cql-expr]

echo ARGUMENTS
echo    -d directory : The absolute path of the directory to export the metacard
echo                   archive. Directory path must exist.
echo    -z zipname   : The name of the archive file to be generated. The command
echo                   will fail if this name is not unique in the directory.
echo    -k keystore  : The absolute path to the keystore directory.

echo OPTIONS
echo    -a alias     : Name of alias of the private key entry in the keystore.
echo                   Typically matches the Common Name (CN) for the host in
echo                   the keystore.
echo    -c cql-expr  : Search using CQL Filter expressions.
echo                   CQL Examples:
echo                   {Textual}  -c "title like 'some text'"
echo                   {Temporal} -c "modified before 2012-09-01T12:30:00Z"
echo                   {Spatial}  -c "DWITHIN(location, POINT (1 2) , 10, kilometers)"