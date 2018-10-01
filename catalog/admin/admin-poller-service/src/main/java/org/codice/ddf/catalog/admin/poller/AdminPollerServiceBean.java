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
package org.codice.ddf.catalog.admin.poller;

import com.google.common.collect.Iterables;
import ddf.action.Action;
import ddf.action.MultiActionProvider;
import ddf.catalog.CatalogFramework;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.impl.SourceInfoRequestSources;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceDescriptor;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.apache.commons.lang.Validate;
import org.apache.shiro.util.CollectionUtils;
import org.codice.ddf.admin.core.api.ConfigurationAdmin;
import org.codice.ddf.admin.core.api.ConfigurationDetails;
import org.codice.ddf.admin.core.api.ConfigurationProperties;
import org.codice.ddf.admin.core.api.ConfigurationStatus;
import org.codice.ddf.admin.core.api.Metatype;
import org.codice.ddf.admin.core.api.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminPollerServiceBean implements AdminPollerServiceBeanMBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdminPollerServiceBean.class);

  private static final String SERVICE_NAME = ":service=admin-source-poller-service";

  private static final String MAP_ENTRY_REPORT_ACTIONS = "report_actions";

  private static final String MAP_ENTRY_OPERATION_ACTIONS = "operation_actions";

  private static final String MAP_ENTRY_ACTION_ID = "id";

  private static final String MAP_ENTRY_ACTION_TITLE = "title";

  private static final String MAP_ENTRY_ACTION_DESCRIPTION = "description";

  private static final String MAP_ENTRY_ACTION_URL = "url";

  private final ObjectName objectName;

  private final MBeanServer mBeanServer;

  private final AdminSourceHelper helper;

  private List<MultiActionProvider> reportActionProviders;

  private List<MultiActionProvider> operationActionProviders;

  private List<String> includeAsSource;

  private List<String> excludeAsSource;

  private String serviceFactoryFilter;

  private CatalogFramework catalogFramework;

  public AdminPollerServiceBean(
      ConfigurationAdmin configurationAdmin,
      org.osgi.service.cm.ConfigurationAdmin felixConfigAdmin,
      CatalogFramework catalogFramework) {
    Validate.notNull(catalogFramework, "Argument catalogFramework cannot be null");
    this.catalogFramework = catalogFramework;

    helper = getHelper();
    helper.configurationAdmin = configurationAdmin;
    helper.felixConfigAdmin = felixConfigAdmin;

    mBeanServer = ManagementFactory.getPlatformMBeanServer();
    ObjectName objName = null;
    try {
      objName = new ObjectName(AdminPollerServiceBean.class.getName() + SERVICE_NAME);
    } catch (MalformedObjectNameException e) {
      LOGGER.info(
          "Unable to create Admin Source Poller Service MBean with name [{}].",
          AdminPollerServiceBean.class.getName() + SERVICE_NAME,
          e);
    }
    objectName = objName;
  }

  public void init() {
    serviceFactoryFilter = getServiceFactoryFilterProperties();
    try {
      try {
        mBeanServer.registerMBean(this, objectName);
        LOGGER.debug(
            "Registered Admin Source Poller Service Service MBean under object name: {}",
            objectName);
      } catch (InstanceAlreadyExistsException e) {
        // Try to remove and re-register
        mBeanServer.unregisterMBean(objectName);
        mBeanServer.registerMBean(this, objectName);
        LOGGER.debug("Re-registered Admin Source Poller Service Service MBean");
      }
    } catch (Exception e) {
      LOGGER.info("Could not register MBean [{}].", objectName, e);
    }
  }

  public void destroy() {
    try {
      if (objectName != null && mBeanServer != null) {
        mBeanServer.unregisterMBean(objectName);
        LOGGER.debug("Unregistered Admin Source Poller Service Service MBean");
      }
    } catch (Exception e) {
      LOGGER.info("Exception unregistering MBean [{}].", objectName, e);
    }
  }

  // returns 1 = available, 0 = unavailable, -1 = status pending
  @Override
  public int sourceStatus(String servicePID) {
    try {
      List<Source> sources = helper.getSources();
      for (Source source : sources) {
        if (source instanceof ConfiguredService) {
          ConfiguredService cs = (ConfiguredService) source;
          try {
            Configuration config = helper.getConfiguration(cs);
            if (config != null && config.getProperties().get("service.pid").equals(servicePID)) {
              return sourceIsAvailable(source.getId());
            }
          } catch (IOException e) {
            LOGGER.info("Couldn't find configuration for source '{}'", source.getId());
          }
        } else {
          LOGGER.info("Source '{}' not a configured service", source.getId());
        }
      }
    } catch (InvalidSyntaxException e) {
      LOGGER.info("Could not get service reference list");
    }

    return 0;
  }

  private int sourceIsAvailable(String sourceId) {
    try {
      final SourceInfoRequest sourceInfoRequest =
          new SourceInfoRequestSources(false, Collections.singleton(sourceId));
      final SourceInfoResponse sourceInfoResponse =
          catalogFramework.getSourceInfo(sourceInfoRequest);
      final Set<SourceDescriptor> sourceDescriptors = sourceInfoResponse.getSourceInfo();
      final SourceDescriptor sourceDescriptor = Iterables.getOnlyElement(sourceDescriptors);

      if (sourceDescriptor.getLastAvailabilityDate() == null) {
        return -1;
      }
      if (sourceDescriptor.isAvailable()) {
        return 1;
      }
    } catch (Exception e) {
      LOGGER.info("Couldn't get availability on source {}", sourceId, e);
    }
    return 0;
  }

  @Override
  public List<Service> allSourceInfo() {
    // Get list of metatypes
    List<Service> metatypes = helper.getMetatypes();

    // Loop through each metatype and find its configurations
    for (Service metatype : metatypes) {
      try {
        List<Configuration> configs = helper.getConfigurations(metatype);

        List<ConfigurationDetails> configurations = new ArrayList<>();
        if (configs != null) {
          for (Configuration config : configs) {
            ConfigurationDetails source = new ConfigurationDetailsImpl();
            String pid = config.getPid();
            String fPid = config.getFactoryPid();
            boolean disabled = pid.endsWith(ConfigurationStatus.DISABLED_EXTENSION);
            if (fPid != null) {
              disabled = disabled || fPid.endsWith(ConfigurationStatus.DISABLED_EXTENSION);
            }
            source.setId(pid);
            source.setEnabled(!disabled);
            source.setFactoryPid(fPid);

            if (!disabled) {
              source.setName(helper.getName(config));
              source.setBundleName(helper.getBundleName(config));
              source.setBundleLocation(config.getBundleLocation());
              source.setBundle(helper.getBundleId(config));
            } else {
              source.setName(config.getPid());
            }

            Dictionary<String, Object> properties = config.getProperties();
            ConfigurationProperties plist = new ConfigurationPropertiesImpl();
            for (String key : Collections.list(properties.keys())) {
              plist.put(key, properties.get(key));
            }
            source.setConfigurationProperties(plist);
            source.put(MAP_ENTRY_REPORT_ACTIONS, getActions(config, reportActionProviders));
            source.put(MAP_ENTRY_OPERATION_ACTIONS, getActions(config, operationActionProviders));
            configurations.add(source);
          }
          metatype.setConfigurations(configurations);
        }
      } catch (Exception e) {
        LOGGER.info("Error getting source info: {}", e.getMessage());
      }
    }

    metatypes.sort(Comparator.comparing(Service::getId, String.CASE_INSENSITIVE_ORDER));
    return metatypes;
  }

  private List<Map<String, String>> getActions(
      Configuration config, List<MultiActionProvider> providers) {
    List<Map<String, String>> actions = new ArrayList<>();
    for (MultiActionProvider provider : providers) {
      if (!provider.canHandle(config)) {
        continue;
      }

      List<Action> curActionList = provider.getActions(config);
      for (Action action : curActionList) {
        Map<String, String> actionProperties = new HashMap<>();
        actionProperties.put(MAP_ENTRY_ACTION_ID, action.getId());
        actionProperties.put(MAP_ENTRY_ACTION_TITLE, action.getTitle());
        actionProperties.put(MAP_ENTRY_ACTION_DESCRIPTION, action.getDescription());
        actionProperties.put(MAP_ENTRY_ACTION_URL, action.getUrl().toString());
        actions.add(actionProperties);
      }
    }
    return actions;
  }

  protected AdminSourceHelper getHelper() {
    return new AdminSourceHelper();
  }

  protected String getServiceFactoryFilterProperties() {
    return createFilter("service.factoryPid");
  }

  private String createFilter(String pidKey) {
    String includes =
        (includeAsSource == null || CollectionUtils.isEmpty(includeAsSource))
            ? String.format("(%s=*)", pidKey)
            : includeAsSource
                .stream()
                .map(pid -> String.format("(%s=%s)", pidKey, pid))
                .collect(Collectors.joining());
    if (excludeAsSource == null || CollectionUtils.isEmpty(excludeAsSource)) {
      return String.format("(|%s)", includes);
    } else {
      String excludes =
          excludeAsSource
              .stream()
              .map(pid -> String.format("(!(%s=%s))", pidKey, pid))
              .collect(Collectors.joining());
      return String.format("(&(|%s)(&%s))", includes, excludes);
    }
  }

  protected class AdminSourceHelper {
    protected ConfigurationAdmin configurationAdmin;
    org.osgi.service.cm.ConfigurationAdmin felixConfigAdmin;

    private BundleContext getBundleContext() {
      return Optional.of(AdminPollerServiceBean.class)
          .map(FrameworkUtil::getBundle)
          .map(Bundle::getBundleContext)
          .orElseThrow(() -> new IllegalStateException("Error getting bundle context"));
    }

    protected List<Source> getSources() throws org.osgi.framework.InvalidSyntaxException {
      List<Source> sources = new ArrayList<>();
      List<ServiceReference<? extends Source>> refs = new ArrayList<>();
      refs.addAll(helper.getBundleContext().getServiceReferences(FederatedSource.class, null));
      refs.addAll(helper.getBundleContext().getServiceReferences(ConnectedSource.class, null));

      for (ServiceReference<? extends Source> ref : refs) {
        sources.add(getBundleContext().getService(ref));
      }

      return sources;
    }

    protected List<Service> getMetatypes() {
      return configurationAdmin.listServices(
          serviceFactoryFilter, ConfigurationAdmin.NO_MATCH_FILTER);
    }

    protected List<Configuration> getConfigurations(Metatype metatype)
        throws InvalidSyntaxException, IOException {
      return CollectionUtils.asList(
          felixConfigAdmin.listConfigurations(
              "(|(service.factoryPid="
                  + metatype.getId()
                  + ")(service.factoryPid="
                  + metatype.getId()
                  + ConfigurationStatus.DISABLED_EXTENSION
                  + "))"));
    }

    protected Configuration getConfiguration(ConfiguredService cs) throws IOException {
      return configurationAdmin.getConfiguration(cs.getConfigurationPid());
    }

    protected String getBundleName(Configuration config) {
      return configurationAdmin.getName(
          helper.getBundleContext().getBundle(config.getBundleLocation()));
    }

    protected long getBundleId(Configuration config) {
      return getBundleContext().getBundle(config.getBundleLocation()).getBundleId();
    }

    @Nullable
    protected String getName(Configuration config) {
      ObjectClassDefinition objectClassDefinition =
          configurationAdmin.getObjectClassDefinition(config);
      return (objectClassDefinition != null) ? objectClassDefinition.getName() : null;
    }
  }

  public void setReportActionProviders(List<MultiActionProvider> reportActionProviders) {
    this.reportActionProviders = reportActionProviders;
  }

  public void setOperationActionProviders(List<MultiActionProvider> operationActionProviders) {
    this.operationActionProviders = operationActionProviders;
  }

  public void setIncludeAsSource(List<String> includeAsSource) {
    this.includeAsSource = includeAsSource;
  }

  public void setExcludeAsSource(List<String> excludeAsSource) {
    this.excludeAsSource = excludeAsSource;
  }

  public static class ConfigurationDetailsImpl extends HashMap<String, Object>
      implements ConfigurationDetails {}

  public static class ConfigurationPropertiesImpl extends HashMap<String, Object>
      implements ConfigurationProperties {}
}
