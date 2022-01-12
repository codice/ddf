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
package org.codice.ddf.branding.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.branding.BrandingPlugin;
import org.codice.ddf.branding.BrandingRegistry;
import org.codice.ddf.platform.util.ServiceComparator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrandingRegistryImpl implements BrandingRegistry {
  private static final Logger LOGGER = LoggerFactory.getLogger(BrandingRegistryImpl.class);

  private Map<ServiceReference, BrandingPlugin> brandingPlugins =
      Collections.synchronizedMap(
          new TreeMap<>(
              new ServiceComparator() {
                public int compare(ServiceReference ref1, ServiceReference ref2) {
                  return ref2.compareTo(ref1);
                }
              }));

  private BrandingListener brandingListener = new BrandingListener();

  public void init() {
    BundleContext context = getContext();
    context.addServiceListener(brandingListener);
  }

  public void destroy() {
    BundleContext context = getContext();
    context.removeServiceListener(brandingListener);
  }

  protected BundleContext getContext() {
    Bundle bundle = FrameworkUtil.getBundle(BrandingRegistryImpl.class);
    if (bundle != null) {
      return bundle.getBundleContext();
    }
    return null;
  }

  @Override
  public String getProductName() {
    return getBrandingPlugins().stream()
        .map(plugin -> StringUtils.substringBeforeLast(plugin.getProductName(), " "))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse("DDF");
  }

  @Override
  public String getAttributeFromBranding(BrandingMethod supplier) {
    return getBrandingPlugins().stream()
        .map(
            plugin -> {
              try {
                return supplier.apply(plugin);
              } catch (IOException e) {
                LOGGER.warn("Could not get the requested attribute from the Branding Plugin", e);
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse("");
  }

  @Override
  public String getProductVersion() {
    return getBrandingPlugins().stream()
        .map(plugin -> StringUtils.substringAfterLast(plugin.getProductName(), " "))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse("");
  }

  @Override
  public List<BrandingPlugin> getBrandingPlugins() {
    if (brandingPlugins.isEmpty()) {
      BundleContext context = getContext();
      try {
        Collection<ServiceReference<BrandingPlugin>> serviceReferences =
            context.getServiceReferences(BrandingPlugin.class, "");
        for (ServiceReference<BrandingPlugin> serviceReference : serviceReferences) {
          brandingPlugins.put(serviceReference, context.getService(serviceReference));
        }
      } catch (InvalidSyntaxException e) {
        LOGGER.debug("Unable to perform initial BrandingPlugin query.", e);
      }
    }
    return new ArrayList<>(brandingPlugins.values());
  }

  public void setBrandingPlugins(List<BrandingPlugin> brandingPlugins) {}

  class BrandingListener implements ServiceListener {

    @Override
    public void serviceChanged(ServiceEvent event) {
      Bundle bundle = FrameworkUtil.getBundle(BrandingRegistryImpl.class);
      ServiceReference<?> serviceReference = event.getServiceReference();
      boolean assignableTo =
          serviceReference.isAssignableTo(bundle, "org.codice.ddf.branding.BrandingPlugin");
      Object service = getContext().getService(serviceReference);
      if (assignableTo
          && event.getType() == ServiceEvent.REGISTERED
          && service instanceof BrandingPlugin) {
        brandingPlugins.put(serviceReference, (BrandingPlugin) service);
      }
      if (assignableTo
          && event.getType() == ServiceEvent.UNREGISTERING
          && service instanceof BrandingPlugin) {
        brandingPlugins.remove(serviceReference);
      }
    }
  }
}
