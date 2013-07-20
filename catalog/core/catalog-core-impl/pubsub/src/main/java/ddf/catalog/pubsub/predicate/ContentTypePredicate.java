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


import java.util.Map;

import org.apache.log4j.Logger;
import org.osgi.service.event.Event;

import ddf.catalog.pubsub.criteria.contenttype.ContentTypeEvaluationCriteriaImpl;
import ddf.catalog.pubsub.criteria.contenttype.ContentTypeEvaluator;
import ddf.catalog.pubsub.internal.PubSubConstants;


public class ContentTypePredicate implements Predicate 
{

    private static Logger logger = Logger.getLogger(ContentTypePredicate.class);
    private String type = null;
    private String version = null;

	
	public ContentTypePredicate( String type, String version) 
	{
	    if(type != null && !type.isEmpty())
	    {
	        this.type = type;
	    }
	    if(version != null && !version.isEmpty())
	    {
	        this.version = version;
	    }
	}

	
	public boolean matches( Event properties ) 
	{
	    logger.debug( "ENTERING: matches" );
	    
	    boolean status = false;
	    
        Map<String, Object> contextualMap = (Map<String, Object>) properties.getProperty( PubSubConstants.HEADER_CONTEXTUAL_KEY );
        String operation = (String) properties.getProperty( PubSubConstants.HEADER_OPERATION_KEY );
        logger.debug( "operation = " + operation );
        
        if(contextualMap != null){
            String metadata = (String) contextualMap.get( "METADATA" );
            logger.debug( "metadata = [" + metadata + "]" );

            // If deleting a catalog entry and the entry's location data is NULL is only the word "deleted" (i.e., the
            // source is deleting the catalog entry and did not send any location data with the delete event), then
            // cannot apply any geospatial filtering - just send the event on to the subscriber
            if ( PubSubConstants.DELETE.equals(operation) && PubSubConstants.METADATA_DELETED.equals(metadata) )
            {
                logger.debug( "Detected a DELETE operation where metadata is just the word 'deleted', so send event on to subscriber" );
                logger.debug( "EXITING: matches" );
                return true;
            }
        }
        
	    
	    Object inputContentType = properties.getProperty( PubSubConstants.HEADER_CONTENT_TYPE_KEY );
	    logger.debug("input obtained from event properties: " + inputContentType.toString());
	    if ( inputContentType != null )
	    {
    		ContentTypeEvaluationCriteriaImpl ctec = new ContentTypeEvaluationCriteriaImpl( this, 
    		    inputContentType.toString() );
    		
    		status = ContentTypeEvaluator.evaluate( ctec );
	    }
		
	    logger.debug( "EXITING: matches" );
	    
		return status;
	}


	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append( "\tcontentType = " + this.type + " version = " + this.version + "\n" );
		
		return sb.toString();
	}


    public void setType( String type )
    {
        this.type = type;
    }


    public String getType()
    {
        return type;
    }


    public void setVersion( String version )
    {
        this.version = version;
    }


    public String getVersion()
    {
        return version;
    }
}
