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
package org.codice.ddf.catalog.content.monitor.configurators;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.io.File;
import org.codice.ddf.catalog.content.monitor.features.KarafStandardFeatures;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;

public class KarafConfigurator {

  public static Option karafConfiguration() {
    return composite(
        karafDistributionConfiguration()
            .frameworkUrl(
                maven()
                    .groupId("org.apache.karaf")
                    .artifactId("apache-karaf")
                    .version(karafVersion())
                    .type("zip"))
            .unpackDirectory(new File("target", "exam"))
            .useDeployFolder(false),
        configureConsole().ignoreLocalConsole(),
        keepRuntimeFolder(),
        logLevel().logLevel(LogLevelOption.LogLevel.DEBUG),
        editConfigurationFilePut("etc/custom.system.properties", "ddf.home", "${karaf.home}"),
        editConfigurationFilePut(
            "etc/custom.system.properties", SystemBaseUrl.EXTERNAL_HOST, "localhost"),
        editConfigurationFilePut(
            "etc/custom.system.properties", SystemBaseUrl.INTERNAL_HOST, "localhost"),
        when(Boolean.getBoolean("isDebugEnabled")).useOptions(debugConfiguration()),
        KarafStandardFeatures.start("standard"));
  }

  public static String karafVersion() {
    ConfigurationManager cm = new ConfigurationManager();
    return cm.getProperty("pax.exam.karaf.version");
  }
}
