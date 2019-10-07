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
package org.codice.ddf.itests.common.config;

import java.io.IOException;
import java.util.Dictionary;
import java.util.List;
import org.codice.ddf.configuration.DictionaryMap;
import org.codice.ddf.itests.common.AdminConfig;
import org.osgi.service.cm.Configuration;

public class ConfigureTestCommons {

  private static final String METACARD_VALIDITY_FILTER_PLUGIN_SERVICE_PID =
      "ddf.catalog.metacard.validation.MetacardValidityFilterPlugin";

  private static final String METACARD_VALIDITY_MARKER_PLUGIN_SERVICE_PID =
      "ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin";

  private static final String METACARD_ATTRIBUTE_SECURITY_POLICY_PLUGIN_PID =
      "org.codice.ddf.catalog.security.policy.metacard.MetacardAttributeSecurityPolicyPlugin";

  private static final String AUTH_Z_REALM_PID = "ddf.security.pdp.realm.AuthzRealm";

  private static final String ADMIN_CONFIG_POLICY_PID =
      "org.codice.ddf.admin.config.policy.AdminConfigPolicy";

  private ConfigureTestCommons() {}

  public static Dictionary<String, Object> configureService(
      String pid, Dictionary<String, Object> newProps, AdminConfig adminConfig) throws IOException {
    Configuration config = adminConfig.getConfiguration(pid, null);
    Dictionary<String, Object> oldProps = config.getProperties();
    config.update(newProps);
    return oldProps;
  }

  /**
   * Configures the MetacardValidityFilterPlugin. See metatype for more detailed descriptions.
   *
   * @param securityAttributeMappings - attribute mapping between metacard attribute and user
   *     attribute
   * @param filterErrors - sets whether metacards with validation errors are filtered
   * @param filterWarnings - sets whether metacards with validation warnings are filtered
   * @return A dictionary containing the old properties that the service had.
   */
  public static Dictionary<String, Object> configureMetacardValidityFilterPlugin(
      List<String> securityAttributeMappings,
      boolean filterErrors,
      boolean filterWarnings,
      AdminConfig configAdmin)
      throws IOException {
    Dictionary<String, Object> properties = new DictionaryMap<>();
    properties.put("attributeMap", securityAttributeMappings.toArray(new String[0]));
    properties.put("filterErrors", filterErrors);
    properties.put("filterWarnings", filterWarnings);
    return configureMetacardValidityFilterPlugin(properties, configAdmin);
  }

  /**
   * Configures the MetacardValidityFilterPlugin. This method should only be used to reset a service
   * configuration using a dictionary returned from the overloaded configuration method.
   *
   * @param props - dictionary of properties provided from the configuration
   * @return A dictionary containing the old properties that the service had.
   */
  public static Dictionary<String, Object> configureMetacardValidityFilterPlugin(
      Dictionary<String, Object> props, AdminConfig configAdmin) throws IOException {
    return configureService(METACARD_VALIDITY_FILTER_PLUGIN_SERVICE_PID, props, configAdmin);
  }

  /**
   * Configures the MetacardValidityMarkerPlugin. See metatype for more detailed descriptions.
   *
   * @param enforcedValidators - names of validators that will be enforced (reject errors/warnings)
   * @param enforceErrors - sets whether metacards with validation errors are rejected
   * @param enforceWarnings - sets whether metacards with validation warnings are rejected
   * @return A dictionary containing the old properties that the service had.
   */
  public static Dictionary<String, Object> configureValidationMarkerPlugin(
      List<String> enforcedValidators,
      boolean enforceErrors,
      boolean enforceWarnings,
      AdminConfig configAdmin)
      throws IOException {
    Dictionary<String, Object> properties = new DictionaryMap<>();
    properties.put("enforcedMetacardValidators", enforcedValidators.toArray(new String[0]));
    properties.put("enforceErrors", enforceErrors);
    properties.put("enforceWarnings", enforceWarnings);
    return configureValidationMarkerPlugin(properties, configAdmin);
  }

  /**
   * Configures the MetacardValidityMarkerPlugin. This method should only be used to reset a service
   * configuration using a dictionary returned from the overloaded configuration method.
   *
   * @param props - dictionary of properties provided from the configuration
   * @return A dictionary containing the old properties that the service had.
   */
  public static Dictionary<String, Object> configureValidationMarkerPlugin(
      Dictionary<String, Object> props, AdminConfig configAdmin) throws IOException {
    return configureService(METACARD_VALIDITY_MARKER_PLUGIN_SERVICE_PID, props, configAdmin);
  }

