#!/bin/bash
if [ ! -n "$TERRIER_HOME" ]
then
  TEMPVAR=`which $0`
  TEMPVAR=`dirname $TEMPVAR`
  TERRIER_HOME=`dirname $TEMPVAR`
  echo "Setting TERRIER_HOME to $TERRIER_HOME"
fi

if [ ! -n "$JAVA_HOME" ]
then
  TEMPVAR=`which java`
  TEMPVAR=`dirname $TEMPVAR`
  JAVA_HOME=`dirname $TEMPVAR`
  echo "Setting JAVA_HOME to $JAVA_HOME"
fi

CLASSPATH=~ben/gnu-trove-1.0.2.jar
CLASSPATH=$CLASSPATH:~ben/terrier2.jar
CLASSPATH=$CLASSPATH:~ben/smooth.jar
CLASSPATH=$CLASSPATH:~ben/antlr-2.7.4.jar

nice $JAVA_HOME/bin/java -Xmx768M \
     -Dterrier.setup=$TERRIER_HOME/etc/terrier.properties \
     -cp $CLASSPATH Run $@
