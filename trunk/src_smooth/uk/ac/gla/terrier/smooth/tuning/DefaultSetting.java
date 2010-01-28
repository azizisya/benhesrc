/*
 * Smooth - Smoothing term frequency normalisation
 * Webpage: http://ir.dcs.gla.ac.uk/smooth
 * Contact: ben{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * Information Retrieval Group
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
 * The Original Code is DefaultSetting.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.smooth.tuning;

import uk.ac.gla.terrier.utility.ApplicationSetup;

// NP

// body, increase, minimise, 0.9312
// title, decrease, minimise,0.9544

/**
 * This class provides functionalities for accessing the default setting of
 * term frequency normalisation parameter tuning by measuring the correlation
 * of term frequency with document length.
 * @author Ben He <ben{a.}dcs.gla.ac.uk>
 * @version $Revision: 1.1 $
 */
public class DefaultSetting {
	/**
	 * Get a boolean variable indicating if the correlation function 
	 * is an increasing or a decreasing function.
	 * @param methodName The name of the normalisation method.
	 * @param taskName The name of the retrieval task.
	 * @return True for an increasing function and false for a 
	 * decreasing function.
	 */
	public static boolean getCorrelationFunctionBehavior(String methodName, String taskName){
		boolean increasing = true;
		if (!ApplicationSetup.getProperty("tune.increase", "").equals("")){
		  increasing = new Boolean(ApplicationSetup.getProperty("tune.increase", "")).booleanValue(); 
        }
		else if (methodName.startsWith("2")){
			if (taskName.equalsIgnoreCase("adhoc"))
				increasing = true;
			else if (taskName.equalsIgnoreCase("td"))
				increasing = true;
		}
		else if (methodName.startsWith("B")){
			if (taskName.equalsIgnoreCase("adhoc"))
				increasing = false;
			else if (taskName.equalsIgnoreCase("td"))
				increasing = false;
		}
		else if (methodName.startsWith("3")){
			if (taskName.equalsIgnoreCase("adhoc"))
				increasing = true;
			else if (taskName.equalsIgnoreCase("td"))
				increasing = true;
		}
		return increasing;
	}
	/**
	 * Get a boolean variable indicating if the derivative of
	 * the correlation function is an increasing function or a
	 * decreasing function.
	 * @param methodName The name of the normalisation method.
	 * @param taskName The name of the retrieval task.
	 * @return True for an increasing derivative function and false
	 * for a decreasing derivative function.
	 */
	public static boolean getCorrelationFunctionProperty(String methodName, String taskName){
		boolean minimise = true;
		if (!ApplicationSetup.getProperty("tune.minimise", "").equals("")){
		  minimise = new Boolean(ApplicationSetup.getProperty("tune.minimise", "")).booleanValue(); 
        }
		else if (methodName.startsWith("2")){
			if (taskName.equalsIgnoreCase("adhoc"))
				minimise = true;
		}
		else if (methodName.startsWith("B")){
			if (taskName.equalsIgnoreCase("adhoc"))
				minimise = true;
		}
		else if (methodName.startsWith("3")){
			if (taskName.equalsIgnoreCase("adhoc"))
				minimise = true;
		}
		return minimise;
	}
	/**
	 * Get the default bound of possible parameter values for the
	 * given normalisation method and task.
	 * @param methodName The name of the normalisation method.
	 * @param taskName The name of the retrieval task.
	 * @return 
	 */
	public static double[] getDefaultBound(String methodName, String taskName){
		double left = 0.01;
		double right = 128;
		double leftBoundary = 0.0001;
		double rightBoundary = 1024;
		if (methodName.startsWith("2")){
			if (taskName.equalsIgnoreCase("adhoc")){
				left = 0.01;
				right = 1024;
				leftBoundary = 0.0001;
				rightBoundary = 1024;
			}
			else if (taskName.equalsIgnoreCase("td")){
				left = 0.0001;
				right = 64;
				leftBoundary = 0.000001;
				rightBoundary = 1024;
			}
				
		}
		else if (methodName.startsWith("B")){
			if (taskName.equalsIgnoreCase("adhoc")){
				left = 0.01;
				right = 1;
				leftBoundary = 0.0001;
				rightBoundary = 1;
			}
			else if (taskName.equalsIgnoreCase("td")){
				left = 0.01;
				right = 1;
				leftBoundary = 0.0001;
				rightBoundary = 1;
			}
		}
		else if (methodName.startsWith("3")){
			if (taskName.equalsIgnoreCase("adhoc")){
				left = 0.01;
				right = 100000;
				leftBoundary = 0.01;
				rightBoundary = 100000;
			}
			else if (taskName.equalsIgnoreCase("td")){
				left = 0.01;
				right = 10000;
				leftBoundary = 0.01;
				rightBoundary = 100000;
			}
		}
		double[] boundary = {left, right, leftBoundary, rightBoundary};
		return boundary;
	}
	/**
	 * Get the optimal correlation for the given normalistion
	 * method and retrieval task.
	 * @param methodName The name of the normalisation method.
	 * @param taskName The name of the retrieval task.
	 * @return The default optimal correlation.
	 */
	public static double getTargetCorrelation(String methodName, String taskName){
		double rho = 0;
		if (methodName.startsWith("2")){
			if (taskName.equalsIgnoreCase("adhoc"))
				//rho = -0.0951;
				rho = -0.0868;
			else if (taskName.equalsIgnoreCase("td"))
				//rho = -0.2944;
				rho = -0.2892;
		}
		else if (methodName.startsWith("B")){
			if (taskName.equalsIgnoreCase("adhoc"))
				//rho = -0.0895;
				rho = -0.0868;
			else if (taskName.equalsIgnoreCase("td"))
				//rho =  -0.2769;
				rho = -0.2892;
		}
		else if (methodName.startsWith("3")){
			if (taskName.equalsIgnoreCase("adhoc"))
				//rho = -0.0759;
				rho = -0.0868;
			else if (taskName.equalsIgnoreCase("td"))
				//rho = -0.2964;
				rho = -0.2892;
		}
		return Double.parseDouble(ApplicationSetup.getProperty("target.correlation", ""+rho));
	}
	/**
	 * Get the default parameter setting for a given normalisation method
	 * for a given retrieval task.
	 * @param methodName The name of the normalisation method.
	 * @param taskName The name of the retrieval task.
	 * @return The default parameter setting.
	 */
	public static double getDefaultSetting(String methodName, String taskName){
		double parameter = 0;
		if (methodName.startsWith("2")){
			if (taskName.equalsIgnoreCase("adhoc"))
				parameter = 7d;
			else if (taskName.equalsIgnoreCase("td"))
				parameter = 0.1d;
		}
		else if (methodName.startsWith("B")){
			if (taskName.equalsIgnoreCase("adhoc"))
				parameter = 0.35d;
			else if (taskName.equalsIgnoreCase("td"))
				parameter = 0.8d;
		}
		else if (methodName.startsWith("3")){
			if (taskName.equalsIgnoreCase("adhoc"))
				parameter = 800d;
			else if (taskName.equalsIgnoreCase("td"))
				parameter = 130d;
		}
		return parameter;
	}
}
