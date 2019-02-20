@echo Off
setlocal enabledelayedexpansion

set /A desiredLength=8
Set Alphanumeric=abcdefghjklmnopqrstuvwxyz0123456789
rem "Length" is the string length of %alphanumeric%
Set Length=31
set count=0
rem "RndAlphaNum" is the output
SET RndAlphaNum=

:loop
Set /A count+=1
Set /A RND=%Random% %% %Length%
SET RndAlphaNum=!RndAlphaNum!!Alphanumeric:~%RND%,1!
if !count! lss %desiredLength% goto loop

echo !RndAlphaNum!
