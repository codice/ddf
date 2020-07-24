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

import static java.lang.String.format;
import static org.codice.ddf.test.common.options.LoggingOptions.logLevelOption;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.DefaultCompositeOption;

/** Contains test options useful for debugging tests. */
public class DebugOptions extends BasicOptions {

  private static final String DEBUG_PORT_KEY = "debugPort";

  private static final String KEEP_RUNTIME_FOLDER_FLAG = "keepRuntimeFolder";

  private static final String CUSTOM_LOGGING = "custom.logging";

  private static final String WAIT_FOR_DEBUG_FLAG = "waitForDebug";
  /**
   * Prevents the deletion of the distribution used during testing.
   *
   * @return
   */
  public static Option keepRuntimeFolder() {
    String keepRuntimeFolder = System.getProperty(KEEP_RUNTIME_FOLDER_FLAG, "true");
    recordConfiguration(KEEP_RUNTIME_FOLDER_FLAG, keepRuntimeFolder);
    return when(Boolean.valueOf(keepRuntimeFolder))
        .useOptions(org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder());
  }

  /**
   * Enables ports for debugging. Setting the `waitForDebug` property to true will cause the test to
   * halt and wait for a remote debugger to attach before executing tests.
   *
   * @return
   */
  public static Option enableRemoteDebugging() {
    String port = getPortFinder().getPortAsString(DEBUG_PORT_KEY);
    recordConfiguration(DEBUG_PORT_KEY, port);
    final String DEBUG_OPTS =
        format(
            "-Xrunjdwp:transport=dt_socket,server=y,suspend=%s,address=%s",
            Boolean.getBoolean(WAIT_FOR_DEBUG_FLAG) ? "y" : "n", port);

    // Since DDF uses a different start script than what pax exam expects karaf to use, we must set
    // our own debug env variables
    return new DefaultCompositeOption(
        CoreOptions.environment("KARAF_DEBUG=true", "JAVA_DEBUG_OPTS=" + DEBUG_OPTS));
  }

  /**
   * Allows custom logging by passing in the `custom.logging` property with the value being in the
   * format of `[package]=[logLevel];[anotherPackage]=[anotherLogLevel]
   *
   * @return
   */
  public static Option enableCustomLogging() {
    return when(System.getProperty(CUSTOM_LOGGING) != null)
        .useOptions(customLogging(System.getProperty(CUSTOM_LOGGING)));
  }

  private static Option customLogging(String customLogging) {
    if (StringUtils.isEmpty(customLogging)) {
      return null;
    }

    appendConfiguration("custom_logging", customLogging);
    Map<String, String> parsedCustomLogging = parseCustomLogging(customLogging);

    List<Option> options =
        parsedCustomLogging.entrySet().stream()
            .map(e -> logLevelOption(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    return new DefaultCompositeOption(options.stream().toArray(Option[]::new));
  }

  private static Map<String, String> parseCustomLogging(String customLogging) {
    return Arrays.stream(customLogging.split(";"))
        .map(e -> e.split("="))
        .collect(Collectors.toMap(e -> e[0], e -> e[1]));
  }

  public static Option defaultDebuggingOptions() {
    return new DefaultCompositeOption(
        keepRuntimeFolder(), enableRemoteDebugging(), enableCustomLogging());
  }
}
