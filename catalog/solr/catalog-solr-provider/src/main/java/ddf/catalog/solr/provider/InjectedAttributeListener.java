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
package ddf.catalog.solr.provider;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.InjectableAttribute;
import ddf.catalog.source.solr.DynamicSchemaResolver;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGI <code>ServiceListener</code> implementation that listens for <code>InjectableAttribute
 * </code>s that are registered in the OSGI service registry. Upon notification that a new attribute
 * has been registered, this listener will update its <code>DynamicSchemaResolver</code> with the
 * additional <code>AttributeDescriptor</code> that was added to the <code>AttributeRegistry</code>.
 */
public class InjectedAttributeListener implements ServiceListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(InjectedAttributeListener.class);

  private BundleContext context;
  private DynamicSchemaResolver resolver;
  private AttributeRegistry attributeRegistry;

  public InjectedAttributeListener(
      DynamicSchemaResolver resolver, AttributeRegistry attributeRegistry) {
    this.resolver = resolver;
    this.attributeRegistry = attributeRegistry;
  }

  public void init() {
    Bundle bundle = FrameworkUtil.getBundle(this.getClass());
    context = bundle.getBundleContext();
    try {
      String filter = "(objectClass=" + InjectableAttribute.class.getName() + ")";
      context.addServiceListener(this, filter);

      Collection<ServiceReference<InjectableAttribute>> alreadyRegistered =
          context.getServiceReferences(InjectableAttribute.class, null);
      alreadyRegistered
          .stream()
          .map(context::getService)
          .filter(Objects::nonNull)
          .map(InjectableAttribute::attribute)
          .forEach(this::registerAttribute);
    } catch (InvalidSyntaxException e) {
      LOGGER.warn("Unable to register listener for injected attributes", e);
    }
  }

  public void close() {
    if (context != null) {
      context.removeServiceListener(this);
    }
  }

  public void serviceChanged(ServiceEvent event) {
    ServiceReference serviceRef = event.getServiceReference();
    Object service = context.getService(serviceRef);
    if (service instanceof InjectableAttribute) {
      if (event.getType() == ServiceEvent.REGISTERED) {
        registerAttribute(((InjectableAttribute) service).attribute());
      }
    }
  }

  private void registerAttribute(String attributeName) {
    Optional<AttributeDescriptor> descriptor = attributeRegistry.lookup(attributeName);
    if (descriptor.isPresent()) {
      AttributeDescriptor ad = descriptor.get();
      LOGGER.debug(
          "Registering attribute {} of type {}",
          ad.getName(),
          ad.getType().getAttributeFormat().name());
      resolver.addAdditionalFields(Arrays.asList(ad));
    }
  }
}
