#!/bin/bash

# set JAVA_HOME variable here.
# JAVA_HOME=your_java_home_dir

OLD_PATH=`pwd`
cd ..
SERVER_HOME=`pwd`
cd $OLD_PATH
# set JVM_OPTS variable here.
JVM_OPTS="-server -Xms512m -Xmx512m -XX:PermSize=64m -XX:MaxPermSize=128m -Dexample.server.home=$SERVER_HOME"

if [ "$1" = "-help" ] ; then
    STATUS_MODE="HELP"
elif [ "$1" = "--help" ] ; then
    STATUS_MODE="HELP"
elif [ "$1" = "help" ] ; then
    STATUS_MODE="HELP"
elif [ "$1" = "" ] ; then
    STATUS_MODE="HELP"
fi

if [ "$STATUS_MODE" = "HELP" ] ; then
    echo "Usage: server io_type [port(8888)] [backlog(100)]"
    exit
fi

if [ -n "$JAVA_HOME"  ] ; then
    JAVA_RUN=$JAVA_HOME/bin/java
else
    JAVA_RUN=`which java 2> /dev/null `
    if [ -z "$JAVA_RUN" ] ; then
        JAVA_RUN=java
    fi
fi

C=`cd ../ > /dev/null 2>&1 && pwd`
for i in $C/lib/*.jar
do
    CD_LIB_JARS="$CD_LIB_JARS:$i"
done
CLASSPATH=$CLASSPATH$CD_LIB_JARS

echo "Using JAVA_HOME:       $JAVA_HOME"
echo "CLASSPATH=$CLASSPATH"
echo
echo "PATH=$PATH"
echo


echo "Using JVM_OPTS:       $JVM_OPTS"
"$JAVA_RUN" -cp "$CLASSPATH" $JVM_OPTS example.$1.EchoServer 0.0.0.0 $2 $3
