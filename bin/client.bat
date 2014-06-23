@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem set JAVA_HOME variable here.
rem set JAVA_HOME=your_java_home_dir

rem set JVM_OPTS variable here.
set JVM_OPTS=-server -Xms512m -Xmx512m -XX:PermSize=64m -XX:MaxPermSize=128m

:HELP
if "%1" == "help" goto START_HELP
if "%1" == "-help" goto START_HELP
if "%1" == "/?" goto START_HELP
if "%1" == "" goto START_HELP
goto NOT_HELP

:START_HELP
echo Usage: client host port [threas(100) time(5) file(reports.csv) title(TITLE)]
echo.       time unit is Minute.
goto THE_END

:NOT_HELP
set JAVA_RUN=java
if "%JAVA_HOME%" == "" goto SKIP_JAVA_HOME
set "JAVA_RUN=%JAVA_HOME%\bin\%JAVA_RUN%"

:SKIP_JAVA_HOME
cd /d %~dp0
cd ..
set CD_LIB_JARS=
for %%i in ("%CD%\lib\*.jar") do set CD_LIB_JARS=!CD_LIB_JARS!%%i;
set CLASSPATH=%CLASSPATH%%CD_LIB_JARS%

echo Using JAVA_HOME:      %JAVA_HOME%
echo.
echo CLASSPATH=%CLASSPATH%
echo.
echo PATH=%PATH%
echo.

:START_NORMAL
echo Using JVM_OPTS:       %JVM_OPTS%
"%JAVA_RUN%" %JVM_OPTS% example.EchoClient %1 %2 %3 %4 "%5" "%6"

:THE_END
endlocal
