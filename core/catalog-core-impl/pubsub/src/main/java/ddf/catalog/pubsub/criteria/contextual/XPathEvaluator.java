/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

package ddf.catalog.pubsub.criteria.contextual;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;

import ddf.util.XPathHelper;



public class XPathEvaluator 
{
	
	public static boolean evaluate( XPathEvaluationCriteria xpathCriteria )	
	{
		Document document = xpathCriteria.getDocument();
		String xpath = xpathCriteria.getXPath();
		
		XPathHelper evaluator = new XPathHelper( document ) ;
		
		try 
		{
			return (Boolean) evaluator.evaluate( xpath, XPathConstants.BOOLEAN );
			
		} 
		catch ( XPathExpressionException e ) 
		{
			e.printStackTrace();
		} 
		
		return false ;
	}
	
}
