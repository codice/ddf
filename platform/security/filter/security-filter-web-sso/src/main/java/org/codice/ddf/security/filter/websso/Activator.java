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
package org.codice.ddf.security.filter.websso;

import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultSecurityConfigurationMapping;
import org.ops4j.pax.web.service.whiteboard.SecurityConfigurationMapping;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

  public Activator() {}

  private ServiceRegistration<SecurityConfigurationMapping> securityReg;

  private static final String AUTH_METHOD = "DDF";

  private static final String REALM_NAME = "DDF";

  @Override
  public void start(BundleContext bundleContext) throws Exception {
    DefaultSecurityConfigurationMapping security = new DefaultSecurityConfigurationMapping();
    security.setAuthMethod(AUTH_METHOD);
    security.setRealmName(REALM_NAME);
    security.setContextSelectFilter("(osgi.http.whiteboard.context.path=/*)");
    bundleContext.registerService(SecurityConfigurationMapping.class, security, null);
  }

  @Override
  public void stop(BundleContext bundleContext) throws Exception {
    if (securityReg != null) {
      securityReg.unregister();
      securityReg = null;
    }
  }
}
