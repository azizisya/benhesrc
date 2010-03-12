package org.terrier.indexing;

import java.io.InputStream;

import org.junit.Test;


@SuppressWarnings("unchecked")
public class TestCollections {

	static Class<? extends Collection>[] COLLECTIONS = (Class<? extends Collection>[]) new Class<?>[]{
		TRECCollection.class,
		TRECUTFCollection.class,
		WARC018Collection.class,
		WARC09Collection.class
	};
	
	@Test public void testConstructor() throws Exception
	{
		for(Class<? extends Collection> c : COLLECTIONS)
		{
			c.getConstructor(InputStream.class);
		}
	}
	
}
