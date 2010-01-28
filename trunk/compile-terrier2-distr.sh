#!/bin/bash
mkdir bin
HOME=/users/grad/ben/workspace
LIB=$HOME/lib
TERRIER_CORE=$LIB/terrier2.jar
JAVA_HOME=/local/java/linux/jdk1.5.0_07
JAVAC=$JAVA_HOME/bin/javac
JAVAVM=$JAVA_HOME/bin/java
JAVARMIC=$JAVA_HOME/bin/rmic
JAVAJAR=$JAVA_HOME/bin/jar
SRCDIR=${HOME}/terrier2-distr

SRC_FILES=`find $HOME/terrier2-distr/* -name *.java`
SRC=`echo $SRC_FILES`

CLASSPATH=$LIB/terrier2.jar:$LIB/terrier-fields.jar:${SRCDIR}:$LIB/trove.jar:$LIB/terrier-links-0.2dev.jar:$LIB/antlr-2.7.4.jar:$LIB/smooth.jar:$LIB/stat.jar:$LIB/log4j-1.2.9.jar
export CLASSPATH=$CLASSPATH
$JAVAC -classpath $CLASSPATH \
       -d bin \
       $SRC
OLD_CLASSPATH=$CLASSPATH
#export CLASSPATH=bin
$JAVARMIC -classpath $CLASSPATH -d bin uk.ac.gla.terrier.distr.matching.DistributedThreeMatchingServer
#$JAVARMIC -classpath $CLASSPATH -d bin uk.ac.gla.terrier.distr.utility.DistributedStatisticsServer
export CLASSPATH=$OLD_CLASSPATH

$JAVAJAR -cf $LIB/terrier2-distr.jar -C bin .
rm -r bin
