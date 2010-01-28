/**
 * 
 */
package uk.ac.gla.terrier.structures;

import java.util.StringTokenizer;

/**
 * @author ben
 *
 */
public class SVMLightFormat extends DataFormat {
	
	public SVMLightFormat(String _normalDelimiter){
		super(_normalDelimiter);
	}

	/* Convert entry to SVM light format.
	 * @see uk.ac.gla.terrier.structures.DataFormat#convertTo(java.lang.String, java.lang.String, java.lang.String[], java.lang.String[])
	 */
	@Override
	public String convertTo(String itemid, String label, String[] ids,
			String[] values) {
		StringBuilder buf = new StringBuilder();
		buf.append(label);
		int n = ids.length;
		for (int i=0; i<n; i++)
			buf.append(" "+ids[i]+":"+values[i]);
		return buf.toString();
	}

	/* Convert standard entry to SVM light format.
	 * @see uk.ac.gla.terrier.structures.DataFormat#convertTo(java.lang.String)
	 */
	@Override
	public String convertTo(String stdEntry) {
		String[] tokens = stdEntry.split(normalDelimiter);
		String itemid = tokens[0].trim();
		String label = tokens[1].trim();
		int idx = 2;
		int length = tokens.length;
		int n = length-2;
		String[] ids = new String[n];
		String[] values = new String[n];
		int counter = 0;
		while (idx<length){
			ids[counter] = ""+(counter+1);
			values[counter] = tokens[idx++];
			counter++;
		}
		return convertTo(itemid, label, ids, values);
	}

	/* (non-Javadoc)
	 * @see uk.ac.gla.terrier.structures.DataFormat#normalise(java.lang.String)
	 */
	@Override
	public String normalise(String entry) {
		String itemid = ""+System.currentTimeMillis();
		String[] tokens = entry.split(" ");
		String label = tokens[0];
		StringBuilder buf = new StringBuilder();
		buf.append(itemid+normalDelimiter+label);
		int length = tokens.length;
		for (int i=1; i<length; i++){
			String[] tuple = tokens[i].split(":");
			buf.append(normalDelimiter+tuple[0]+normalDelimiter+tuple[1]);
		}
		return buf.toString();
	}

}
