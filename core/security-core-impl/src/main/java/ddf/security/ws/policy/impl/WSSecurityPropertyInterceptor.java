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
package ddf.security.ws.policy.impl;


import java.util.Map.Entry;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.ws.policy.WSSecurityProperties;


/**
 * Interceptor that sets the security properties on the current message context
 * so that they may be read by other interceptors in the chain. <br/>
 * <br/>
 * NOTE: This interceptor needs to be called <b>BEFORE</b> the
 * WSS4JInInterceptor.
 * 
 */
public class WSSecurityPropertyInterceptor extends AbstractPhaseInterceptor<Message>
{
    private WSSecurityProperties properties;
    private Logger logger = LoggerFactory.getLogger(WSSecurityPropertyInterceptor.class);

    /**
     * Sets the interceptor at the Phase.PRE_PROTOCOL level and also adds it
     * before the WSS4JInInterceptor.
     */
    public WSSecurityPropertyInterceptor()
    {
        super(Phase.PRE_PROTOCOL);
        getBefore().add(WSS4JInInterceptor.class.getName());
    }

    @Override
    public void handleMessage( Message msg ) throws Fault
    {
        for ( Entry<String, Object> curEntry : properties.entrySet() )
        {
            logger
                .debug("Setting {}:{} as a property on the incoming message.", curEntry.getKey(), curEntry.getValue());
            msg.setContextualProperty(curEntry.getKey(), curEntry.getValue());
        }
    }

    /**
     * Sets the list of properties that should be set on the message context.
     * 
     * @param properties A reference to a WSSecurityProperties class.
     */
    public void setProperties( WSSecurityProperties properties )
    {
        this.properties = properties;
    }

    /**
     * Retrieves the list of properties that are being set.
     * 
     * @return a reference to the WSSecurityProperties class.
     */
    public WSSecurityProperties getProperties()
    {
        return properties;
    }

}
