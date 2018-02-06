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
package org.codice.ddf.admin.application.service.impl;

import static org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.codice.ddf.admin.application.plugin.ApplicationPlugin;
import org.codice.ddf.admin.application.rest.model.FeatureDetails;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationNode;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.admin.core.api.ConfigurationAdmin;
import org.codice.ddf.admin.core.api.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the Application Service MBean. Provides an MBean interface for the application
 * service api.
 */
public class ApplicationServiceBean implements ApplicationServiceBeanMBean {

  private static final String MAP_NAME = "name";

  private static final String MAP_VERSION = "version";

  private static final String MAP_DESCRIPTION = "description";

  private static final String MAP_CHILDREN = "children";

  private static final String MAP_STATE = "state";

  private static final String MAP_URI = "uri";

  private static final String INSTALL_PROFILE_DEFAULT_APPLICATIONS = "defaultApplications";

  @SuppressWarnings("squid:S1192")
  private static final String INSTALL_PROFILE_DESCRIPTION = "description";

  private static final String INSTALL_PROFILE_NAME = "name";

  private static final String MAP_DEPENDENCIES = "dependencies";

  private static final String MAP_PARENTS = "parents";

  private static final String MAP_STATUS = "status";

  private static final String MAP_REPOSITORY = "repository";

