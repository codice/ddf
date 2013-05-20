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


import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.ws.policy.WSSecurityProperties;


/**
 * Implementation of the security properties interface. Allows calling clients
 * to send in a list of properties that are added. Otherwise, just extends the
 * HashMap class.
 * 
 */
public class WSSecurityPropertiesImpl extends HashMap<String, Object> implements WSSecurityProperties
{
    private Logger logger = LoggerFactory.getLogger(WSSecurityPropertiesImpl.class);

    private static final long serialVersionUID = 1L;

    /**
     * Sets the internal properties list with values from the incoming list.
     * 
     * @param properties List of properties to set.
     */
    public void setSecurityPropList( List<String> properties )
    {
        String[] values;
        clear();
        if (properties != null)
        {
            for ( String mapping : properties )
            {
                values = mapping.split("=");
                if (values.length == 2)
                {
                    logger.debug("Adding mapping: {} = {} to WS-Security Property Map.", values[0].trim(),
                        values[1].trim());
                    put(values[0].trim(), values[1].trim());
                }
                else
                {
                    logger.warn("WS-Security Property mapping ignored: {} doesn't match expected format of key=value",
                        mapping);
                }
            }
        }
    }

}
