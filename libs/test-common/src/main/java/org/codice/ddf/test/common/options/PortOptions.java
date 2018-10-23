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
package org.codice.ddf.test.common.options;

import static org.codice.ddf.test.common.options.SystemProperties.SYSTEM_PROPERTIES_FILE_PATH;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.options.DefaultCompositeOption;

/** Options for configuring test environment ports */
public class PortOptions extends BasicOptions {

  public static final String KARAF_MGMT_CFG_FILE_PATH = "etc/org.apache.karaf.management.cfg";

  public static final String KARAF_SHELL_CFG_FILE_PATH = "etc/org.apache.karaf.shell.cfg";

  public static final String RMI_REGISTRY_PORT_PROPERTY = "rmiRegistryPort";

  public static final String RMI_SERVER_PORT_PROPERTY = "rmiServerPort";

  public static final String SSH_PORT_PROPERTY = "sshPort";

  private static final String HTTPS_PORT_KEY = "httpsPort";

  private static final String HTTP_PORT_KEY = "httpPort";

  private static final String FTP_PORT_KEY = "ftpPort";

  private static final String RMI_REG_PORT_KEY = "rmiRegistryPort";

  private static final String RMI_SERVER_PORT_KEY = "rmiServerPort";

  private static final String SSH_PORT_KEY = "sshPort";

  private static final String SOLR_PORT_KEY = "solrPort";

  public static Option defaultPortsOptions() {
    return new DefaultCompositeOption(
        httpsPort(),
        httpPort(),
        ftpPort(),
        rmiRegistryPort(),
        rmiServerPort(),
        sshPort(),
        solrPort());
  }

  public static Option httpsPort() {
    String port = getPortFinder().getPortAsString(HTTPS_PORT_KEY);
    recordConfiguration("%s=%s", HTTPS_PORT_KEY, port);
    return KarafDistributionOption.editConfigurationFilePut(
        SystemProperties.SYSTEM_PROPERTIES_FILE_PATH, SystemProperties.HTTPS_PORT_PROPERTY, port);
  }

  public static Option httpPort() {
    String port = getPortFinder().getPortAsString(HTTP_PORT_KEY);
    recordConfiguration("%s=%s", HTTP_PORT_KEY, port);
    return KarafDistributionOption.editConfigurationFilePut(
        SystemProperties.SYSTEM_PROPERTIES_FILE_PATH, SystemProperties.HTTP_PORT_PROPERTY, port);
  }

  public static Option ftpPort() {
    String port = getPortFinder().getPortAsString(FTP_PORT_KEY);
    recordConfiguration("%s=%s", FTP_PORT_KEY, port);
    return KarafDistributionOption.editConfigurationFilePut(
        SystemProperties.SYSTEM_PROPERTIES_FILE_PATH, SystemProperties.FTP_PORT_PROPERTY, port);
  }

  public static Option rmiRegistryPort() {
    String port = getPortFinder().getPortAsString(RMI_REG_PORT_KEY);
    recordConfiguration("%s=%s", RMI_REG_PORT_KEY, port);
    return KarafDistributionOption.editConfigurationFilePut(
        KARAF_MGMT_CFG_FILE_PATH, RMI_REGISTRY_PORT_PROPERTY, port);
  }

  public static Option rmiServerPort() {
    String port = getPortFinder().getPortAsString(RMI_SERVER_PORT_KEY);
    recordConfiguration("%s=%s", RMI_SERVER_PORT_KEY, port);
    return KarafDistributionOption.editConfigurationFilePut(
        KARAF_MGMT_CFG_FILE_PATH, RMI_SERVER_PORT_PROPERTY, port);
  }

  public static Option sshPort() {
    String port = getPortFinder().getPortAsString(SSH_PORT_KEY);
    recordConfiguration("%s=%s", SSH_PORT_KEY, port);
    return KarafDistributionOption.editConfigurationFilePut(
        KARAF_SHELL_CFG_FILE_PATH, SSH_PORT_PROPERTY, port);
  }

  public static Option solrPort() {
    String port = getPortFinder().getPortAsString(SSH_PORT_KEY);
    recordConfiguration("%s=%s", SOLR_PORT_KEY, port);
    return editConfigurationFilePut(SYSTEM_PROPERTIES_FILE_PATH, "solr.http.port", port);
  }
}
