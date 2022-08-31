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
package org.codice.ddf.rest.impl.action;

import ddf.action.ActionProvider;
import ddf.catalog.Constants;
import ddf.catalog.transform.MetacardTransformer;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.DictionaryMap;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers {@link ActionProvider} objects into the Service Registry based upon the services
 * provided by other objects.
 *
 * @author Ashraf Barakat
 */
public class ActionProviderRegistryProxy {

  static final String ACTION_ID_PREFIX = "catalog.data.metacard.";

  private static final String PROVIDER_INTERFACE_NAME = ActionProvider.class.getName();

  private static final Logger LOGGER = LoggerFactory.getLogger(ActionProviderRegistryProxy.class);

  private Map<ServiceReference<MetacardTransformer>, ServiceRegistration> actionProviderRegistry =
      new HashMap<>();

  private MetacardTransformerActionProviderFactory actionFactory;

  public ActionProviderRegistryProxy(MetacardTransformerActionProviderFactory actionFactory) {
    this.actionFactory = actionFactory;
  }

  public void bind(ServiceReference<MetacardTransformer> reference) {

    LOGGER.info("New service registered [{}]", reference);

    String transformerId = null;

    if (reference.getProperty(Constants.SERVICE_ID) != null) {

      transformerId = reference.getProperty(Constants.SERVICE_ID).toString();

      // backwards compatibility
    }
    if (StringUtils.isBlank(transformerId)
        && reference.getProperty(Constants.SERVICE_SHORTNAME) != null) {

      transformerId = reference.getProperty(Constants.SERVICE_SHORTNAME).toString();
    }

    if (StringUtils.isBlank(transformerId)) {
      return;
    }

    String actionProviderId = ACTION_ID_PREFIX + transformerId;

    List<String> attributeNames = getAttributeNames(reference);

    ActionProvider provider =
        actionFactory.createActionProvider(actionProviderId, transformerId, attributeNames);

    Dictionary<String, String> actionProviderProperties = new DictionaryMap<>();

    actionProviderProperties.put(Constants.SERVICE_ID, actionProviderId);

    ServiceRegistration actionServiceRegistration =
        getBundleContext()
            .registerService(PROVIDER_INTERFACE_NAME, provider, actionProviderProperties);

    LOGGER.info("Registered new {} [{}]", PROVIDER_INTERFACE_NAME, actionServiceRegistration);

    actionProviderRegistry.put(reference, actionServiceRegistration);
  }

  public void unbind(ServiceReference<MetacardTransformer> reference) {

    LOGGER.info("Service unregistered [{}]", reference);

    ServiceRegistration actionProviderRegistration = actionProviderRegistry.remove(reference);

    if (actionProviderRegistration != null) {
      actionProviderRegistration.unregister();
      LOGGER.info("Unregistered {} [{}]", PROVIDER_INTERFACE_NAME, actionProviderRegistration);
    }
  }

  private List<String> getAttributeNames(ServiceReference<MetacardTransformer> serviceReference) {
    if (serviceReference != null) {
      MetacardTransformer transformer = getBundleContext().getService(serviceReference);
      return Optional.ofNullable(serviceReference.getProperty("required-attributes"))
          .filter(List.class::isInstance)
          .map(List.class::cast)
          .orElse(Collections.emptyList());
    }
    return Collections.emptyList();
  }

  protected BundleContext getBundleContext() {
    return FrameworkUtil.getBundle(ActionProviderRegistryProxy.class).getBundleContext();
  }
}
