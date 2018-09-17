@ECHO off
REM Call this script with the desired PROPERTY_NAME as the first argument and DEFAULT value as the second argument
SET PROPERTY_NAME=%1
SET PROPERTY_DEFAULT=%2
SET SCRIPTDIR=%~dp0%
SET VALUE=""

PUSHD %SCRIPTDIR%..
SET HOME_DIR=%CD%
POPD

SET PROPERTIES_FILE=%HOME_DIR%\etc\custom.system.properties

REM Check file exists
IF NOT EXIST "%PROPERTIES_FILE%" (
    ECHO Cannot find the file %PROPERTIES_FILE%
    SET RETURN_VALUE=%PROPERTY_DEFAULT%
    GOTO:return
)

REM Retrieve propertry from file
FOR /f "tokens=1* delims==" %%G in ('FINDSTR /I /R /C:"%PROPERTY_NAME% *=" %PROPERTIES_FILE%') DO SET VALUE=%%H

REM Remove potential leading whitespace
FOR /F "tokens=1* delims= " %%c IN ("%VALUE%") DO SET PROPERTY_VALUE=%%c

IF [%PROPERTY_VALUE%] == [] (
	IF [%PROPERTY_DEFAULT%] == [] (
		ECHO ERROR: %PROPERTY_NAME% is not a property of %PROPERTIES_FILE%
		SET RETURN_VALUE=""
	) ELSE (
		ECHO ERROR: %PROPERTY_NAME% is not a property of %PROPERTIES_FILE%
		SET RETURN_VALUE=%PROPERTY_DEFAULT%
	)
) ELSE (
	SET RETURN_VALUE=%PROPERTY_VALUE:/=\%
)
:return
SET %~1=%RETURN_VALUE%
