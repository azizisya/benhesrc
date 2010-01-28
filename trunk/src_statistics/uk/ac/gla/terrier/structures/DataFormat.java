package uk.ac.gla.terrier.structures;

public abstract class DataFormat {
	public String normalDelimiter = "|";
	
	public DataFormat(String _normalDelimiter){
		this.normalDelimiter = _normalDelimiter;
	}
	
	/**
	 * Convert from data to entry
	 * @param itemid
	 * @param label
	 * @param ids
	 * @param values
	 * @return
	 */
	public abstract String convertTo(String itemid, String label, String[] ids, String[] values);
	
	/**
	 * Convert from the standard data format to local entry.
	 * @param stdEntry
	 * @return
	 */
	public abstract String convertTo(String stdEntry);
	
	/**
	 * Standard data format:
	 * itemid,label,id1,value1,id2,value2... 
	 * @param entry
	 * @return
	 */
	public abstract String normalise(String entry);
}
