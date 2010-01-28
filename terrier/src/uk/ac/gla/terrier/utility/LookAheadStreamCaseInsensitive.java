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
 * The Original Code is LookAheadStreamCaseInsensitive.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.utility;
import java.io.InputStream;
import java.io.IOException;
/** Version of LookAheadStream that is case-insensitive
 * @author Craig Macdonald
 * @since 2.1
 * @version $Revision: 1.1 $ */
public class LookAheadStreamCaseInsensitive extends LookAheadStream
{
	/** Create a LookAheadStream that is case insensitive
	  * @param parent The InputStream to wrap
	  * @param endMarker the marker at which to give EOF
	  */
	public LookAheadStreamCaseInsensitive(InputStream parent, String endMarker) {
		super(parent, endMarker.toUpperCase());
	}
    /**
     * Read a character from the parent stream, first checking that
     * it doesnt form part of the end marker.
     * @return int the code of the read character, or -1 if the end of
     *       the stream has been reached.
     * @throws IOException if there is any error while reading from the stream.
     */
    public int read() throws IOException {
        if (EOF)
            return -1;
        if (BufLen > 0) {
            BufLen--;
            return Buffer[BufIndex++];
        }
        int c = -1;
        boolean keepReading = true;
        while (keepReading) {
            if ((c = ParentStream.read()) == -1)
            {
                EOF = true;
                return -1;
            }
            char cc = Character.toUpperCase((char)c);
            if (cc == EndMarker[BufLen]) {
                Buffer[BufLen++] = cc;
                if (BufLen == MarkerLen) {
                    EOF = true;
                    return -1;
                }
            } else {
                Buffer[BufLen++] = cc;
                BufIndex = 0;
                //keepReading = false;
                break;
            }
        }
        BufLen--;
        return Buffer[BufIndex++];
    }
}
