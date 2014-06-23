@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem set JAVA_HOME variable here.
rem set JAVA_HOME=your_java_home_dir

set OLD_PATH=%CD%
cd ..
set SERVER_HOME=%CD%
cd %OLD_PATH%
rem set JVM_OPTS variable here.
set JVM_OPTS=-server -Xms512m -Xmx512m -XX:PermSize=64m -XX:MaxPermSize=128m -Dexample.server.home=%SERVER_HOME%

:HELP
if "%1" == "help" goto START_HELP
if "%1" == "-help" goto START_HELP
if "%1" == "/?" goto START_HELP
if "%1" == "" goto START_HELP
goto NOT_HELP

:START_HELP
echo Usage: server io_type [port(8888)] [backlog(100)]
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
"%JAVA_RUN%" %JVM_OPTS% example.%1.EchoServer 0.0.0.0 %2 %3

:THE_END
endlocal
