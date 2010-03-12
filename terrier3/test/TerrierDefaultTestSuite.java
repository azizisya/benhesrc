/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://terrier.org/
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.ac.uk/
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is TerrierDefaultTestSuite.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.terrier.evaluation.TestAdhocEvaluation;
import org.terrier.indexing.TestCollections;
import org.terrier.matching.TestMatchingQueryTerms;
import org.terrier.statistics.TestGammaFunction.TestWikipediaLanczosGammaFunction;
import org.terrier.structures.TestBasicLexiconEntry;
import org.terrier.structures.TestBitIndexPointer;
import org.terrier.structures.TestCompressingMetaIndex;
import org.terrier.structures.TestPostingStructures;
import org.terrier.structures.TestTRECQuery;
import org.terrier.structures.collections.TestFSArrayFile;
import org.terrier.structures.collections.TestFSOrderedMapFile;
import org.terrier.structures.indexing.TestCompressingMetaIndexBuilderPartitioner;
import org.terrier.structures.indexing.singlepass.hadoop.TestPositingAwareSplit;
import org.terrier.structures.indexing.singlepass.hadoop.TestSplitEmittedTerm;
import org.terrier.structures.serialization.TestFixedSizeTextFactory;
import org.terrier.terms.TestPorterStemmer;
import org.terrier.terms.TestTermPipelineAccessor;
import org.terrier.tests.ShakespeareEndToEndTestSuite;
import org.terrier.utility.TestArrayUtils;
import org.terrier.utility.TestDistance;
import org.terrier.utility.TestHeapSort;
import org.terrier.utility.TestRounding;
import org.terrier.utility.TestStaTools;
import org.terrier.utility.TestTermCodes;
import org.terrier.utility.io.TestCountingInputStream;
import org.terrier.utility.io.TestHadoopPlugin;
import org.terrier.utility.io.TestRandomDataInputMemory;


/** This class defines the active JUnit test classes for Terrier
 * @since 3.0
 * @author Craig Macdonald */
@RunWith(Suite.class)
@SuiteClasses({
	//.tests
	ShakespeareEndToEndTestSuite.class,
	
	//.compression
	//TestCompressedBitFiles.class,
	
	//.evaluation
	TestAdhocEvaluation.class,
	
	//.indexing
	TestCollections.class,
	
	//.matching
	TestMatchingQueryTerms.class,
	
	//.statistics
	TestWikipediaLanczosGammaFunction.class,
	
	//.structures
	TestBasicLexiconEntry.class,
	TestBitIndexPointer.class,
	TestCompressingMetaIndex.class,
	TestPostingStructures.class,
	TestTRECQuery.class,
	
	//.structures.collections
	TestFSOrderedMapFile.class,
	TestFSArrayFile.class,
	
	//.structures.indexing
	TestCompressingMetaIndexBuilderPartitioner.class,
	
	//.structures.indexing.sp.hadoop
	TestSplitEmittedTerm.class,
	TestPositingAwareSplit.class,
	
	//.structures.serialization
	TestFixedSizeTextFactory.class,
	
	//.terms
	TestTermPipelineAccessor.class,
	TestPorterStemmer.class,
	
	//.utility
	TestArrayUtils.class,
	TestDistance.class,
	TestHeapSort.class,
	TestRounding.class,
	TestStaTools.class,
	TestTermCodes.class,
	
	//utility.io
	TestRandomDataInputMemory.class,
	TestHadoopPlugin.class,
	TestCountingInputStream.class
})
public class TerrierDefaultTestSuite {}
