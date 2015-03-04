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

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.security.wss4j.AbstractWSS4JInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class AnonymousInterceptorWrapper extends AbstractWSS4JInterceptor {
	private static final Logger LOGGER = LoggerFactory.getLogger(AnonymousInterceptorWrapper.class);

    private PhaseInterceptor anonIntercep;

	public AnonymousInterceptorWrapper() {
        super();
        setPhase(Phase.PRE_PROTOCOL);
        //make sure this interceptor runs before the WSS4J one in the same Phase, otherwise it won't work
        Set<String> before = getBefore();
        before.add(WSS4JInInterceptor.class.getName());
	}

    public void setAnonIntercep(PhaseInterceptor anonIntercep) {
        this.anonIntercep = anonIntercep;
    }

    @Override
	public void handleMessage(SoapMessage msg) throws Fault {
        if (anonIntercep != null) {
            anonIntercep.handleMessage(msg);
        } else {
            LOGGER.debug("Anonymous Interceptor is not installed, ignoring");
        }
	}

}
