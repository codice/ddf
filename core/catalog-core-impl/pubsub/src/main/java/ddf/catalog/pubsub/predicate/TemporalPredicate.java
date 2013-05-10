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

import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.osgi.service.event.Event;

import ddf.catalog.data.Metacard;
import ddf.catalog.pubsub.EventProcessorImpl.DateType;
import ddf.catalog.pubsub.criteria.temporal.TemporalEvaluationCriteria;
import ddf.catalog.pubsub.criteria.temporal.TemporalEvaluationCriteriaImpl;
import ddf.catalog.pubsub.criteria.temporal.TemporalEvaluator;
import ddf.catalog.pubsub.internal.PubSubConstants;


public class TemporalPredicate implements Predicate 
{
	private Date end;
	private Date start;
	private long offset;
	private DateType type;
	private static Logger logger = Logger.getLogger(TemporalPredicate.class);
	
	/**
	 * Instantiates a new temporal predicate.
	 * 
	 * @param start
	 *            the start date
	 * @param end
	 *            the end date
	 * @param type
	 *            What date
	 */
	public TemporalPredicate(Date start, Date end, DateType type) 
	{
		this.end = end;
		this.start = start;
		this.offset = 0;
		this.type = type;
	}
	
	
	public TemporalPredicate(long offset, DateType type) 
    {
        this.offset = offset;
        this.start = null;
        this.end = null;
        this.type = type;
    }

	
	public boolean matches(Event properties) 
	{
	    logger.debug("ENTERING: matches");
	    
		TemporalEvaluationCriteria tec = null;
		Date date = null;
		
        Map<String, Object> contextualMap = (Map<String, Object>) properties.getProperty( PubSubConstants.HEADER_CONTEXTUAL_KEY );
        String operation = (String) properties.getProperty( PubSubConstants.HEADER_OPERATION_KEY );
        logger.debug( "operation = " + operation );
        String metadata = (String) contextualMap.get( "METADATA" );
        logger.debug( "metadata = [" + metadata + "]" );

        // If deleting a catalog entry and the entry's location data is NULL is only the word "deleted" (i.e., the
        // source is deleting the catalog entry and did not send any location data with the delete event), then
        // cannot apply any geospatial filtering - just send the event on to the subscriber
        if ( operation.equals( PubSubConstants.DELETE ) && metadata.equals( PubSubConstants.METADATA_DELETED ) )
        {
            logger.debug( "Detected a DELETE operation where metadata is just the word 'deleted', so send event on to subscriber" );
            return true;
        }

		Metacard entry = (Metacard) properties.getProperty( PubSubConstants.HEADER_ENTRY_KEY );
		logger.debug("entry id: " + entry.getId());
		if (entry != null) {
			
		    switch(this.type)
		    {
		        case modified:
		            logger.debug("search by modified: " + entry.getModifiedDate());
		            date = entry.getModifiedDate();
		            break;
		        case effective:
		            logger.debug("search by effective: " + entry.getEffectiveDate());
		            date = entry.getEffectiveDate();
		            break;
		        case created: 
		            // currently searches by createdDate not supported by endpoints
		            logger.debug("search by created: " + entry.getCreatedDate());
		            date = entry.getCreatedDate();
		            break;
                case expiration:
                    // currently searches by expirationDate not supported by endpoints
                    logger.debug("search by expiration: " + entry.getExpirationDate());
                    date = entry.getExpirationDate();
                    break;
		    }
		    
		    if ( offset > 0 )
		    {
		        this.end = new Date();
	            long startTimeMillis = end.getTime() - offset;
	            this.start = new Date(startTimeMillis);
	            
	            logger.debug("time period lowerBound = " + start);
	            logger.debug("time period upperBound = " + end);
		    }
		    tec = new TemporalEvaluationCriteriaImpl(end, start, date);
		}

		logger.debug("EXITING: matches");
		
		return TemporalEvaluator.evaluate(tec);
	}

	
	public static boolean isTemporal(String startXML, String endXML) 
	{
		return !startXML.isEmpty() && !endXML.isEmpty();
	}
	

	public Date getEnd() 
	{
		return end;
	}

	
	public Date getStart() 
	{
		return start;
	}
	
	
	public DateType getType() 
	{
		return type;
	}


	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		
		if ( start != null )
		{
		    sb.append( "\tstart = " + start.toString() + "\n" );
		}
		
		// end date can be null for non-absolute (i.e., MODIFIED) criteria
		if ( end != null ) 
		{
			sb.append( "\tend = " + end.toString() + "\n" );
		}

        sb.append( "\toffset = " + Long.toString(offset) + "\n" );		
		sb.append( "\t(DateType) type = " + type.toString() + "\n" );
		
		return sb.toString();
	}

}
