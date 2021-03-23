/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.interceptor;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.security.wss4j.AbstractWSS4JInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuestInterceptorWrapper extends AbstractWSS4JInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(GuestInterceptorWrapper.class);

  public GuestInterceptorWrapper() {
    super();
    setPhase(Phase.PRE_PROTOCOL);
    // make sure this interceptor runs before the WSS4J one in the same Phase, otherwise it won't
    // work
    Set<String> before = getBefore();
    before.add(WSS4JInInterceptor.class.getName());
    before.add(PolicyBasedWSS4JInInterceptor.class.getName());
  }

  protected BundleContext getContext() {
    Bundle cxfBundle = FrameworkUtil.getBundle(GuestInterceptorWrapper.class);
    if (cxfBundle != null) {
      return cxfBundle.getBundleContext();
    }
    return null;
  }

  @Override
  public void handleMessage(SoapMessage msg) throws Fault {
    BundleContext context = getContext();
    PhaseInterceptor guestIntercep = null;
    Collection<ServiceReference<PhaseInterceptor>> guestIntercepRefs = null;
    if (context != null) {
      try {
        guestIntercepRefs =
            context.getServiceReferences(PhaseInterceptor.class, "(interceptor=guest)");
      } catch (InvalidSyntaxException e) {
        // ignore, it isn't invalid
      }

      if (guestIntercepRefs != null && guestIntercepRefs.size() > 0) {
        Iterator<ServiceReference<PhaseInterceptor>> iterator = guestIntercepRefs.iterator();
        guestIntercep = context.getService(iterator.next());
      }
      if (guestIntercep != null) {
        guestIntercep.handleMessage(msg);
      } else {
        LOGGER.debug("Guest Interceptor is not installed, ignoring");
      }
    } else {
      LOGGER.debug("Unable to acquire bundle context for guest interceptor");
    }
  }
}
