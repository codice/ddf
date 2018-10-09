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
package org.codice.ddf.test.common.configurators;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.test.common.configurators.BundleOptionBuilder.BundleOption;
import org.codice.ddf.test.common.configurators.FeatureOptionBuilder.FeatureOption;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the basic DDF configuration {@link Option}s required to run with Pax Exam. The class
 * must be extended to implement {@link #getDistributionOptions()} and provide the distribution that
 * will be run in the test container.
 *
 * <p>Any other bundles and features required by the tests can be provided by extending this class
 * and implementing {@link #getBundleOptions()} and {@link #getFeatureOptions()}. Other
 * configuration options can be provided by overwriting {@link #getExtraOptions()}.
 *
 * <p>The following system properties can be used to customize the application's runtime behavior:
 *
 * <ul>
 *   <li>{@code logLevel}: sets logging to the level specified for all {@code ddf} and {@code
 *       org.codice.ddf} packages, unless {@code logPackages} is also provided.
 *   <li>{@code logPackages}: coma-separated list of packages {@code logLevel} will apply to; {@code
 *       ddf} and {@code org.codice.ddf} if not provided.
 * </ul>
 *
 * @see KarafOptions
 */
public abstract class DdfBaseOptions implements ApplicationOptions {

  /** Servlet context system property key */
  public static final String SERVLET_CONTEXT_KEY = "org.apache.cxf.servlet.context";

  /** Root context value */
  public static final String ROOT_CONTEXT = "/services";

  private static final String OSGI_SERVICE_HTTP_PORT_KEY = "org.osgi.service.http.port";

  private static final String OSGI_SERVICE_HTTP_PORT_SECURE_KEY =
      "org.osgi.service.http.port.secure";

  private static final String TEST_LOGLEVEL_PROPERTY = "logLevel";

  private static final String TEST_DEBUG_PACKAGE_PROPERTY = "logPackages";

  private static final String LOGGER_NAME = "log4j2.logger.%s.name";

  private static final String LOGGER_LEVEL = "log4j2.logger.%s.level";

  private static final String DEFAULT_LOG_PACKAGES = "ddf,org.codice.ddf";

  private static final Logger LOGGER = LoggerFactory.getLogger(DdfBaseOptions.class);

  private static final String SYSTEM_PROPERTIES_FILE = "etc/custom.system.properties";

  private static final String PAX_WEB_CONFIG_FILE = "etc/org.ops4j.pax.web.cfg";

  private static final String PAX_LOGGING_CONFIG_FILE = "etc/org.ops4j.pax.logging.cfg";

  private final PortFinder portFinder;

  /**
   * Constructor.
   *
   * @param portFinder instance of the {@link PortFinder} to use to assign unique ports to the new
   *     Pax Exam container
   */
  public DdfBaseOptions(PortFinder portFinder) {
    this.portFinder = portFinder;
  }

  @Override
  public Option get() {
    return composite(
        getSystemPropertyOptions(),
        getLoggingOptions(),
        getDistributionOptions(),
        getBundleOptions().build(),
        getFeatureOptions().build(),
        getExtraOptions());
  }

  /**
   * Gets the distribution artifact to deploy in the Pax Exam container, e.g., the Karaf or DDF
   * distribution zip file.
   *
   * @return distribution artifact to deploy
   */
  protected abstract Option getDistributionOptions();

  /**
   * Gets the {@link BundleOption} that contains all the bundles to start in the Pax Exam container
   * for the tests to run.
   *
   * <p><b>Important:</b> Since the bundle being tested will automatically be started by this class,
   * it should <i>not</i> be returned by this method.
   *
   * @return {@link BundleOption} that contains the bundles to start. Defaults to an empty list of
   *     bundles.
   */
  protected BundleOption getBundleOptions() {
    return BundleOptionBuilder.empty();
  }

  /**
   * Gets the {@link FeatureOption} that contains all the features to start in the Pax Exam
   * container for the tests to run.
   *
   * @return {@link FeatureOption} that contains the features to start. Defaults to an empty list of
   *     features.
   */
  protected FeatureOption getFeatureOptions() {
    return FeatureOptionBuilder.empty();
  }

  /**
   * Gets the extra Pax Exam {@link Option} to use to configure the Pax Exam container. {@link
   * org.ops4j.pax.exam.CoreOptions#composite(Option...)} can be used to combine multiple options
   * into one.
   *
   * @return extra {@link Option}. Defaults to empty.
   */
  protected Option getExtraOptions() {
    return CoreOptions.composite();
  }

  /**
   * Gets the list of packages whose logging level will be changed when the {@code logLevel} system
   * property has been defined. By default, the {@code ddf} and {@code org.codice.ddf} packages will
   * have their logging level set to the one specified by {@code logLevel}.
   *
   * @return coma-separated list of packages to include by default. Defaults to {@code ddf} and
   *     {@code org.codice.ddf}.
   */
  protected String getDefaultLogPackages() {
    return DEFAULT_LOG_PACKAGES;
  }

  private Option getSystemPropertyOptions() {
    return composite(
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_FILE,
            SystemBaseUrl.INTERNAL_PORT,
            portFinder.getPortAsString(SystemBaseUrl.INTERNAL_HTTP_PORT)),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_FILE,
            SystemBaseUrl.INTERNAL_HTTP_PORT,
            portFinder.getPortAsString(SystemBaseUrl.INTERNAL_HTTP_PORT)),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_FILE,
            SystemBaseUrl.INTERNAL_HTTPS_PORT,
            portFinder.getPortAsString(SystemBaseUrl.INTERNAL_HTTPS_PORT)),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_FILE, SystemBaseUrl.INTERNAL_PROTOCOL, "http://"),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_FILE,
            SystemBaseUrl.EXTERNAL_PORT,
            portFinder.getPortAsString(SystemBaseUrl.EXTERNAL_HTTP_PORT)),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_FILE,
            SystemBaseUrl.EXTERNAL_HTTP_PORT,
            portFinder.getPortAsString(SystemBaseUrl.EXTERNAL_HTTP_PORT)),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_FILE,
            SystemBaseUrl.EXTERNAL_HTTPS_PORT,
            portFinder.getPortAsString(SystemBaseUrl.EXTERNAL_HTTPS_PORT)),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_FILE, SystemBaseUrl.EXTERNAL_PROTOCOL, "http://"),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_FILE, SystemBaseUrl.INTERNAL_ROOT_CONTEXT, ROOT_CONTEXT),
        editConfigurationFilePut("etc/org.apache.cxf.osgi.cfg", SERVLET_CONTEXT_KEY, ROOT_CONTEXT),
        editConfigurationFilePut(
            "etc/org.apache.cxf.osgi.cfg",
            "org.apache.cxf.servlet.disable-address-updates",
            "true"),
        editConfigurationFilePut(
            PAX_WEB_CONFIG_FILE,
            OSGI_SERVICE_HTTP_PORT_KEY,
            portFinder.getPortAsString(SystemBaseUrl.EXTERNAL_HTTP_PORT)),
        editConfigurationFilePut(
            PAX_WEB_CONFIG_FILE,
            OSGI_SERVICE_HTTP_PORT_SECURE_KEY,
            portFinder.getPortAsString(SystemBaseUrl.EXTERNAL_HTTPS_PORT)),
        editConfigurationFilePut(PAX_WEB_CONFIG_FILE, "org.osgi.service.http.enabled", "true"),
        editConfigurationFilePut(SYSTEM_PROPERTIES_FILE, "ddf.home", "${karaf.home}"),
        editConfigurationFilePut(SYSTEM_PROPERTIES_FILE, SystemBaseUrl.EXTERNAL_HOST, "localhost"),
        editConfigurationFilePut(SYSTEM_PROPERTIES_FILE, SystemBaseUrl.INTERNAL_HOST, "localhost"),
        installStartupFile(
            getClass().getClassLoader().getResource("blacklisted.properties"),
            "/etc/blacklisted.properties"));
  }

  private Option getLoggingOptions() {
    List<Option> stdoutLoggerOptions = new ArrayList<>();
    stdoutLoggerOptions.add(
        editConfigurationFilePut(PAX_LOGGING_CONFIG_FILE, "log4j2.appender.stdout.name", "StdOut"));
    stdoutLoggerOptions.add(
        editConfigurationFilePut(
            PAX_LOGGING_CONFIG_FILE, "log4j2.appender.stdout.type", "Console"));
    stdoutLoggerOptions.add(
        editConfigurationFilePut(
            PAX_LOGGING_CONFIG_FILE, "log4j2.appender.stdout.layout.pattern", "${log4j2.pattern}"));
    stdoutLoggerOptions.add(
        editConfigurationFilePut(
            PAX_LOGGING_CONFIG_FILE, "log4j2.appender.stdout.layout.type", "PatternLayout"));
    stdoutLoggerOptions.add(
        editConfigurationFilePut(
            PAX_LOGGING_CONFIG_FILE, "log4j2.rootLogger.appenderRef.StdOut.ref", "StdOut"));

    String logLevel = System.getProperty(TEST_LOGLEVEL_PROPERTY);

    if (logLevel == null) {
      return composite(stdoutLoggerOptions.toArray(new Option[0]));
    }

    String packages = System.getProperty(TEST_DEBUG_PACKAGE_PROPERTY);

    if (packages == null) {
      packages = getDefaultLogPackages();
    }

    List<Option> options =
        Arrays.stream(packages.split(","))
            .map(String::trim)
            .peek(p -> LOGGER.info("Enabling {} logs for {}", logLevel, p))
            .map(p -> getLoggingOptions(p, logLevel))
            .collect(Collectors.toList());
    options.addAll(stdoutLoggerOptions);

    return composite(options.toArray(new Option[0]));
  }

  @SuppressWarnings(
      "squid:S2629") // Don't need to check for argument evaluation before logging errors
  private Option getLoggingOptions(String loggerName, String logLevel) {
    String loggerId = loggerName.replace('.', '_');
    LOGGER.error("Adding logger name: {} = {}", String.format(LOGGER_NAME, loggerId), loggerName);
    LOGGER.error("Adding logger level: {} = {}", String.format(LOGGER_LEVEL, loggerId), logLevel);
    return composite(
        editConfigurationFilePut(
            PAX_LOGGING_CONFIG_FILE, String.format(LOGGER_NAME, loggerId), loggerName),
        editConfigurationFilePut(
            PAX_LOGGING_CONFIG_FILE, String.format(LOGGER_LEVEL, loggerId), logLevel));
  }

  /**
   * Copies the content of a JAR resource to the destination specified before the container starts
   * up. Useful to add test configuration files before tests are run.
   *
   * @param resource URL to the JAR resource to copy
   * @param destination destination relative to DDF_HOME
   * @return option object
   * @throws IOException thrown if a problem occurs while copying the resource
   */
  protected Option installStartupFile(URL resource, String destination) {
    try (InputStream is = resource.openStream()) {
      return installStartupFile(is, destination);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private Option installStartupFile(InputStream is, String destination) {
    try {
      File tempFile = Files.createTempFile("StartupFile", ".temp").toFile();
      tempFile.deleteOnExit();
      FileUtils.copyInputStreamToFile(is, tempFile);
      return replaceConfigurationFile(destination, tempFile);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
