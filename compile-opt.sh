#!/bin/bash
mkdir ~/workspace/tmp_classes
find ~/workspace/src_optimisation/ -name *.java |\
xargs javac -classpath \
~/workspace/src_optimisation:$LIB/terrier2.jar:$LIB/antlr-2.7.4.jar:$LIB/trove.jar:$LIB/html-parser.jar:$LIB/jdbm-0.20.jar:$LIB/terrier-links-0.2dev.jar:$LIB/OptimizationJ2SEDemo.jar:$LIB/ireval.jar -d ~/workspace/tmp_classes
jar cfM $LIB/terrier-optimisation.jar -C ~/workspace/tmp_classes/ .
rm -r ~/workspace/tmp_classes
