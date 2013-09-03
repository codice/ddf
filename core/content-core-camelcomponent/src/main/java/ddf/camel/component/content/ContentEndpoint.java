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
package ddf.camel.component.content;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ContentEndpoint extends DefaultEndpoint
{
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ContentEndpoint.class);
    

    public ContentEndpoint(String uri, ContentComponent component)
    {
        super(uri, component);
        LOGGER.debug("INSIDE CamelContentEndpoint(uri, component)");
        setSynchronous(true);
    }

    
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.camel.impl.DefaultEndpoint#getComponent()
     */
    public ContentComponent getComponent() 
    {
        return (ContentComponent) super.getComponent();
    }
    
    
    @Override
    public Producer createProducer() throws Exception
    {
        LOGGER.debug("INSIDE createProducer");

        // Camel Producers map to <to> route nodes.
        Producer producer = new ContentProducer(this);
        
        return producer;
    }

    
    @Override
    public Consumer createConsumer( Processor processor ) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    
    @Override
    public boolean isSingleton()
    {
        return true;
    }

}
