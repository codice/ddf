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
package org.codice.ddf.rest.ddf.context;

import java.util.Dictionary;
import java.util.Hashtable;
import org.ops4j.pax.web.service.spi.context.DefaultServletContextHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

  private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

  private ServiceRegistration<ServletContextHelper> registration;

  @Override
  public void start(BundleContext bundleContext) throws Exception {
    Dictionary<String, Object> properties = new Hashtable<>();
    properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "ddfDefaultContext");
    properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
    //        properties.put(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY, "");
    properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
    registration =
        bundleContext.registerService(
            ServletContextHelper.class,
            new DefaultServletContextHelperServiceFactory(),
            properties);
  }

  @Override
  public void stop(BundleContext bundleContext) throws Exception {
    if (registration != null) {
      registration.unregister();
      registration = null;
    }
  }

  private static class DefaultServletContextHelperServiceFactory
      implements ServiceFactory<ServletContextHelper> {

    @Override
    public ServletContextHelper getService(
        Bundle bundle, ServiceRegistration<ServletContextHelper> registration) {
      return new DefaultServletContextHelper(bundle);
    }

    @Override
    public void ungetService(
        Bundle bundle,
        ServiceRegistration<ServletContextHelper> registration,
        ServletContextHelper service) {}
  }
}
