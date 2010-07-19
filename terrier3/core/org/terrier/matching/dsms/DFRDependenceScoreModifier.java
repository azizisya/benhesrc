/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
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
 * The Original Code is DependenceScoreModifier.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 *   Jie Peng <pj{a.}dcs.gla.ac.uk>
 */
package org.terrier.matching.dsms;


import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;

import uk.ac.gla.terrier.statistics.GammaFunction;

/**
 * Implements the pBiL and pBil2 DFR-based dependence models. For more information, see 
 * Incorporating Term Dependency in the DFR Framework.
 * Jie Peng, Craig Macdonald, Ben He, Vassilis Plachouras, Iadh Ounis. 
 * In Proceedings of SIGIR 2007. July 2007. Amsterdam, the Netherlands. 2007. 
 * <p>
 * <b>Properties</b>
 * <ul>
 * <li><i>See properties for DependenceScoreModifier</i></li>
 * <li><tt>proximity.norm2</tt> - should Normalisation2 be applied?, defaults to false</li>
 * <li><tt>proximity.norm2.c</tt> - c value for Normalisation2, defaults to 1.0</li>
 * </ul>
 * 
 * @author Vassilis Plachouras, Jie Peng, Craig Macdonald
 * @version $Revision: 1.3 $
 */
public class DFRDependenceScoreModifier extends DependenceScoreModifier {
	
	protected static final double REC_LOG_2 = 1.0d / Math.log(2.0d);
	protected static final GammaFunction gf = new GammaFunction();
	
	/** whether to apply Normalisation 2 */
	protected boolean norm2 = Boolean.parseBoolean(ApplicationSetup
			.getProperty("proximity.norm2", "false"));
	protected double ngramC;
	
	public DFRDependenceScoreModifier() {
	}

	public DFRDependenceScoreModifier(final String[] pTerms) {
		phraseTerms = pTerms;
	}

	public DFRDependenceScoreModifier(final String[] pTerms, boolean r) {
		this(pTerms);
	}

	

	@Override
	public boolean modifyScores(Index index, MatchingQueryTerms terms,
			ResultSet set) {
		ngramC = Double.parseDouble(ApplicationSetup.getProperty("proximity.norm2.c", "1.0d"));
		return super.modifyScores(index, terms, set);
	}

	@Override
	protected double scoreFDSD(int matchingNGrams, int docLength)
	{
		if (matchingNGrams == 0)
			return 0.0d;
		final int numberOfNGrams = (docLength > 0 && docLength < ngramLength) ? 1
				: docLength - ngramLength + 1;
		
		double score = 0.0d;
		
		// apply Norm2 to pf?
		double matchingNGramsNormalised = norm2 ? matchingNGrams
				* Math.log(1 + ngramC * avgDocLen / numberOfNGrams)
				* REC_LOG_2 : matchingNGrams;

				double background = norm2 ? avgDocLen : numberOfNGrams;

				double p = 1.0D / background;
				double q = 1.0d - p;
				score = -gf.compute_log(background + 1.0d)
				* REC_LOG_2
				+ gf.compute_log(matchingNGramsNormalised + 1.0d)
				* REC_LOG_2
				+ gf.compute_log(background - matchingNGramsNormalised
						+ 1.0d)
						* REC_LOG_2
						- matchingNGramsNormalised * Math.log(p)
						* REC_LOG_2
						- (background - matchingNGramsNormalised) * Math.log(q)
						* REC_LOG_2;
		score = score / (1.0d + matchingNGramsNormalised);
		return score;
	}

}
