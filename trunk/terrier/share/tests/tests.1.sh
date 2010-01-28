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
# The Original Code is tests.1.sh
#
# The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
# All Rights Reserved.
#
# Contributor(s):
#   Craig Macdonald craigm{a.}dcs.gla.ac.uk (original author)

. share/tests/test.lib.sh

#The following topic set is disabled, as Terrier's index format cannot support such queries accurately
#share/tests/test.shakespeare-merchant.phrase-fields.topics

TEST_NAME="Blocks" TERRIER_OPTIONS="-Dblock.indexing=true" doShakeSpeare \
	share/tests/test.shakespeare-merchant.basic.topics \
	share/tests/test.shakespeare-merchant.phrase.topics

TEST_NAME="Blocks UTF" TERRIER_OPTIONS="-Dblock.indexing=true -Dstring.use_utf=true" doShakeSpeare \
    share/tests/test.shakespeare-merchant.basic.topics \
    share/tests/test.shakespeare-merchant.phrase.topics

TEST_NAME="Blocks+fields" TERRIER_OPTIONS="-DFieldTags.process=TITLE -Dblock.indexing=true" doShakeSpeare \
	share/tests/test.shakespeare-merchant.basic.topics \
	share/tests/test.shakespeare-merchant.field.topics \
	share/tests/test.shakespeare-merchant.phrase.topics 

TEST_NAME="Blocks+fields UTF" TERRIER_OPTIONS="-DFieldTags.process=TITLE -Dblock.indexing=true -Dstring.use_utf=true" doShakeSpeare \
    share/tests/test.shakespeare-merchant.basic.topics \
    share/tests/test.shakespeare-merchant.field.topics \
    share/tests/test.shakespeare-merchant.phrase.topics
