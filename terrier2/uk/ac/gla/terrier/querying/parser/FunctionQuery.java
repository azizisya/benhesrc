package uk.ac.gla.terrier.querying.parser;

import java.lang.reflect.Constructor;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier;
import uk.ac.gla.terrier.matching.dsms.FunctionScoreModifier;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.Tuple;

/**
 * Class for modeling function queries, special constructs aimed at allowing
 * the application of a specific document score modifier (the function) over a
 * subset of query terms (the arguments for the function).
 * 
 * @author rodrygo
 */
public class FunctionQuery extends MultiTermQuery {

	private static final long serialVersionUID = 1L;
	
	private String fName = null;
	private double weight = 1.0d;
	
	//private Lexicon lexicon = Index.createIndex().getLexicon();
	
	/**
	 * Default constructor.
	 */
	public FunctionQuery() {
		
	}
	
	/**
	 * Creates a FunctionQuery object to handle calls to the given class.
	 * 
	 * @param fName The name of the class that implements this function.
	 * @param weight The weight to be applied to this function.
	 */
	public FunctionQuery(String fName, double weight) {
		this.fName = fName;
		this.weight = weight;
	}
	
	public String getFName() {
		return fName;
	}
	
	public void setFName(String fName) {
//		System.err.println("  " + fName);
		this.fName = fName;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		System.out.println("f:" + weight);
		this.weight = weight;
	}
	
	/**
	 * @return Returns the arguments to the function as an array of strings.
	 */
	public Tuple[] getArguments() {
		int numTuples = super.v.size();
		Tuple[] tuples = new Tuple[numTuples];
		
		// for each tuple argument for this function (a explicit MultiTermQuery)
		for (int i = 0; i < numTuples; i++) {
			MultiTermQuery mtq = (MultiTermQuery) super.v.get(i);
			
			int numTerms = mtq.v.size();
			ExpansionTerm[] expansionTerms = new ExpansionTerm[numTerms];
			
			// for each term in this tuple (i.e., a SingleTermQuery)
			for (int j = 0; j < numTerms; j++) {
				SingleTermQuery stq = (SingleTermQuery) mtq.v.get(j);
				
				//lexicon.findTerm(stq.getTerm());
				expansionTerms[j] = new ExpansionTerm(-1, stq.getWeight());
				expansionTerms[j].setToken(stq.getTerm());
			}
			
			tuples[i] = new Tuple(expansionTerms);
			tuples[i].setWeight(mtq.getWeight());
		}
				
		return tuples;
	}
	
	/**
	 * Overrides MultiTermQuery.obtainQueryTerms so that the
	 * DocumentScoreModifier implementing this function is added to
	 * MatchingQueryTerms but not the arguments for this function, which should
	 * not be matched as ordinary query terms.
	 * 
	 * @param terms The matching terms
	 */
	public void obtainQueryTerms(MatchingQueryTerms terms) {
		String name = "uk.ac.gla.terrier.matching.dsms." + fName;
		
		try {
			//class
			Class<?> cls = Class.forName(name);
			
			if (!FunctionScoreModifier.class.isAssignableFrom(cls)) {
				throw new IllegalArgumentException("Invalid FunctionScoreModifier: " + fName);
			}
			
			//parameter types: String[] terms, double weight
			Class<?> params[] = new Class<?>[] { Tuple[].class, double.class };
			//constructor
			Constructor<?> ct = cls.getConstructor(params);
			//take function arguments as the DSM constructor arguments
			Object[] args = new Object[] { this.getArguments(), weight };
			//instantiate function
			DocumentScoreModifier dsm = (DocumentScoreModifier) ct.newInstance(args);
			
			//add document score modifier
			terms.addDocumentScoreModifier(dsm);
//			System.out.println("  >>>> ADDED FunctionDSM: " + fName);
		} catch (Exception e) {
			System.err.println("Could not instantiate function " + name);
			e.printStackTrace();
		}
		
	}

	public Object clone() {
		return new FunctionQuery(fName, weight);
	}
	
	public String toString() {
		String str = fName + " [ " + super.toString() + " ] ";		
		if (weight != 1.0d) {
			str += "^" + weight;
		}		
		return str;
	}
	
}
