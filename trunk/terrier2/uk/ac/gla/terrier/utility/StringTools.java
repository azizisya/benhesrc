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
 * The Original Code is StringTools.java.
 *
 * The Original Code is Copyright (C) 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}.dcs.gla.ac.uk/>
 */
package uk.ac.gla.terrier.utility;

import java.io.UnsupportedEncodingException;
/**
 * This class implements useful string functions
 */
public class StringTools {

	/** Returns how long String s is in bytes, if encoded in UTF-8
	  * @param s The string to be measured.
	  * @return The number of bytes s is when encoded in UTF-8
	  */
	public static int utf8_length(String s)
	{
		try{
			return s.getBytes("UTF-8").length;	
		}catch(UnsupportedEncodingException uce){
			//this should never happen, as UTF-8 is always supported
			return s.length();
		}
	}

	/** Normalises several common encodings found, for instance in HTTP or HTML headers,
	  * into the compatible Java encoding */
	public static String normaliseEncoding(String encodingName)
	{
		if(encodingName == null) return null;

		if(encodingName.toLowerCase().startsWith("x-mac-")){
			String tmp = encodingName.substring(6);
			String first = tmp.substring(0, 1);
			tmp = tmp.substring(1);
            //e.g. convert 'x-mac-roman' to 'x-MacRoman'
			encodingName = "x-Mac"+first.toUpperCase()+tmp;
        	return encodingName;
		}
		encodingName = encodingName.toUpperCase();
		//from EuroGOVCollection
        if (encodingName.startsWith("WINDOWS"))
        {
            if (encodingName.indexOf("_") > 0)
            {
                encodingName = encodingName.replaceFirst("^WINDOWS_","WINDOWS-");
            }
            else if (encodingName.indexOf("-") == -1)
            {
                encodingName = encodingName.replaceFirst("^WINDOWS", "WINDOWS-");
            }
        }
        else if (encodingName.startsWith("WIN"))
        {
            encodingName = encodingName.replaceFirst("^WIN(-|_)?","WINDOWS-");
        }
		return encodingName;
	}
}
