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

  public static final String METACARD_VALIDITITY_FILTER_PLUGIN_SERVICE_PID =
      "ddf.catalog.metacard.validation.MetacardValidityFilterPlugin";

  public static final String METACARD_VALIDITITY_MARKER_PLUGIN_SERVICE_PID =
      "ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin";

  public static final String METACARD_ATTRIBUTE_SECURITY_POLICY_PLUGIN_PID =
      "org.codice.ddf.catalog.security.policy.metacard.MetacardAttributeSecurityPolicyPlugin";

  public static final String AUTH_Z_REALM_PID = "ddf.security.pdp.realm.AuthzRealm";

  private ConfigureTestCommons() {}

  public static void configureMetacardValidityFilterPlugin(
      List<String> securityAttributeMappings, AdminConfig configAdmin) throws IOException {
    Configuration config =
        configAdmin.getConfiguration(METACARD_VALIDITITY_FILTER_PLUGIN_SERVICE_PID, null);
    Dictionary<String, Object> properties = new DictionaryMap<>();
    properties.put("attributeMap", securityAttributeMappings);
    config.update(properties);
  }

  public static void configureFilterInvalidMetacards(
      String filterErrors, String filterWarnings, AdminConfig configAdmin) throws IOException {
    Configuration config =
        configAdmin.getConfiguration(METACARD_VALIDITITY_FILTER_PLUGIN_SERVICE_PID, null);

    Dictionary<String, Object> properties = new DictionaryMap<>();
    properties.put("filterErrors", filterErrors);
    properties.put("filterWarnings", filterWarnings);
    config.update(properties);
  }

  public static void configureEnforceValidityErrorsAndWarnings(
      String enforceErrors, String enforceWarnings, AdminConfig configAdmin) throws IOException {
    Configuration config =
        configAdmin.getConfiguration(METACARD_VALIDITITY_MARKER_PLUGIN_SERVICE_PID, null);

    Dictionary<String, Object> properties = new DictionaryMap<>();
    properties.put("enforceErrors", enforceErrors);
    properties.put("enforceWarnings", enforceWarnings);
    config.update(properties);
  }

  public static void configureEnforcedMetacardValidators(
      List<String> enforcedValidators, AdminConfig configAdmin) throws IOException {

    // Update metacardMarkerPlugin config with no enforcedMetacardValidators
    Configuration config =
        configAdmin.getConfiguration(METACARD_VALIDITITY_MARKER_PLUGIN_SERVICE_PID, null);

    Dictionary<String, Object> properties = new DictionaryMap<>();
    properties.put("enforcedMetacardValidators", enforcedValidators);
    config.update(properties);
  }

  public static Dictionary<String, Object> configureMetacardAttributeSecurityFiltering(
      List<String> intersectAttributes, List<String> unionAttributes, AdminConfig configAdmin)
      throws IOException {
    Dictionary<String, Object> properties = new DictionaryMap<>();
    properties.put("intersectMetacardAttributes", intersectAttributes);
    properties.put("unionMetacardAttributes", unionAttributes);
    return configureMetacardAttributeSecurityFiltering(properties, configAdmin);
  }

  public static Dictionary<String, Object> configureMetacardAttributeSecurityFiltering(
      Dictionary<String, Object> properties, AdminConfig configAdmin) throws IOException {
    Configuration config =
        configAdmin.getConfiguration(METACARD_ATTRIBUTE_SECURITY_POLICY_PLUGIN_PID, null);

    Dictionary oldProperties = config.getProperties();
    config.update(properties);
    return oldProperties;
  }

  public static Dictionary<String, Object> configureAuthZRealm(
      List<String> attributes, AdminConfig configAdmin) throws IOException {
    Dictionary<String, Object> properties = new DictionaryMap<>();
    properties.put("matchOneMap", attributes);
    return configureAuthZRealm(properties, configAdmin);
  }

  public static Dictionary<String, Object> configureAuthZRealm(
      Dictionary<String, Object> properties, AdminConfig configAdmin) throws IOException {
    Configuration config = configAdmin.getConfiguration(AUTH_Z_REALM_PID, null);

    Dictionary oldProperties = config.getProperties();
    config.update(properties);
    return oldProperties;
  }
}
