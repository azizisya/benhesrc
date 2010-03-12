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
 * The Original Code is SimpleJettyHTTPServer.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.utility;

import java.io.IOException;

import org.mortbay.http.HttpContext;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.jetty.Server;


/** Class to make a simple Jetty servlet. Two arguments: port name, and webapps root path.
 * <tt>share/images</tt> is automatically added as /images.
 * @author craigm
 * @since 3.0
 */
public class SimpleJettyHTTPServer {

	private SocketListener listener;
	protected Server webserver;	
	
	public SimpleJettyHTTPServer(String bindAddress, int port, String webappRoot) throws IOException 
	{
		webserver = new Server();
		listener = new SocketListener();
	    listener.setPort(port);
	    if (bindAddress != null)
	    	listener.setHost(bindAddress);
	    webserver.addListener(listener);
	    webserver.addWebApplication("/", webappRoot);
	    
	    HttpContext imagesContext = new HttpContext();
	    imagesContext.setContextPath("/images/*");
	    imagesContext.setResourceBase(ApplicationSetup.TERRIER_SHARE + "/images/");
	    imagesContext.addHandler(new ResourceHandler());
	    webserver.addContext(imagesContext);
	}
	
	public void start() throws Exception {
		webserver.start();
	}
	
	public void stop() throws InterruptedException {
	    webserver.stop();
	}
	
	public static void main(String[] args) throws Exception
	{
		if (args.length != 2)
		{
			System.err.println("Usage: SimpleJettyHTTPServer port src/webapps/simple/");
			return;
		}
		new SimpleJettyHTTPServer(null, Integer.parseInt(args[0]), args[1]).start();
	}
	
}
