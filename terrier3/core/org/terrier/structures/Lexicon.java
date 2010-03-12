/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://terrier.org/
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
 * The Original Code is Lexicon.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original contributor)
  *   Ben He <ben{a.}dcs.gla.ac.uk>
  *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package org.terrier.structures;
import java.io.Closeable;
import java.util.Map;

public abstract class Lexicon<KEY> implements Closeable, Iterable<Map.Entry<KEY,LexiconEntry>>
{
    static class LexiconFileEntry<KEY2> implements Map.Entry<KEY2,LexiconEntry>
	{
        KEY2 key;
        LexiconEntry value;
				
        public LexiconFileEntry(KEY2 k, LexiconEntry v)
        {
            this.key = k;
            this.value = v;
		}
			
        public int hashCode()
        {
            LexiconFileEntry<KEY2> e = this;
            return (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
            	(e.getValue()==null ? 0 : e.getValue().hashCode());
        }

        public LexiconEntry setValue(LexiconEntry v)
        {
        	LexiconEntry old = value;
            value = v;
            return old;
		}

        public KEY2 getKey()
        {
            return key;
		}

        public LexiconEntry getValue()
        {
            return value;    
        }

        @SuppressWarnings("unchecked")
		public boolean equals(Object o)
        {
            if (! (o instanceof Map.Entry))
					return false;
            LexiconFileEntry e1 = this;
            Map.Entry<String,LexiconEntry> e2 = (Map.Entry)o;
            return (e1.getKey()==null ?
              e2.getKey()==null : e1.getKey().equals(e2.getKey()))  &&
             (e1.getValue()==null ?
              e2.getValue()==null : e1.getValue().equals(e2.getValue()));
        }
	}

    public abstract int numberOfEntries();
    public abstract LexiconEntry getLexiconEntry(KEY term);
    public abstract Map.Entry<KEY,LexiconEntry> getLexiconEntry(int termid);
    public abstract Map.Entry<KEY,LexiconEntry> getIthLexiconEntry(int index);
}
