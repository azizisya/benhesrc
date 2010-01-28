#!/bin/bash
package=$1;
mkdir bin
HOME=~/workspace
LIB=$HOME/lib
TERRIER_CORE=$LIB/terrier2.jar
JAVAC=$JAVA_HOME/bin/javac
JAVAVM=$JAVA_HOME/bin/java
JAVARMIC=$JAVA_HOME/bin/rmic
JAVAJAR=$JAVA_HOME/bin/jar
SRCDIR=${HOME}/$package

SRC_FILES=`find $SRCDIR/* -name '*.java'`
SRC=`echo $SRC_FILES`

#CLASSPATH=/users/grad/ben/workspace/${package}:$LIB/terrier2.jar:$LIB/trove.jar:$LIB/html-parser.jar:$LIB/jdbm-0.20.jar:$LIB/snowball-1.0.jar:$LIB/stat.jar:$LIB/mtj.jar:$LIB/terrier-links.jar:$LIB/log4j-1.2.9.jar:$LIB/commons-cli-1.0.jar:$LIB/commons-configuration-1.5.jar:$LIB/commons-lang-2.1.jar:$LIB/commons-logging-1.1.jar:$LIB/mg4j-1.0.3.1.jar:$LIB/textractor-oracle.jar:$LIB/PDFBox-0.6.6.jar
CLASSPATH=/users/grad/ben/workspace/${package}
for jar in $LIB/*.jar; do
	if [ ! -n "$CLASSPATH" ]
	then                
		CLASSPATH=$jar;
        else
                CLASSPATH=$CLASSPATH:$jar;
      	fi
done

export CLASSPATH=$CLASSPATH
#$JAVAC -classpath $CLASSPATH \
#       -d bin \
#       $SRC

find $SRCDIR/* -name '*.java' |\
	xargs $JAVA_HOME/bin/javac \
    	-classpath $CLASSPATH \
       -d bin

#OLD_CLASSPATH=$CLASSPATH
#export CLASSPATH=bin
#$JAVARMIC -classpath $CLASSPATH -d bin uk.ac.gla.terrier.smooth.utility.SimilarityServer
#export CLASSPATH=$OLD_CLASSPATH

$JAVAJAR -cf $LIB/${package}.jar -C bin .
rm -r bin
scp $LIB/${package}.jar $irlab:~/workspace/lib
