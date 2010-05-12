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
 * The Original Code is PerFieldNormWeightingModel.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 */
package org.terrier.matching.models;

import org.terrier.matching.models.basicmodel.BasicModel;
import org.terrier.matching.models.normalisation.Normalisation;
import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.EntryStatistics;
import org.terrier.structures.FieldLexiconEntry;
import org.terrier.structures.postings.FieldPosting;
import org.terrier.structures.postings.Posting;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.ArrayUtils;
import org.terrier.utility.StaTools;

/** 
 * A class for generating arbitrary per-field normalisation models.
 * Per-field normalisation models are field-based models - i.e.
 * they take into account the frequency of a term in each individual
 * field of the document. In particular, they apply length normalisation
 * on the frequency from each field, before using a linear combination
 * of term frequencies to garner the final frequency.
 * <p><b>Properties:</b></p>
 * <ul>
 * <li>w.fieldId (starting at 0)</li>
 * <li>c.fieldId (starting at 0)</li>
 * @author Craig Macdonald
 * @since 3.0
 */
public class PerFieldNormWeightingModel extends WeightingModel {
	private static final long serialVersionUID = 1L;
	String[] params;
	BasicModel basicModel;
	Normalisation[] fieldNormalisations;
	
	int[] fieldGlobalFrequencies;
	double[] fieldWeights;
	int fieldCount;
	
	Class<? extends Normalisation> normClass;
	
	public PerFieldNormWeightingModel(
			Class<? extends BasicModel> _basicModel,
			Class<? extends Normalisation> _normalisationModel) throws Exception
	{
		this.params = new String[]{_basicModel.getSimpleName(), _normalisationModel.getSimpleName()};
		this.basicModel = _basicModel.newInstance();
		this.normClass = _normalisationModel;
	}
	
	public PerFieldNormWeightingModel(String[] parameters) throws Exception
	{
		this.params = parameters;
		this.basicModel = Class.forName(parameters[0]).asSubclass(BasicModel.class).newInstance();
		this.normClass = Class.forName(parameters[1]).asSubclass(Normalisation.class);
	}

	@Override
	public double score(Posting _p) {
		FieldPosting p = (FieldPosting)_p;
		final int[] tff = p.getFieldFrequencies();
		final int[] fieldLengths = p.getFieldLengths();
		final double[] normFieldFreqs = new double[fieldCount];
		for(int i=0;i<fieldCount;i++)
		{
			normFieldFreqs[i] = fieldWeights[i] * fieldNormalisations[i].normalise(tff[i], fieldLengths[i], fieldGlobalFrequencies[i]);
		}
		final double tf = StaTools.sum(normFieldFreqs);
		return basicModel.score(tf, super.documentFrequency, super.termFrequency, super.keyFrequency, p.getDocumentLength());
	}

	@Override
	public void setBackgroundStatistics(CollectionStatistics _cs) 
	{
		super.setBackgroundStatistics(_cs);
		fieldCount = _cs.getNumberOfFields();
		if (fieldCount < 1)
			throw new IllegalStateException("Fields must be 1 or more");
				
		basicModel.setNumberOfDocuments(_cs.getNumberOfDocuments());
		basicModel.setNumberOfTokens(_cs.getNumberOfTokens());
		
		fieldNormalisations = new Normalisation[fieldCount];
		fieldGlobalFrequencies  = new int[fieldCount];
		fieldWeights = new double[fieldCount];
		try
		{			
			for(int fi=0;fi<fieldCount;fi++)
			{
				fieldWeights[fi] = Double.parseDouble(ApplicationSetup.getProperty("w."+ fi, ""+1.0));
				Normalisation nf = this.fieldNormalisations[fi] = normClass.newInstance();
				final double param = Double.parseDouble(ApplicationSetup.getProperty("c."+ fi, ""+1.0));
				nf.setParameter(param);
				nf.setNumberOfDocuments(_cs.getNumberOfDocuments());
				final long tokensf = _cs.getFieldTokens()[fi];
				nf.setNumberOfTokens(tokensf);
				nf.setAverageDocumentLength(_cs.getAverageFieldLengths()[fi]);				
			}	
		} catch (Exception e) {
			throw new IllegalStateException(e);			
		}
	}

	@Override
	public void setEntryStatistics(EntryStatistics _es) {
		super.setEntryStatistics(_es);
		if (! (_es instanceof FieldLexiconEntry))
			return;
		FieldLexiconEntry fes = (FieldLexiconEntry)_es;
		fieldGlobalFrequencies = fes.getFieldFrequencies();
		for(int i=0;i<fieldCount;i++)
		{
			fieldNormalisations[i].setDocumentFrequency(es.getDocumentFrequency());
		}
	}
	
	@Override
	public String getInfo() {
		StringBuilder s = new StringBuilder();
		s.append(this.getClass().getSimpleName());
		s.append('(');
		s.append(basicModel.getInfo());
		for(Normalisation n : fieldNormalisations)
		{
			s.append("Norm");
			s.append(n.getInfo());
			s.append(',');
		}
		s.append("w=");
		s.append(ArrayUtils.join(fieldWeights, ","));
		return s.toString();
	}


	@Override
	public double score(double tf, double docLength) {
		return 0;
	}

	@Override
	public double score(double tf, double docLength, double n_t, double F_t,
			double keyFrequency) {
		return 0;
	}

}
