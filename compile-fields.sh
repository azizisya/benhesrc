#!/usr/local/bin/bash
mkdir /users/grad/ben/workspace/tmp_classes
LIB=/users/grad/ben/workspace/lib
find /users/grad/ben/workspace/src_fields/ -name *.java |grep -v BiLGammaFieldMatching |\
xargs javac -classpath \
/users/grad/ben/workspace/src_fields:$LIB/terrier2.jar:$LIB/antlr-2.7.4.jar:$LIB/trove.jar:$LIB/html-parser.jar:$LIB/jdbm-0.20.jar:$LIB/terrier-links-0.2dev.jar:$LIB/stat.jar:$LIB/log4j-1.2.9.jar -d /users/grad/ben/workspace/tmp_classes
jar cfM $LIB/terrier-fields.jar -C /users/grad/ben/workspace/tmp_classes/ .
rm -r /users/grad/ben/workspace/tmp_classes