  /** the name of the metatype service to be looked up. */
  private static final String META_TYPE_NAME = "org.osgi.service.metatype.MetaTypeService";

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationServiceBeanMBean.class);

  private final ConfigurationAdmin configAdmin;

  private ObjectName objectName;

  private MBeanServer mBeanServer;

  /** the service pid string. */
  private ApplicationService appService;

  /** has all the application plugins. */
  private List<ApplicationPlugin> applicationPlugins;

  /** the service tracker. */
  private ServiceTracker<Object, Object> serviceTracker;

  /**
   * Creates an instance of an ApplicationServiceBean
   *
   * @param appService ApplicationService that is running in the system.
   * @throws ApplicationServiceException If an error occurs when trying to construct the MBean
   *     objects.
   */
  public ApplicationServiceBean(
      ApplicationService appService, ConfigurationAdmin configAdmin, MBeanServer mBeanServer)
      throws ApplicationServiceException {
    this.appService = appService;
    this.configAdmin = configAdmin;
    this.mBeanServer = mBeanServer;
    try {
      objectName =
          new ObjectName(ApplicationService.class.getName() + ":service=application-service");
    } catch (MalformedObjectNameException mone) {
      throw new ApplicationServiceException("Could not create objectname.", mone);
    }
  }

  /**
   * Initializes the initial variables and registers the class to the MBean server. <br>
   * <br>
   * <b>NOTE: This should be run before any other operations are performed. Operations will NOT be
   * usable until this is called (and until destroy() is called).</b>
   *
   * @throws ApplicationServiceException if an error occurs during registration.
   */
  public void init() throws ApplicationServiceException {
    try {
      registerMBean();
    } catch (Exception e) {
      LOGGER.warn("Could not register mbean.", e);
      throw new ApplicationServiceException(e);
    }
  }

  private void registerMBean()
      throws MBeanRegistrationException, NotCompliantMBeanException, InstanceNotFoundException,
          InstanceAlreadyExistsException {
    try {
      LOGGER.debug("Registering application service MBean under object name: {}", objectName);
      mBeanServer.registerMBean(this, objectName);
    } catch (InstanceAlreadyExistsException iaee) {
      // Try to remove and re-register
      LOGGER.debug("Re-registering Application Service MBean");
      mBeanServer.unregisterMBean(objectName);
      mBeanServer.registerMBean(this, objectName);
    }
  }

  /**
   * Destroys the application service bean by unregistering it from the MBean server. <br>
   * <br>
   * <b>NOTE: This should be run after all operations are completed and the bean is no longer
   * needed. Operations will NOT be usable after this is called (until init() is called). </b>
   *
   * @throws ApplicationServiceException if an error occurs during unregistration.
   */
  public void destroy() throws ApplicationServiceException {
    try {
      if (objectName != null && mBeanServer != null) {
        mBeanServer.unregisterMBean(objectName);
      }
    } catch (Exception e) {
      LOGGER.warn("Exception unregistering mbean: ", e);
      throw new ApplicationServiceException(e);
    }
  }

  @Override
  public List<Map<String, Object>> getInstallationProfiles() {
    List<Feature> installationProfiles = appService.getInstallationProfiles();
    List<Map<String, Object>> profiles = new ArrayList<>();

    for (Feature profile : installationProfiles) {
      Map<String, Object> profileMap = new HashMap<>();
      profileMap.put(INSTALL_PROFILE_NAME, profile.getName());
      profileMap.put(INSTALL_PROFILE_DESCRIPTION, profile.getDescription());

      List<String> includedFeatures = new ArrayList<>();
      profile.getDependencies().forEach(dep -> includedFeatures.add(dep.getName()));
      profileMap.put(INSTALL_PROFILE_DEFAULT_APPLICATIONS, includedFeatures);

      profiles.add(profileMap);
    }

    return profiles;
  }

  @Override
  public List<Map<String, Object>> getApplicationTree() {
    Set<ApplicationNode> rootApplications = appService.getApplicationTree();
    List<Map<String, Object>> applications = new ArrayList<>();
    for (ApplicationNode curRoot : rootApplications) {
      applications.add(convertApplicationNode(curRoot));
    }
    LOGGER.debug("Returning {} root applications.", applications.size());
    return applications;
  }

  private Map<String, Object> convertApplicationNode(ApplicationNode application) {
    LOGGER.debug("Converting {} to a map", application.getApplication().getName());
    Map<String, Object> appMap = new HashMap<>();
    Application internalApplication = application.getApplication();
    appMap.put(MAP_NAME, internalApplication.getName());
    appMap.put(MAP_VERSION, internalApplication.getVersion());
    appMap.put(MAP_DESCRIPTION, internalApplication.getDescription());
    appMap.put(MAP_STATE, application.getStatus().getState().toString());
    appMap.put(MAP_URI, internalApplication.getURI().toString());
    List<Map<String, Object>> children = new ArrayList<>();
    for (ApplicationNode curNode : application.getChildren()) {
      children.add(convertApplicationNode(curNode));
    }
    appMap.put(MAP_CHILDREN, children);
    return appMap;
  }

  @Override
  public List<Map<String, Object>> getApplications() {
    Set<ApplicationNode> rootApplications = appService.getApplicationTree();
    List<Map<String, Object>> applications = new ArrayList<>();
    List<Map<String, Object>> applicationsArray = new ArrayList<>();
    for (ApplicationNode curRoot : rootApplications) {
      List<String> parentList = new ArrayList<>();
      applications.add(convertApplicationEntries(curRoot, parentList, applicationsArray));
    }
    LOGGER.debug("Returning {} root applications.", applications.size());
    return applicationsArray;
  }

  private Map<String, Object> convertApplicationEntries(
      ApplicationNode application,
      List<String> parentList,
      List<Map<String, Object>> applicationsArray) {
    LOGGER.debug("Converting {} to a map", application.getApplication().getName());
    Map<String, Object> appMap = new HashMap<>();
    Application internalApplication = application.getApplication();
    appMap.put(MAP_NAME, internalApplication.getName());
    appMap.put(MAP_VERSION, internalApplication.getVersion());
    appMap.put(MAP_DESCRIPTION, internalApplication.getDescription());
    appMap.put(MAP_STATE, application.getStatus().getState().toString());
    appMap.put(MAP_URI, internalApplication.getURI().toString());
    List<String> childrenList = new ArrayList<>();
    parentList.add(internalApplication.getName());
    List<String> transferParentList = new ArrayList<>();
    appMap.put(MAP_PARENTS, parentList);

    for (ApplicationNode curNode : application.getChildren()) {
      Application node = curNode.getApplication();
      childrenList.add(node.getName());
      makeDependencyList(childrenList, curNode);

      convertApplicationEntries(curNode, parentList, applicationsArray);
    }
    appMap.put(MAP_DEPENDENCIES, childrenList);

    if (parentList.size() == 1) {
      transferParentList.clear();
      appMap.put(MAP_PARENTS, transferParentList);
    } else {
      int index = parentList.indexOf(internalApplication.getName());
      for (int i = 0; i < index; i++) {
        transferParentList.add(parentList.get(i));
      }
      appMap.put(MAP_PARENTS, transferParentList);
      parentList.clear();
      parentList.addAll(transferParentList);
    }
    applicationsArray.add(appMap);
    return appMap;
  }

  private void makeDependencyList(List<String> childrenList, ApplicationNode application) {
    LOGGER.debug("Getting Dependency List", application.getApplication().getName());
    for (ApplicationNode curNode : application.getChildren()) {
      Application node = curNode.getApplication();
      childrenList.add(node.getName());
      makeDependencyList(childrenList, curNode);
    }
  }

  @Override
  public synchronized boolean startApplication(String appName) {
    try {
      LOGGER.debug("Starting application with name {}", appName);
      appService.startApplication(appName);
      LOGGER.debug("Finished installing application {}", appName);
      return true;
    } catch (ApplicationServiceException ase) {
      LOGGER.warn("Application {} was not successfully started.", appName, ase);
      return false;
    }
  }

  @Override
  public synchronized boolean stopApplication(String appName) {
    try {
      LOGGER.debug("Stopping application with name {}", appName);
      appService.stopApplication(appName);
      LOGGER.debug("Finished stopping application {}", appName);
      return true;
    } catch (ApplicationServiceException ase) {
      LOGGER.warn("Application {} was not successfully stopped.", appName, ase);
      return false;
    }
  }

  @Override
  public void addApplications(List<Map<String, Object>> applicationURLList) {
    for (Map<String, Object> curURL : applicationURLList) {
      String applicationUrl = (String) curURL.get("value");
      try {
        appService.addApplication(new URI(applicationUrl));
      } catch (URISyntaxException use) {
        LOGGER.warn("Could not add application with url {}, not a valid URL.", applicationUrl);
        LOGGER.debug("Could not add application", use);
      } catch (ApplicationServiceException ase) {
        LOGGER.warn(
            "Could not add application with url {} due to error {}.",
            applicationUrl,
            ase.getMessage());
        LOGGER.debug("Could not add application", ase);
      }
    }
  }

  @Override
  public void removeApplication(String appName) {
    if (!StringUtils.isEmpty(appName)) {
      try {
        LOGGER.debug("Removing application with name: {}", appName);
        appService.removeApplication(appName);
      } catch (ApplicationServiceException ase) {
        LOGGER.warn("Could not remove application with nae {} due to error.", appName, ase);
      }
    }
  }

  @Nullable
  Set<BundleInfo> getBundleInfosForApplication(String applicationID) {
    Set<BundleInfo> bundleInfos = null;
    try {
      Application app = appService.getApplication(applicationID);
      if (app == null) {
        return null;
      }
      bundleInfos = app.getBundles();
    } catch (ApplicationServiceException e) {
      LOGGER.warn("There was an error while trying to access the application", e);
    }
    return bundleInfos;
  }

  /** {@inheritDoc}. */
  @Override
  public List<Map<String, Object>> getServices(String applicationID) {
    List<Service> services =
        configAdmin.listServices(getDefaultFactoryLdapFilter(), getDefaultLdapFilter());

    if (services.isEmpty()) {
      return Collections.emptyList();
    }

    Set<BundleInfo> bundleInfos = getBundleInfosForApplication(applicationID);
    if (bundleInfos == null) {
      return Collections.emptyList();
    }

    Set<String> bundleLocations =
        bundleInfos.stream().map(BundleInfo::getLocation).collect(Collectors.toSet());

    MetaTypeService metatypeService = getMetaTypeService();
    Set<MetaTypeInformation> metatypeInformation =
        (metatypeService == null)
            ? Collections.emptySet()
            : Arrays.stream(getContext().getBundles())
                .filter(b -> bundleLocations.contains(b.getLocation()))
                .map(metatypeService::getMetaTypeInformation)
                .collect(Collectors.toSet());

    return services
        .stream()
        .filter(
            service ->
                hasBundleLocation(service, bundleLocations)
                    || hasMetatypesForService(service, metatypeInformation))
        .collect(Collectors.toList());
  }

  /**
   * Checks to see if a given service configuration has a bundle location in the set provided.
   *
   * @param service the service with a location to check.
   * @param bundleLocations a set of bundle locations to check.
   * @return true if the service's location is in bundleLocations, false otherwise.
   */
  private boolean hasBundleLocation(Map<String, Object> service, Set<String> bundleLocations) {
    return Stream.of(service)
        .map(m -> m.get("configurations"))
        .filter(Objects::nonNull)
        .map(List.class::cast)
        .flatMap(List::stream)
        .map(Map.class::cast)
        .map(m -> m.get("bundle_location"))
        .filter(Objects::nonNull)
        .map(String.class::cast)
        .anyMatch(bundleLocations::contains);
  }

  /**
   * Checks to see if there are any metatypes out there for a particular service.
   *
   * @param service - our service we want metatypes for.
   * @param metatypeInformations - Where we'll look for metatypes that match our service from.
   * @return true if there is, and the service should be added, or false if it shouldn't be.
   */
  private boolean hasMetatypesForService(
      Map<String, Object> service, Set<MetaTypeInformation> metatypeInformations) {
    String id = (String) service.get("id");
    if (id == null) {
      return false;
    }
    Boolean isFactory = (Boolean) service.get("factory");
    if (isFactory == null) {
      return false;
    }

    Stream<MetaTypeInformation> infoStream = metatypeInformations.stream().filter(Objects::nonNull);
    return (isFactory
            ? infoStream.map(MetaTypeInformation::getFactoryPids)
            : infoStream.map(MetaTypeInformation::getPids))
        .filter(Objects::nonNull)
        .flatMap(Arrays::stream)
        .anyMatch(id::equals);
  }

  private String getDefaultFactoryLdapFilter() {
    List<String> filterList = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(filterList)) {
      StringBuilder ldapFilter = new StringBuilder();
      ldapFilter.append("(");
      ldapFilter.append("|");

      for (String fpid : filterList) {
        ldapFilter.append("(");
        ldapFilter.append(SERVICE_FACTORYPID);
        ldapFilter.append("=");
        ldapFilter.append(fpid);
        ldapFilter.append(")");
      }

      ldapFilter.append(")");

      return ldapFilter.toString();
    }
    return "(" + SERVICE_FACTORYPID + "=" + "*)";
  }

  private String getDefaultLdapFilter() {
    List<String> filterList = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(filterList)) {
      StringBuilder ldapFilter = new StringBuilder();
      ldapFilter.append("(");
      ldapFilter.append("|");

      for (String fpid : filterList) {
        ldapFilter.append("(");
        ldapFilter.append(Constants.SERVICE_PID);
        ldapFilter.append("=");
        ldapFilter.append(fpid);
        ldapFilter.append("*");
        ldapFilter.append(")");
      }

      ldapFilter.append(")");

      return ldapFilter.toString();
    }
    return "(" + Constants.SERVICE_PID + "=" + "*)";
  }

  /**
   * Getter method for the plugin list.
   *
   * @return the plugin list.
   */
  public List<ApplicationPlugin> getApplicationPlugins() {
    return applicationPlugins;
  }

  /**
   * Setter method for the plugin list.
   *
   * @param applicationPlugins the plugin list.
   */
  public void setApplicationPlugins(List<ApplicationPlugin> applicationPlugins) {
    this.applicationPlugins = applicationPlugins;
  }

  @Override
  public List<Map<String, Object>> getAllFeatures() {
    return getFeatureMap(appService.getAllFeatures());
  }

  @Override
  public List<Map<String, Object>> findApplicationFeatures(String applicationName) {
    return getFeatureMap(appService.findApplicationFeatures(applicationName));
  }

  private List<Map<String, Object>> getFeatureMap(List<FeatureDetails> featureViews) {
    List<Map<String, Object>> features = new ArrayList<>();
    for (FeatureDetails feature : featureViews) {
      Map<String, Object> featureMap = new HashMap<>();
      featureMap.put(MAP_NAME, feature.getName());
      featureMap.put(MAP_VERSION, feature.getVersion());
      featureMap.put(MAP_STATUS, feature.getStatus());
      featureMap.put(MAP_REPOSITORY, feature.getRepository());
      features.add(featureMap);
    }
    return features;
  }

  /**
   * Gets the service with the specified class name. Will create a new {@link ServiceTracker} if the
   * service is not already retrieved.
   *
   * @return the service or <code>null</code> if missing.
   */
  @Nullable
  final MetaTypeService getMetaTypeService() {
    BundleContext context = getContext();

    if (serviceTracker == null) {
      serviceTracker = new ServiceTracker<>(context, META_TYPE_NAME, null);
      serviceTracker.open();
    }
    return (MetaTypeService) serviceTracker.getService();
  }

  protected BundleContext getContext() {
    Bundle cxfBundle = FrameworkUtil.getBundle(ApplicationServiceBean.class);
    if (cxfBundle != null && cxfBundle.getBundleContext() != null) {
      return cxfBundle.getBundleContext();
    }
    throw new IllegalStateException("Could not get context from cxfBundle");
  }

  /** {@inheritDoc}. */
  @Override
  public List<Map<String, Object>> getPluginsForApplication(String appName) {
    List<Map<String, Object>> returnValues = new ArrayList<>();

    for (ApplicationPlugin plugin : applicationPlugins) {
      if (plugin.matchesAssocationName(appName)) {
        returnValues.add(plugin.toJSON());
      }
    }

    return returnValues;
  }

  /**
   * serviceTracker setter method. Needed for use in unit tests.
   *
   * @param serviceTracker the desired serviceTracker instance
   */
  void setServiceTracker(ServiceTracker serviceTracker) {
    this.serviceTracker = (ServiceTracker<Object, Object>) serviceTracker;
  }
}
