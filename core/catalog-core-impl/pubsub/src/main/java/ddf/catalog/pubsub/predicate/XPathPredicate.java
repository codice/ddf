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

package ddf.catalog.pubsub.predicate;

import org.osgi.service.event.Event;
import org.w3c.dom.Document;

import ddf.catalog.pubsub.criteria.contextual.XPathEvaluationCriteria;
import ddf.catalog.pubsub.criteria.contextual.XPathEvaluationCriteriaImpl;
import ddf.catalog.pubsub.criteria.contextual.XPathEvaluator;
import ddf.catalog.pubsub.internal.PubSubConstants;


public class XPathPredicate implements Predicate 
{
	private String xpath;
	

	public String getXpath() 
	{
		return xpath;
	}

	
	public XPathPredicate(String xpath) 
	{
		this.xpath = xpath;
	}

	
	public static boolean isXPath(String xpath) 
	{
		return !xpath.isEmpty();
	}

	
	public boolean matches(Event properties) 
	{
		XPathEvaluationCriteria xec = 
			new XPathEvaluationCriteriaImpl( (Document) properties.getProperty( PubSubConstants.HEADER_XPATH_KEY ), xpath );
		
		return XPathEvaluator.evaluate(xec);
	}


	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append( "\txpath = " + xpath + "\n" );
		
		return sb.toString();
	}
	
}
