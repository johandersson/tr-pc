@echo off
setlocal

set "TR_JAVA_HOME=%~dp0..\runtime"
if exist "%TR_JAVA_HOME%\bin\server\jvm.dll" goto launch

set "TR_JAVA_HOME=%JAVA_HOME%"
if exist "%TR_JAVA_HOME%\bin\server\jvm.dll" goto launch

set "TR_JAVA_HOME="
for /f "tokens=2,* delims==" %%A in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /R /C:"^[ ]*java.home ="') do set "TR_JAVA_HOME=%%B"
for /f "tokens=* delims= " %%A in ("%TR_JAVA_HOME%") do set "TR_JAVA_HOME=%%A"

:launch
if defined TR_JAVA_HOME (
    start "" "%~dp0trgtd64.exe" --jdkhome "%TR_JAVA_HOME%" %*
) else (
    start "" "%~dp0trgtd64.exe" %*
)
