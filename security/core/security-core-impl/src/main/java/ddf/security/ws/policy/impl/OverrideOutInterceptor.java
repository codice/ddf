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


import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.PolicyOutInterceptor;

import ddf.security.ws.policy.AbstractOverrideInterceptor;
import ddf.security.ws.policy.PolicyLoader;


/**
 * Implementation of the OverrideInterceptor for the outgoing message.
 * 
 */
public class OverrideOutInterceptor extends AbstractOverrideInterceptor
{

    /**
     * Creates a new instance of the interceptor. This sets up the override
     * interceptor specifically for the incoming message.
     * 
     * @param loader
     */
    public OverrideOutInterceptor( PolicyLoader loader )
    {
        super(Phase.SETUP, loader);
        getBefore().add(PolicyOutInterceptor.class.getName());
    }

}
