#!/bin/bash
#
# Terrier - Terabyte Retriever version 1.0.1
# Webpage: http://ir.dcs.gla.ac.uk/terrier 
# Contact: terrier@dcs.gla.ac.uk 
#
# The contents of this file are subject to the Mozilla Public
# License Version 1.1 (the "License"); you may not use this file
# except in compliance with the License. You may obtain a copy of
# the License at http://www.mozilla.org/MPL/
#
# Software distributed under the License is distributed on an "AS
# IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
# implied. See the License for the specific language governing
# rights and limitations under the License.
#
# The Original Code is compile.sh
#
# The Initial Developer of the Original Code is the University of Glasgow.
# Portions created by The Initial Developer are Copyright (C) 2004, 2005 
# the initial Developer. All Rights Reserved.
#
# Contributor(s):
#	Vassilis Plachouras <vassilis@dcs.gla.ac.uk> (original author)
#	Craig Macdonald <craigm@dcs.gla.ac.uk>
#
# Compiles the source code of Terrier and creates a jar file 
# terrier-$VERSION.jar in the directory lib

# -----------------------------------------------------------------

fullPath () {
	t='TEMP=`cd $TEMP; pwd`'
	for d in $*; do
		eval `echo $t | sed 's/TEMP/'$d'/g'`
	done
}

# -----------------------------------------------------------------

echo "************* COMPILING ***************"

TERRIER_HOME=/users/grad/ben/workspace
echo "Setting TERRIER_HOME to $TERRIER_HOME"

if [ ! -n "$JAVA_HOME" ]
then
	#where is java?
	TEMPVAR=`which java`
	#j2sdk/bin folder is in the dir that java was in
	TEMPVAR=`dirname $TEMPVAR`
	#then java install prefix is folder above
	JAVA_HOME=`dirname $TEMPVAR`
	echo "Setting JAVA_HOME to $JAVA_HOME"
fi

JARNAME=terrier2.jar
TMPDIR=$TERRIER_HOME/tmp_classes

for jar in $LIB/*.jar; do
	if [ ! -n "$CLASSPATH" ]
	then
		CLASSPATH=$jar;
        else
                CLASSPATH=$CLASSPATH:$jar
        fi
done

#CLASSPATH=/users/grad/ben/workspace/terrier2:$LIB/trove.jar:$LIB/htmlparser.jar:$LIB/jdbm-0.20.jar:$LIB/stat.jar:$LIB/PDFBox-0.6.6.jar:$LIB/poi-2.5.1-final-20040804.jar:$LIB/tm-extractors-0.4.jar:$LIB/antlr-2.7.4.jar:$LIB/junit.jar:$LIB/log4j-1.2.9.jar:$LIB/terrier-links.jar:$LIB/snowball.jar:$LIB/commons-lang-2.1.jar:/local/trmaster/opt/hadoop/current/conf/:$LIB/commons-cli-2.0-SNAPSHOT.jar:$LIB/commons-codec-1.3.jar:$LIB/commons-httpclient-3.0.1.jar:$LIB/commons-logging-1.0.4.jar:$LIB/commons-logging-api-1.0.4.jar:$LIB/hadoop-0.17.1-core.jar

echo $CLASSPATH

pushd $TERRIER_HOME/terrier2/uk/ac/gla/terrier/querying/parser &>/dev/null
${JAVA_HOME}/bin/java -cp ${CLASSPATH} antlr.Tool terrier_floatlex.g
${JAVA_HOME}/bin/java -cp ${CLASSPATH} antlr.Tool terrier_normallex.g
${JAVA_HOME}/bin/java -cp ${CLASSPATH} antlr.Tool terrier.g
popd &>/dev/null

mkdir $TMPDIR
find $TERRIER_HOME/terrier2/ -name '*.java' |grep -v TRECFullQuery|\
	xargs $JAVA_HOME/bin/javac \
		-classpath $CLASSPATH \
		-d $TMPDIR && \
$JAVA_HOME/bin/jar cfM $TERRIER_HOME/lib/$JARNAME \
		-C $TMPDIR .

rm -rf $TMPDIR
scp lib/terrier2.jar $irlab:~/workspace/lib
exit

