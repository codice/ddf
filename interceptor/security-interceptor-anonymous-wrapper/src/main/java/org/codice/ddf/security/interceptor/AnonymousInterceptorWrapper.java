/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

package org.codice.ddf.security.interceptor;

import java.util.Set;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.security.wss4j.AbstractWSS4JInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnonymousInterceptorWrapper extends AbstractWSS4JInterceptor {
	private static final Logger LOGGER = LoggerFactory.getLogger(AnonymousInterceptorWrapper.class);
	private BundleContext context = null;
	private PhaseInterceptor anonIntercep = null;
	
	public AnonymousInterceptorWrapper(BundleContext context) {
        super();
        this.context = context;
        setPhase(Phase.PRE_PROTOCOL);
        //make sure this interceptor runs before the WSS4J one in the same Phase, otherwise it won't work
        Set<String> before = getBefore();
        before.add(WSS4JInInterceptor.class.getName());
	}

	@Override
	public void handleMessage(SoapMessage msg) throws Fault {
	    ServiceReference anonIntercepRef = context
                .getServiceReference(PhaseInterceptor.class.getName());
	    
	    if (anonIntercepRef != null) {
	        anonIntercep = (PhaseInterceptor) context.getService(anonIntercepRef);
	        if (anonIntercep != null) {
	            anonIntercep.handleMessage(msg);
	        } else {
	            LOGGER.debug("Anonymous Interceptor is null");
	        }
	    } else {
	        LOGGER.warn("Anonymous Interceptor is not installed, ignoring.");
	    }
	}

}
