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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.osgi.service.event.Event;

import ddf.catalog.pubsub.criteria.entry.DadEvaluationCriteria;
import ddf.catalog.pubsub.criteria.entry.DadEvaluationCriteriaImpl;
import ddf.catalog.pubsub.criteria.entry.DadEvaluator;
import ddf.catalog.pubsub.criteria.entry.EntryEvaluationCriteria;
import ddf.catalog.pubsub.criteria.entry.EntryEvaluationCriteriaImpl;
import ddf.catalog.pubsub.criteria.entry.EntryEvaluator;
import ddf.catalog.pubsub.internal.PubSubConstants;


public class EntryPredicate implements Predicate 
{
	private String catalogId;
	private URI productUri;
	
	private static final Logger logger = Logger.getLogger( EntryPredicate.class );
	

	public EntryPredicate() 
	{
		this.catalogId = null;
		this.productUri = null;
	}
	
	
	public EntryPredicate( String catalogId ) 
	{
		this.catalogId = catalogId;
		this.productUri = null;
	}

    public EntryPredicate( URI dad )
    {
        this.catalogId = null;
        this.productUri = dad;
    }

	public boolean matches( Event properties ) 
	{
	    logger.trace("ENTERING: " + "EntryPredicate.matches");
		boolean status = false;
		
		Map<String, Object> contextualMap = (Map<String, Object>) properties.getProperty( PubSubConstants.HEADER_CONTEXTUAL_KEY );
        String operation = (String) properties.getProperty( PubSubConstants.HEADER_OPERATION_KEY );
        logger.debug( "operation = " + operation );
        
        if(contextualMap != null){
            String metadata = (String) contextualMap.get( "METADATA" );
    
            // If deleting a catalog entry and the entry's location data is NULL is only the word "deleted" (i.e., the
            // source is deleting the catalog entry and did not send any location data with the delete event), then
            // cannot apply any geospatial filtering - just send the event on to the subscriber
            if ( PubSubConstants.DELETE.equals( operation ) && PubSubConstants.METADATA_DELETED.equals( metadata ) )
            {
                logger.debug( "Detected a DELETE operation where metadata is just the word 'deleted', so send event on to subscriber" );
                logger.debug( "EXITING: matches" );
                return true;
            }
        }
		
		if ( catalogId != null )
		{
			EntryEvaluationCriteria eec = new EntryEvaluationCriteriaImpl( catalogId, 
					properties.getProperty( PubSubConstants.HEADER_ID_KEY ).toString() );
			
			status = EntryEvaluator.evaluate( eec );
		}
		else if ( productUri != null )
		{
			logger.debug( "Doing DAD matches" );
			
			String incomingProductUriString = (String)properties.getProperty( PubSubConstants.HEADER_DAD_KEY );
			URI incomingProductUri;
            try
            {
                incomingProductUri = new URI(incomingProductUriString);
                DadEvaluationCriteria dec = new DadEvaluationCriteriaImpl( productUri, 
                    incomingProductUri ); 
                
                status = DadEvaluator.evaluate( dec );
            }
            catch (URISyntaxException e)
            {
                logger.debug("Error comparing DADs");
                status = false;
            }
		}
		
        logger.debug("entry evaluation = " + status);

        logger.trace("EXITING: " + "EntryPredicate.matches");
        
        return status;
	}
	
	
	public String getCatalogId() 
	{
		return catalogId;
	}


	public URI getDad() 
	{
		return productUri;
	}


	public void setDad( URI productUri ) 
	{
		this.productUri = productUri;
	}


	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append( "\tcatalogId = " + catalogId + "\n" );
		sb.append( "\tdad = " + productUri + "\n" );
		
		return sb.toString();
	}
}
