/*
 * Created on 23 Apr 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.querying;

import uk.ac.gla.terrier.applications.GenoPassageExtraction;
import uk.ac.gla.terrier.structures.Index;

public class GenoPassageManager extends Manager {
	
	public GenoPassageExtraction genoPassageExtractor; 
	
	public GenoPassageManager(String sentWModelName, String qemodelName){
		super();
		this.setGenoPassageExtractor(sentWModelName, qemodelName);
	}
	
	public GenoPassageManager(String sentWModelName, String qemodelName, Index index){
		super(index);
		this.setGenoPassageExtractor(sentWModelName, qemodelName);
	}
	
	public void setGenoPassageExtractor(String sentWModelName, String qemodelName){
		genoPassageExtractor = new GenoPassageExtraction(sentWModelName, qemodelName);
	}
}
