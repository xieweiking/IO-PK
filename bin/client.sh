#!/bin/bash

# set JAVA_HOME variable here.
# JAVA_HOME=your_java_home_dir

# set JVM_OPTS variable here.
JVM_OPTS="-server -Xms64m -Xmx128m -XX:PermSize=64m -XX:MaxPermSize=128m"

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
    echo "Usage: client host port [threas(100) time(5) file(reports.csv) title(TITLE)]"
    echo "       time unit is Minute"
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
"$JAVA_RUN" -cp "$CLASSPATH" $JVM_OPTS example.EchoClient $1 $2 $3 $4 "$5" "$6"
