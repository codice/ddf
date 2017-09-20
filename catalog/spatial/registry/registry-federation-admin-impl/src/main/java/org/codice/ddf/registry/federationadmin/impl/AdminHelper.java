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
package org.codice.ddf.registry.federationadmin.impl;

import static org.codice.ddf.admin.core.api.ConfigurationAdmin.NO_MATCH_FILTER;

import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.Source;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.codice.ddf.admin.core.api.Service;
import org.codice.ddf.registry.api.internal.RegistryStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminHelper {
  private static final Logger LOGGER = LoggerFactory.getLogger(AdminHelper.class);

  private static final String REGISTRY_FILTER =
      String.format(
          "(|(%s=*Registry*Store*)(%s=*registry*store*))",
          ConfigurationAdmin.SERVICE_FACTORYPID, ConfigurationAdmin.SERVICE_FACTORYPID);

  private static final String REGISTRY_POLICY_PID =
      "org.codice.ddf.registry.policy.RegistryPolicyPlugin";

  private static final String REGISTRY_ID_KEY = "registryEntryIds";

  private static final String DISABLE_REGISTRY_KEY = "registryDisabled";

  private static final String WHITE_LIST_KEY = "whiteList";

  private static final String DISABLED = "_disabled";

  private final ConfigurationAdmin configurationAdmin;

  private final org.codice.ddf.admin.core.api.ConfigurationAdmin configAdmin;

  public AdminHelper(
      org.codice.ddf.admin.core.api.ConfigurationAdmin configAdmin,
      ConfigurationAdmin configurationAdmin) {
    this.configurationAdmin = configurationAdmin;
    this.configAdmin = configAdmin;
  }

  private BundleContext getBundleContext() {
    Bundle bundle = FrameworkUtil.getBundle(this.getClass());
    if (bundle != null) {
      return bundle.getBundleContext();
    }
    return null;
  }

  public List<Source> getRegistrySources() throws InvalidSyntaxException {
    List<ServiceReference<? extends Source>> refs = new ArrayList<>();
    refs.addAll(getBundleContext().getServiceReferences(RegistryStore.class, null));
    return refs.stream().map(e -> getBundleContext().getService(e)).collect(Collectors.toList());
  }

  public List<Service> getMetatypes() {
    return configAdmin.listServices(REGISTRY_FILTER, NO_MATCH_FILTER);
  }

  public List<Configuration> getConfigurations(Service metatype)
      throws InvalidSyntaxException, IOException {
    Configuration[] configurations =
        configurationAdmin.listConfigurations(
            String.format(
                "(|(%s=%s)(%s=%s%s))",
                ConfigurationAdmin.SERVICE_FACTORYPID,
                metatype.getId(),
                ConfigurationAdmin.SERVICE_FACTORYPID,
                metatype.getId(),
                DISABLED));
    return configurations == null ? new ArrayList<>() : Arrays.asList(configurations);
  }

  public Configuration getConfiguration(ConfiguredService cs) throws IOException {
    return configurationAdmin.getConfiguration(cs.getConfigurationPid());
  }

  public String getBundleName(Configuration config) {
    return configAdmin.getName(getBundleContext().getBundle(config.getBundleLocation()));
  }

  public long getBundleId(Configuration config) {
    return getBundleContext().getBundle(config.getBundleLocation()).getBundleId();
  }

  public String getName(Configuration config) {
    return configAdmin.getObjectClassDefinition(config).getName();
  }

  Map<String, Object> getFilterProperties() throws IOException {
    Map<String, Object> props = new HashMap<>();
    Dictionary<String, Object> configProps =
        configurationAdmin.getConfiguration(REGISTRY_POLICY_PID, null).getProperties();
    Map<String, Object> configMap = new HashMap<>();
    if (configProps == null) {
      configProps = new Hashtable<>();
    }

    props.put(
        FederationAdmin.CLIENT_MODE,
        Optional.ofNullable(configProps.get(DISABLE_REGISTRY_KEY)).orElse(false));
    props.put(
        FederationAdmin.FILTER_INVERTED,
        Optional.ofNullable(configProps.get(WHITE_LIST_KEY)).orElse(false));
    props.put(
        FederationAdmin.SUMMARY_FILTERED,
        Optional.ofNullable(configProps.get(REGISTRY_ID_KEY)).orElse(new String[0]));
    return props;
  }

  void setFilteredNodeList(List<String> filterList) {
    setFilterProperty(REGISTRY_ID_KEY, filterList.toArray(new String[0]));
  }

  void setFilterInverted(boolean inverted) {
    setFilterProperty(WHITE_LIST_KEY, inverted);
  }

  void setClientMode(boolean clientMode) {
    setFilterProperty(DISABLE_REGISTRY_KEY, clientMode);
  }

  private void setFilterProperty(String key, Object value) {
    try {
      Configuration config = configurationAdmin.getConfiguration(REGISTRY_POLICY_PID, null);
      Dictionary<String, Object> props = config.getProperties();
      if (props == null) {
        props = new Hashtable<>();
      }
      props.put(key, value);
      config.update(props);
    } catch (IOException e) {
      LOGGER.debug("Error setting filter property {} to {}", key, value);
    }
  }
}
