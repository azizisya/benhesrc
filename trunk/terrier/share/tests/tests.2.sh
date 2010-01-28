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
# The Original Code is tests.2.sh
#
# The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
# All Rights Reserved.
#
# Contributor(s):
#   Craig Macdonald craigm{a.}dcs.gla.ac.uk (original author)

. share/tests/test.lib.sh

MODELS="BB2 BM25 DFR_BM25 DLH13 DLH DFRee Hiemstra_LM IFB2 In_expB2 In_expC2 InL2 LemurTF_IDF PL2 TF_IDF"
BASICMODELS="B BM Br IF In_exp In P"
AFTEREFFECTS="B L LL"
NORM="0 2 2exp 3 B"
#The following topic set is disabled, as Terrier's index format cannot support such queries accurately
#share/tests/test.shakespeare-merchant.phrase-fields.topics

for i in `echo $MODELS`;
do
	echo $i
	TEST_NAME="Model-$i" TERRIER_OPTIONS="-Dtrec.model=$i" \
		INDEXING_OPTION_FIRST=1 INDEXING_OPTION_LAST=1 INDEXING_OPTION_1="-i " doShakespeare_Root \
		share/tests/test.shakespeare-merchant.basic.topics 
done

for b in `echo $BASICMODELS`
do	#for each basic model
	for a in `echo $AFTEREFFECTS`;
	do	#for each aftereffect
		for n in `echo $NORM`; 
		do	#for each normalisation
			TEST_NAME="DFRWeightingModel($b,$a,$n)" TERRIER_OPTIONS="-Dtrec.model=DFRWeightingModel($b,$a,$n)" \
				INDEXING_OPTION_FIRST=1 INDEXING_OPTION_LAST=1 INDEXING_OPTION_1="-i " doShakespeare_Root \
				share/tests/test.shakespeare-merchant.basic.topics
		done
	done
done




