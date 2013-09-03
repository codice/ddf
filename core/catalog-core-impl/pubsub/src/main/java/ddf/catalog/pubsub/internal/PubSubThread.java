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

package ddf.catalog.pubsub.internal;

import org.apache.log4j.Logger;
import org.osgi.service.event.EventAdmin;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.event.EventProcessor;
import ddf.catalog.pubsub.EventProcessorImpl;


public class PubSubThread extends Thread 
{
	private Metacard entry;
	private String topic;
	Logger logger = Logger.getLogger(PubSubThread.class);
	private EventAdmin eventAdmin;

	
	public PubSubThread( Metacard entry, String topic, EventAdmin eventAdmin ) 
	{
		this.entry = entry;
		this.topic = topic;
		this.eventAdmin	= eventAdmin;
	}
	
	
	public void run() 
	{
		logger.debug( "Processing entry event in separate thread - topic = " + topic );
		
		if ( topic.equals( EventProcessor.EVENTS_TOPIC_CREATED ) ) 
		{
			EventProcessorImpl.processEntry( entry, PubSubConstants.CREATE, eventAdmin );
			//new EventProcessorImpl().processEntry( entry, PubSubConstants.CREATE, eventAdmin );
		} 
		else if ( topic.equals( EventProcessor.EVENTS_TOPIC_UPDATED ) ) 
		{
			EventProcessorImpl.processEntry( entry, PubSubConstants.UPDATE, eventAdmin );
			//new EventProcessorImpl().processEntry( entry, PubSubConstants.UPDATE, eventAdmin );
		} 
		else if ( topic.equals( EventProcessor.EVENTS_TOPIC_DELETED ) ) 
		{
			EventProcessorImpl.processEntry( entry, PubSubConstants.DELETE, eventAdmin );
			//new EventProcessorImpl().processEntry( entry, PubSubConstants.DELETE, eventAdmin );
		}
	}
	
}
