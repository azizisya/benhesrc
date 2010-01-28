#!/bin/bash
mkdir ~/workspace/tmp_classes
find ~/workspace/src_statistics/ -name *.java |\
	xargs javac -classpath \
	$LIB/trove.jar:$LIB/html-parser.jar:$LIB/jdbm-0.20.jar:$LIB/PDFBox-0.6.6.jar:$LIB/poi-2.5.1-final-20040804.jar:$LIB/tm-extractors-0.4.jar:$LIB/antlr-2.7.4.jar:$LIB/junit.jar \
	-d ~/workspace/tmp_classes
jar cfM $LIB/stat.jar -C ~/workspace/tmp_classes/ .
rm -r ~/workspace/tmp_classes