  /**
   * Configures the MetacardAttributeSecurityPolicyPlugin. See metatype for more detailed
   * descriptions.
   *
   * @param intersectAttributes - intersection mapping between source and destination attributes.
   * @param unionAttributes - union mapping between source and destination attributes.
   * @return A dictionary containing the old properties that the service had.
   */
  public static Dictionary<String, Object> configureMetacardAttributeSecurityFiltering(
      List<String> intersectAttributes, List<String> unionAttributes, AdminConfig configAdmin)
      throws IOException {
    Dictionary<String, Object> properties = new DictionaryMap<>();
    properties.put("intersectMetacardAttributes", intersectAttributes.toArray(new String[0]));
    properties.put("unionMetacardAttributes", unionAttributes);
    return configureMetacardAttributeSecurityFiltering(properties, configAdmin);
  }

  /**
   * Configures the MetacardAttributeSecurityPolicyPlugin. This method should only be used to reset
   * a service configuration using a dictionary returned from the overloaded configuration method.
   *
   * @param properties - dictionary of properties provided from the configuration
   * @return A dictionary containing the old properties that the service had.
   */
  public static Dictionary<String, Object> configureMetacardAttributeSecurityFiltering(
      Dictionary<String, Object> properties, AdminConfig configAdmin) throws IOException {
    return configureService(METACARD_ATTRIBUTE_SECURITY_POLICY_PLUGIN_PID, properties, configAdmin);
  }

  /**
   * Configures the AuthzRealm. See metatype for more detailed descriptions.
   *
   * @param matchAllMappings - list of 'Match-All' subject attribute to Metacard attribute mapping
   * @param matchOneAttributes - list of 'Match-One' subject attribute to Metacard attribute mapping
   * @param environmentAttributes - List of environment attributes to pass to the XACML engine
   * @return A dictionary containing the old properties that the service had.
   */
  public static Dictionary<String, Object> configureAuthZRealm(
      List<String> matchAllMappings,
      List<String> matchOneAttributes,
      List<String> environmentAttributes,
      AdminConfig configAdmin)
      throws IOException {
    Dictionary<String, Object> properties = new DictionaryMap<>();
    properties.put("matchAllMappings", matchAllMappings.toArray(new String[0]));
    properties.put("matchOneMappings", matchOneAttributes.toArray(new String[0]));
    properties.put("environmentAttributes", environmentAttributes.toArray(new String[0]));
    return configureAuthZRealm(properties, configAdmin);
  }

  /**
   * Configures the AuthzRealm. This method should only be used to reset a service configuration
   * using a dictionary returned from the overloaded configuration method.
   *
   * @param properties - dictionary of properties provided from the configuration
   * @return A dictionary containing the old properties that the service had.
   */
  public static Dictionary<String, Object> configureAuthZRealm(
      Dictionary<String, Object> properties, AdminConfig configAdmin) throws IOException {
    return configureService(AUTH_Z_REALM_PID, properties, configAdmin);
  }

  /**
   * Configures the AdminConfigPolicy. See metatype for more detailed descriptions.
   *
   * @param featurePolicies - features or apps that will only be modifiable and viewable to users
   *     with the set attributes
   * @param servicePolicies - services that will only be modifiable and viewable to users with the
   *     set attributes
   * @return A dictionary containing the old properties that the service had.
   */
  public static Dictionary<String, Object> configureAdminConfigPolicy(
      List<String> featurePolicies, List<String> servicePolicies, AdminConfig configAdmin)
      throws IOException {
    Dictionary<String, Object> properties = new DictionaryMap<>();
    properties.put("featurePolicies", featurePolicies.toArray(new String[0]));
    properties.put("servicePolicies", servicePolicies.toArray(new String[0]));
    return configureAdminConfigPolicy(properties, configAdmin);
  }

  /**
   * Configures the AdminConfigPolicy. This method should only be used to reset a service
   * configuration using a dictionary returned from the overloaded configuration method.
   *
   * @param properties - dictionary of properties provided from the configuration
   * @return A dictionary containing the old properties that the service had.
   */
  public static Dictionary<String, Object> configureAdminConfigPolicy(
      Dictionary<String, Object> properties, AdminConfig configAdmin) throws IOException {
    return configureService(ADMIN_CONFIG_POLICY_PID, properties, configAdmin);
  }
}
