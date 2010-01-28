package uk.ac.gla.terrier.matching.models.queryexpansion;

import uk.ac.gla.terrier.matching.models.Idf;

/** 
 * This class implements the Kullback-Leibler divergence for
 * query expansion. See G. Amati's PhD Thesis.
 * @author Gianni Amati, Ben He
 * @version $Revision: 1.1 $
 */
public class Information extends QueryExpansionModel {
    /** A default constructor.*/
    public Information() {
		super();
    }
    
    /**
     * Returns the name of the model.
     * @return the name of the model
     */
    public final String getInfo() {
		return "Information";
    }
    /**
     * This method computes the normaliser of parameter-free query expansion.
     * @return The normaliser.
     */
    public final double parameterFreeNormaliser(){	
    	return (maxTermFrequency) * Math.log(collectionLength/totalDocumentLength)/
			(Math.log(2d)*totalDocumentLength);
		//return  maxTermFrequency * idf.log(collectionLength/totalDocumentLength)/ idf.log (totalDocumentLength);
	}
	
    /**
     * This method computes the normaliser of parameter-free query expansion.
     * @param maxTermFrequency The maximum of the term frequency of the query terms.
     * @param collectionLength The number of tokens in the collections.
     * @param totalDocumentLength The sum of the length of the top-ranked documents.
     * @return The normaliser.
     */
	public final double parameterFreeNormaliser(double maxTermFrequency, double collectionLength, double totalDocumentLength){
		return (maxTermFrequency) * Math.log(collectionLength/totalDocumentLength)/
		(Math.log(2d)*totalDocumentLength);
	}
    
    /** This method implements the query expansion model.
     *  @param withinDocumentFrequency double The term frequency in the X top-retrieved documents.
     *  @param termFrequency double The term frequency in the collection.
     *  @return double The query expansion weight using he complete 
     *          Kullback-Leibler divergence.
     */
    public final double score(double withinDocumentFrequency, double termFrequency) {
		return - Idf.log(withinDocumentFrequency / this.totalDocumentLength );
    } 
    
    /**
	 * This method implements the query expansion model.
	 * @param withinDocumentFrequency double The term frequency in the X top-retrieved documents.
     * @param termFrequency double The term frequency in the collection.
     * @param totalDocumentLength double The sum of length of the X top-retrieved documents.
     * @param collectionLength double The number of tokens in the whole collection.
     * @param averageDocumentLength double The average document length in the collection.
	 * @return double The score returned by the implemented model.
	 */
	public final double score(
        double withinDocumentFrequency, 
        double termFrequency,
        double totalDocumentLength, 
        double collectionLength, 
        double averageDocumentLength
    ){
		return - Idf.log(withinDocumentFrequency / this.totalDocumentLength);
    }
}
