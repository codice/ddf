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

import java.nio.file.Paths;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;

/** Options for handling test resources. */
public class TestResourcesOptions {

  public static final String TEST_RESOURCES_DIR = "test.resources.dir";

  public static final String DEFAULT_TEST_RESOURCES_PATH =
      Paths.get("target", "test-classes").toAbsolutePath().toString();

  /**
   * Allows test resources under "target/test-classes" to be accessed by the security manager during
   * test run time.
   *
   * @return
   */
  public static Option includeTestResources() {
    return KarafDistributionOption.editConfigurationFilePut(
        SystemProperties.SYSTEM_PROPERTIES_FILE_PATH,
        TEST_RESOURCES_DIR,
        DEFAULT_TEST_RESOURCES_PATH);
  }

  /**
   * Since Pax Exam uses 2 JVM's, the test resources in each of these JVM's differs. This method
   * resolves the proper path of the test resource, regardless of the JVM being used, if the test
   * resources dir is specified by {@link TestResourcesOptions#includeTestResources}.
   *
   * @param resourcePath
   * @return
   */
  public static String getTestResource(String resourcePath) {
    return System.getProperty(TEST_RESOURCES_DIR, DEFAULT_TEST_RESOURCES_PATH) + resourcePath;
  }
}
