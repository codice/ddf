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

import ddf.security.common.audit.SecurityLogger;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.system.SystemService;
import org.codice.ddf.admin.application.plugin.ApplicationPlugin;
import org.codice.ddf.admin.application.rest.model.FeatureDetails;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.admin.core.api.ConfigurationAdmin;
import org.codice.ddf.admin.core.api.Service;
import org.codice.ddf.sync.installer.api.SynchronizedInstaller;
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

  private static final String INSTALL_PROFILE_DEFAULT_APPLICATIONS = "defaultApplications";

  @SuppressWarnings("squid:S1192")
  private static final String INSTALL_PROFILE_DESCRIPTION = "description";

  private static final String INSTALL_PROFILE_NAME = "name";

  private static final String MAP_STATUS = "status";

  private static final String MAP_REPOSITORY = "repository";

  /** the name of the metatype service to be looked up. */
  private static final String META_TYPE_NAME = "org.osgi.service.metatype.MetaTypeService";

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationServiceBeanMBean.class);

  private static final Map<Long, String> BUNDLE_LOCATIONS = new ConcurrentHashMap<>();

  private final ConfigurationAdmin configAdmin;

  private final SystemService systemService;

  private final ObjectName objectName;

  private final MBeanServer mBeanServer;

  /** the service pid string. */
  private final ApplicationService appService;

  /** has all the application plugins. */
  private List<ApplicationPlugin> applicationPlugins;

  /** the service tracker. */
  private ServiceTracker<Object, Object> serviceTracker;

  private SynchronizedInstaller syncInstaller;

  /**
   * Creates an instance of an ApplicationServiceBean
   *
   * @param appService ApplicationService that is running in the system.
   * @throws ApplicationServiceException If an error occurs when trying to construct the MBean
   *     objects.
   */
  public ApplicationServiceBean(
      ApplicationService appService,
      ConfigurationAdmin configAdmin,
      MBeanServer mBeanServer,
      SynchronizedInstaller syncInstaller,
      SystemService systemService)
      throws ApplicationServiceException {
    this.appService = appService;
    this.configAdmin = configAdmin;
    this.mBeanServer = mBeanServer;
    this.syncInstaller = syncInstaller;
    this.systemService = systemService;
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
  public void installFeature(String feature) {
    LOGGER.trace("Installing feature {}", feature);

    try {
      AccessController.doPrivileged(
          (PrivilegedExceptionAction<Void>)
              () -> {
                syncInstaller.installFeatures(
                    EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles), feature);
                return null;
              });
    } catch (VirtualMachineError e) {
      throw e;
    } catch (PrivilegedActionException e) {
      handleInstallFeatureException(feature, e.getException());
    } catch (Throwable e) {
      handleInstallFeatureException(feature, e);
    }
  }

  @Override
  public void uninstallFeature(String feature) {
    LOGGER.trace("Uninstalling feature {}", feature);

    try {
      AccessController.doPrivileged(
          (PrivilegedExceptionAction<Void>)
              () -> {
                syncInstaller.uninstallFeatures(
                    EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles), feature);
                return null;
              });
    } catch (VirtualMachineError e) {
      throw e;
    } catch (PrivilegedActionException e) {
      handleUninstallFeatureException(feature, e.getException());
    } catch (Throwable e) {
      handleUninstallFeatureException(feature, e);
    }
  }

  private void handleInstallFeatureException(String feature, Throwable e) {
    handleFeatureException(
        String.format("Could not install feature %s", feature),
        "{}. Refer to documentation for additional features that must be installed for this feature to install correctly.",
        e);
  }

  private void handleUninstallFeatureException(String feature, Throwable e) {
    handleFeatureException(
        String.format("Could not uninstall feature %s", feature),
        "{}. Refer to documentation for additional features that must be uninstalled before this feature can properly uninstall.",
        e);
  }

  private void handleFeatureException(String errorMessage, String logMessage, Throwable e) {
    LOGGER.error(logMessage, errorMessage, e);
    throw new ApplicationServiceBeanException(errorMessage);
  }

  @Override
  public List<Map<String, Object>> getInstallationProfiles() {
    try {
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

    } catch (VirtualMachineError e) {
      throw e;
    } catch (Throwable e) {
      LOGGER.info("Could not retrieve installation profiles", e);
      throw new ApplicationServiceBeanException("Could not retrieve installation profiles");
    }
  }

  @Override
  public List<Map<String, Object>> getApplications() {
    try {
      return appService
          .getApplications()
          .stream()
          .filter(
              app ->
                  !getPluginsForApplication(app.getName()).isEmpty()
                      || !getServices(app.getName()).isEmpty())
          .map(this::convertApplicationEntries)
          .collect(Collectors.toList());
    } catch (VirtualMachineError e) {
      throw e;
    } catch (Throwable e) {
      LOGGER.info("Could not retrieve applications", e);
      throw new ApplicationServiceBeanException("Could not retrieve applications");
    }
  }

  private Map<String, Object> convertApplicationEntries(Application application) {
    LOGGER.debug("Converting {} to a map", application.getName());
    Map<String, Object> appMap = new HashMap<>();
    appMap.put(MAP_NAME, application.getName());
    appMap.put(MAP_DESCRIPTION, application.getDescription());
    return appMap;
  }

  @Nullable
  Set<BundleInfo> getBundleInfosForApplication(String applicationID) {
    Application app = appService.getApplication(applicationID);
    return app == null ? null : app.getBundles();
  }

  /** {@inheritDoc}. */
  @Override
  public List<Map<String, Object>> getServices(String applicationID) {
    try {
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
                  .filter(b -> bundleLocations.contains(computeLocation(b)))
                  .map(metatypeService::getMetaTypeInformation)
                  .collect(Collectors.toSet());

      return services
          .stream()
          .filter(
              service ->
                  hasBundleLocation(service, bundleLocations)
                      || hasMetatypesForService(service, metatypeInformation))
          .collect(Collectors.toList());
    } catch (VirtualMachineError e) {
      throw e;
    } catch (Throwable e) {
      LOGGER.info("Could not retrieve services", e);
      throw new ApplicationServiceBeanException("Could not retrieve services");
    }
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
        // Casting inline rather than with a class cast call on the stream
        // to work around a compiler quirk
        //        .map(Map.class::cast)
        .map(m -> ((Map) m).get("bundle_location"))
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
    try {
      return getFeatureMap(appService.getAllFeatures());
    } catch (VirtualMachineError e) {
      throw e;
    } catch (Throwable e) {
      LOGGER.error("Could not retrieve features. Restarting the system may resolve this.", e);
      throw new ApplicationServiceBeanException("Could not get all features");
    }
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
    try {
      List<Map<String, Object>> returnValues = new ArrayList<>();
      for (ApplicationPlugin plugin : applicationPlugins) {
        if (plugin.matchesAssocationName(appName)) {
          returnValues.add(plugin.toJSON());
        }
      }
      return returnValues;
    } catch (VirtualMachineError e) {
      throw e;
    } catch (Throwable e) {
      LOGGER.info("Could not retrieve plugins for application", e);
      throw new ApplicationServiceBeanException("Could not get plugins for application");
    }
  }

  @Override
  public void restart() {
    try {
      if (!restartServiceWrapperIfControlled()) {
        // create the restart.jvm file such that we would rely on the ddf script to restart
        // ourselves and not on karaf. This will have the advantage to restart anything else
        // (e.g. solr) that is also managed by that script
        LOGGER.debug("generating restart.jvm file");
        AccessController.doPrivileged(
            (PrivilegedExceptionAction<Void>)
                () -> {
                  FileUtils.touch(
                      Paths.get(System.getProperty("ddf.home"), "bin", "restart.jvm").toFile());
                  // make sure Karaf is not going to restart us as we want the ddf script to do it
                  System.setProperty("karaf.restart.jvm", "false");
                  systemService.halt();
                  return null;
                });
      }
      SecurityLogger.audit("Restarting system");
      LOGGER.info("Restarting the system.");
    } catch (PrivilegedActionException e) {
      SecurityLogger.audit("Failed to restart system");
      LOGGER.debug("failed to request a restart: ", e.getException());
    } catch (Exception e) {
      SecurityLogger.audit("Failed to restart system");
      LOGGER.debug("failed to request a restart: ", e);
    }
  }

  /**
   * serviceTracker setter method. Needed for use in unit tests.
   *
   * @param serviceTracker the desired serviceTracker instance
   */
  void setServiceTracker(ServiceTracker serviceTracker) {
    this.serviceTracker = (ServiceTracker<Object, Object>) serviceTracker;
  }

  private boolean restartServiceWrapperIfControlled()
      throws InstanceNotFoundException, MBeanException, ReflectionException,
          MalformedObjectNameException {
    if (System.getProperty("wrapper.key") != null) {
      LOGGER.debug("asking service wrapper to restart");
      ManagementFactory.getPlatformMBeanServer()
          .invoke(
              new ObjectName("org.tanukisoftware.wrapper:type=WrapperManager"),
              "restart",
              null,
              null);
      return true;
    }
    return false;
  }

  private static String computeLocation(Bundle bundle) {
    return BUNDLE_LOCATIONS.computeIfAbsent(bundle.getBundleId(), id -> bundle.getLocation());
  }

  /**
   * Exception to throw to ensure Jolokia registers an error. Also clears the stacktrace so details
   * do not leak to the UI.
   */
  private class ApplicationServiceBeanException extends RuntimeException {
    public ApplicationServiceBeanException(
        String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
    }

    ApplicationServiceBeanException(String message) {
      super(message, null, false, false);
      this.setStackTrace(new StackTraceElement[0]);
    }
  }
}
