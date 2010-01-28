/*
 * Smooth - Smoothing term frequency normalisation
 * Webpage: http://ir.dcs.gla.ac.uk/smooth
 * Contact: ben{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * Information Retrieval Group
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
 * The Original Code is ParameterTuning.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.smooth.tuning;

import uk.ac.gla.terrier.smooth.matching.BufferedMatching;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * An interface providing contract for implementing the
 * parameter tuning mechanism.
 * @author Ben He <ben{a.}dcs.gla.ac.uk>
 * @version $Revision: 1.1 $
 */
public abstract class ParameterTuning {
	/** The prefix of the name of a normalisation method's effect. */
	public final String nemethodPackagePrefix = "uk.ac.gla.terrier.smooth.normalisation.";
	/** The prefix of the name of the package of classes of normalisation method. */
	public final String terrierNormalilsationPrefix =
		"uk.ac.gla.terrier.models.normalisation.";
	/** The prefix of the name of a normalisation method. */
	public final String methodNamePrefix = "Normalisation";
	/**
	 * The average document length in the collection.
	 */
	protected double avl;
	/** The number of tokens in the collection. */
	protected double numberOfTokens;
	
	/** Indicate if it is running under debugging model. This correspondes to property
	 * <tt>debugging.mode</tt>. 
	 * */
	protected final boolean debugging = new Boolean(
			ApplicationSetup.getProperty("debugging.mode", "false")).booleanValue();
	/** The index used for assessing the terms' statistics. */
	public Index index;
	/** The matching used in the tuning process. */
	protected BufferedMatching matching;
	/** The lexicon used for assessing the terms' statistics.*/
	protected Lexicon lexicon;
	/**
	 * The default constructor.
	 * @param index The index used for assessing the terms' statistics.
	 */
	public ParameterTuning(Index index){
		this.index = index;
		if (debugging){
			System.err.println("index: " + index);
			System.err.println("terrier.index.path: " + ApplicationSetup.TERRIER_INDEX_PATH);
			System.err.println("lexicon.filename: " + ApplicationSetup.LEXICON_FILENAME);
		}
		this.lexicon = index.getLexicon();
		matching = new BufferedMatching(index);
	}
	public double getAvl() {
		return avl;
	}
	public void setAvl(double avl) {
		this.avl = avl;
	}
	
	public double getNumberOfTokens() {
		return numberOfTokens;
	}
	public void setNumberOfTokens(double numberOfTokens) {
		this.numberOfTokens = numberOfTokens;
	}
	/**
	 * The interface for implementing a tuning method using sampled queries.
	 * @param taskName The name of the retrieval task.
	 * @param numberOfSamples The number of sampled queries to simulate.
	 * @return The estimated parameter setting.
	 */
	abstract public double tuneSampling(String taskName, int numberOfSamples);
	/**
	 * The interface for implementing a tuning method using real TREC queries.
	 * @param taskName The name of the retrieval task.
	 * @return The estimated parameter setting.
	 */
	abstract public double tuneRealTRECQuery(String taskName);
	
}
