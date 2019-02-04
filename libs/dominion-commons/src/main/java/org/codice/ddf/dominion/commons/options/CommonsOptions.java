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
package org.codice.ddf.dominion.commons.options;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.codice.dominion.options.Option;
import org.codice.dominion.options.Options;
import org.codice.dominion.options.Options.EnableRemoteDebugging;

/**
 * This class defines annotations that can be used to configure Dominion containers. It is solely
 * used for scoping.
 */
public class CommonsOptions {
  public static final String CUSTOM_SYSTEM_PROPERTIES = "etc/custom.system.properties";
  public static final String KARAF_MGMT_CFG = "etc/org.apache.karaf.management.cfg";
  public static final String KARAF_SHELL_CFG = "etc/org.apache.karaf.shell.cfg";

  /**
   * Dominion option to prepare the VM for testing. For example, it can set the appropriate required
   * memory constraints on the VM. It can also force the file encoding.
   */
  @Options.VMOption({"-Xmx4096M", "-Xms2048M"})
  // avoid integration tests stealing focus on OS X
  @Options.VMOption({"-Djava.awt.headless=true", "-Dfile.encoding=UTF8"})
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Documented
  public @interface ConfigureVMOptionsForTesting {}

  /**
   * Dominion option to configure debugging for DDF inside the container.
   *
   * <p>This option will support a system property named {@link
   * org.codice.dominion.options.Options.EnableKeepingRuntimeFolder#PROPERTY_KEY} for controlling
   * whether or not the runtime folder should be deleted at the end of testing. By default it will
   * be preserved. It will also prepare DDF to be remotely debugged with a debug port to be
   * dynamically allocated. The system property {@link EnableRemoteDebugging#PROPERTY_KEY} will
   * allow one DDF to actually wait for the debugger before starting up.
   */
  @Options.EnableKeepingRuntimeFolder
  @Options.EnableRemoteDebugging
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Documented
  public @interface ConfigureDebugging {}

  /**
   * Dominion options for dynamically allocating ports for HTTPS, HTTP, FTP, SSH, RMI, and Solr
   * HTTP.
   */
  @Options.UpdateConfigFile(
    target = CommonsOptions.CUSTOM_SYSTEM_PROPERTIES,
    key = "org.codice.ddf.system.httpsPort",
    value = "{port.https}"
  )
  @Options.UpdateConfigFile(
    target = CommonsOptions.CUSTOM_SYSTEM_PROPERTIES,
    key = "org.codice.ddf.system.httpPort",
    value = "{port.http}"
  )
  @Options.UpdateConfigFile(
    target = CommonsOptions.CUSTOM_SYSTEM_PROPERTIES,
    key = "org.codice.ddf.catalog.ftp.port",
    value = "{port.ftp}"
  )
  @Options.UpdateConfigFile(
    target = CommonsOptions.KARAF_MGMT_CFG,
    key = "rmiRegistryPort",
    value = "{port.rmi.registry}"
  )
  @Options.UpdateConfigFile(
    target = CommonsOptions.KARAF_MGMT_CFG,
    key = "rmiServerPort",
    value = "{port.rmi.server}"
  )
  @Options.UpdateConfigFile(
    target = CommonsOptions.KARAF_SHELL_CFG,
    key = "sshPort",
    value = "{port.ssh}"
  )
  @Options.UpdateConfigFile(
    target = CommonsOptions.CUSTOM_SYSTEM_PROPERTIES,
    key = "solr.http.port",
    value = "{port.solr}"
  )
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Documented
  public @interface ConfigurePorts {}

  /**
   * Dominion option for configuring log levels. It sets the <code>"ddf"</code> and <code>
   * "org.codice"</code> logger names to <code>INFO</code>. It also supports the system property
   * <code>"custom.logging"</code> to configure multiple loggers using the format <code>
   * "[logger.name]=[level];[logger.name]=[level]"</code>.
   */
  @Options.SetLogLevel(name = "ddf", level = "INFO")
  @Options.SetLogLevel(name = "org.codice", level = "INFO")
  @Options.SetLogLevels("{custom.logging:-}")
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Documented
  public @interface ConfigureLogging {}

  // these were built from the itests requirements. Uncomment and figure out where the actual
  // implementation should go: here or in the extension itself if it is too specific on PaxExam

  //  @Option.Annotation(GrantSolrSecurityPermissionsExtension.class)
  //  @Target(ElementType.TYPE)
  //  @Retention(RetentionPolicy.RUNTIME)
  //  @Inherited
  //  @Documented
  //  public @interface GrantSolrSecurityPermissions {}

  //  @Options.ReplaceFile(
  //      target = "/etc/definitions/injections.json",
  //      resource = "/injections.json"
  //  )
  //  @Target(ElementType.TYPE)
  //  @Retention(RetentionPolicy.RUNTIME)
  //  @Inherited
  //  @Documented
  //  public @interface InstallTestCatalogInjectionDefinition {
  //  }
  //
  //  @Options.ReplaceFile(target = "/etc/forms/forms.json", resource = "/etc/forms/forms.json")
  //  @Options.ReplaceFile(
  //      target = "/etc/forms/results.json",
  //      resource = "/etc/forms/results.json"
  //  )
  //  @Options.ReplaceFile(target = "/etc/forms/imagery.xml", resource = "/etc/forms/imagery.xml")
  //  @Options.ReplaceFile(
  //      target = "/etc/forms/contact-name.xml",
  //      resource = "/etc/forms/contact-name.xml"
  //  )
  //  @Target(ElementType.TYPE)
  //  @Retention(RetentionPolicy.RUNTIME)
  //  @Inherited
  //  @Documented
  //  public @interface InstallTestCatalogUIForms {
  //  }
  //
  //  @Options.ReplaceFile(
  //      target = "/etc/users.properties",
  //      resource = "/etc/test-users.properties"
  //  )
  //  @Options.ReplaceFile(
  //      target = "/etc/users.attributes",
  //      resource = "/etc/test-users.attributes"
  //  )
  //  @Target(ElementType.TYPE)
  //  @Retention(RetentionPolicy.RUNTIME)
  //  @Inherited
  //  @Documented
  //  public @interface InstallTestUserProperties {
  //  }

  /** Options to install the DDF Dominion common options in addition to the Dominion framework. */
  @Options.Install
  @Option.Annotation
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Documented
  public @interface Install {}

  private CommonsOptions() {}
}
