#!/bin/bash

# Terrier - Terabyte Retriever 
# Webpage: http://ir.dcs.gla.ac.uk/terrier 
# Contact: terrier{a.}dcs.gla.ac.uk
# University of Glasgow - Department of Computing Science
# http://www.gla.ac.uk/
# 
# The contents of this file are subject to the Mozilla Public License
# Version 1.1 (the "License"); you may not use this file except in
# compliance with the License. You may obtain a copy of the License at
# http://www.mozilla.org/MPL/
#
# Software distributed under the License is distributed on an "AS IS"
# basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
# the License for the specific language governing rights and limitations
# under the License.
#
# The Original Code is test.lib.sh
#
# The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
# All Rights Reserved.
#
# Contributor(s):
#   Craig Macdonald craigm{a.}dcs.gla.ac.uk (original author)




#this function uses the following environment variables
# TERRIER_OPTIONS
# TEST_NAME
function doShakeSpeare()
{
	#classical indexing
	TEST_NAME="$TEST_NAME with classical indexing" \
		INDEXING_OPTION_FIRST=1 INDEXING_OPTION_LAST=1 INDEXING_OPTION_1="-i " \
		doShakespeare_Root $*

	#classical indexing with merging
	TEST_NAME="$TEST_NAME with classical indexing and merging" \
		INDEXING_OPTION_FIRST=1 INDEXING_OPTION_LAST=1 INDEXING_OPTION_1="-i " \
		TERRIER_OPTIONS="$TERRIER_OPTIONS -Dindexing.max.docs.per.builder=12" \
		doShakespeare_Root $*

	#classical indexing in 2 JVMs
	TEST_NAME="$TEST_NAME with classical indexing in 2 processes" \
		INDEXING_OPTION_FIRST=1 INDEXING_OPTION_LAST=2 INDEXING_OPTION_1="-i -d" INDEXING_OPTION_2="-i -v"\
		doShakespeare_Root $*

	#single pass indexing
	TEST_NAME="$TEST_NAME with single pass indexing" \
		INDEXING_OPTION_FIRST=1 INDEXING_OPTION_LAST=1 INDEXING_OPTION_1="-i -j" \
		doShakespeare_Root $*
	
	#single pass indexing with merging
	TEST_NAME="$TEST_NAME with single pass indexing and merging" \
		INDEXING_OPTION_FIRST=1 INDEXING_OPTION_LAST=1 INDEXING_OPTION_1="-i -j" \
		TERRIER_OPTIONS="$TERRIER_OPTIONS -Dindexing.max.docs.per.builder=12" \
		doShakespeare_Root $*

	#single pass hadoop mode indexing
	TEST_NAME="$TEST_NAME with single pass Hadoop indexing" \
		INDEXING_OPTION_FIRST=1 INDEXING_OPTION_LAST=1 INDEXING_OPTION_1="-i -H" \
		doShakespeare_Root $*
}

#INDEXING_OPTION_FIRST, INDEXING_OPTION_LAST, INDEXING_OPTION_1, INDEXING_OPTION_2, ...
function doShakespeare_Root
{
	echo Running test ${TEST_NAME}
	mkdir -p var/test
	rm -rf var/test/*
	TERRIER_ETC=var/test bin/trec_setup.sh var/test/
	echo share/shakespeare-merchant.trec > var/test/collection.spec
	echo terrier.index.path=${PWD}/var/test >> var/test/terrier.properties
	echo ignore.low.idf.terms=false >> var/test/terrier.properties
	echo trec.results=${PWD}/var/test >> var/test/terrier.properties
	echo Running Indexing: ${INDEXING_OPTION_FIRST}..${INDEXING_OPTION_LAST}
	if [ "$DEBUG" == "1" ];
	then
		cat > var/test/terrier-log.xml <<EOF
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">
<log4j:configuration xmlns:log4j="http://jakarta.apache.org/log4j/">
 <appender name="console" class="org.apache.log4j.ConsoleAppender">
  <param name="Target" value="System.err"/>
  <layout class="org.apache.log4j.SimpleLayout"/>
 </appender>
 <root>
  <priority value="debug" />
  <appender-ref ref="console" />
 </root>
</log4j:configuration>
EOF
	fi
	i=${INDEXING_OPTION_FIRST}
	let $((i--))
	while ((i < INDEXING_OPTION_LAST));
	do
		let $((i++))
		VAR="INDEXING_OPTION_`echo ${i}`"
		VALUE=${!VAR}
		echo ${i}. TERRIER_ETC=var/test bin/trec_terrier.sh `echo $VALUE`
		TERRIER_ETC=var/test bin/trec_terrier.sh `echo $VALUE`
	done
	if [ ! -e "var/test/data.properties" ];
	then
		echo "****************";
		echo Test ${TEST_NAME} failed: Index not found;
		echo "****************";
		return 3;
	fi
	echo =========================
	echo Index properties
	echo =========================
	cat var/test/data.properties
	#cat var/test/data_*.properties
	ls var/test/
	echo =========================
	for i in $*;
	do
		echo $i >> var/test/trec.topics.list
	done
	NUMQ=`cat var/test/trec.topics.list | grep -v '^#' | xargs cat |grep -v '^#' | wc -l `
	echo ${PWD}/share/tests/test.shakespeare-merchant.all.qrels > var/test/trec.qrels
	echo trec.topics.parser=SingleLineTRECQuery >> var/test/terrier.properties
	#TERRIER_ETC=var/test bin/trec_terrier.sh --printlexicon | sed 's/^/LEX: /'
	TERRIER_ETC=var/test bin/trec_terrier.sh -r
	TERRIER_ETC=var/test bin/trec_terrier.sh -e
	cat var/test/*.eval
	MAP=`cat var/test/*.eval | grep "Average Precision"| sed "s/Average Precision: //" | head -1`
	RUNQ=`cat var/test/*.eval | grep "Number of queries" |  sed "s/Number of queries  = //" | head -1`
	if [ -z "$RUNQ" ];
	then
		RUNQ=0
	fi
	RES=`cat var/test/*.res`
	rm -rf var/test
	if [ $RUNQ  -eq $NUMQ ];
	then
		if [ "$MAP" == "1.0000" ];
		then
			echo "****************"; 
			echo Test ${TEST_NAME} passed: MAP Good; 
			echo "****************"; 
			return 0;
		else 
			echo "****************"; 
			echo Test ${TEST_NAME} failed: Bad MAP $MAP; 
			echo "****************"; 
			echo $RES
			return 1; 
		fi
	else
		echo "****************";
		echo Test ${TEST_NAME} failed: not enough queries: $RUNQ found, $NUMQ expected. MAP was $MAP;
		echo "****************";
		echo $RES	
		return 2;	
	fi

}
