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
package org.codice.ddf.admin.insecure.defaults.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.junit.Test;

public class PlatformGlobalConfigurationValidatorTest {

  private static final String HTTP_PROTOCOL = "http://";

  private static final String HTTPS_PROTOCOL = "https://";

  private static final String NULL_ADMIN_VALIDATE =
      "Unable to determine if Platform Global Configuration has insecure defaults. Cannot access Configuration Admin.";

  private static final String VALIDATE_EXCEPT =
      "Unable to determine if Platform Global Configuration has insecure defaults.";

  @Test
  public void testValidateWhenHttpIsEnabled() throws Exception {
    // Setup
    System.setProperty(SystemBaseUrl.EXTERNAL_PROTOCOL, HTTP_PROTOCOL);
    PlatformGlobalConfigurationValidator pgc = new PlatformGlobalConfigurationValidator();

    // Perform Test
    List<Alert> alerts = pgc.validate();

    // Verify
    assertThat(alerts.size(), is(1));
    assertThat(
        alerts.get(0).getMessage(),
        is(PlatformGlobalConfigurationValidator.PROTCOL_IN_PLATFORM_GLOBAL_CONFIG_IS_HTTP));
  }

  @Test
  public void testValidateWhenHttpIsDisabled() throws Exception {
    // Setup

    System.setProperty(SystemBaseUrl.EXTERNAL_PROTOCOL, HTTPS_PROTOCOL);
    PlatformGlobalConfigurationValidator pgc = new PlatformGlobalConfigurationValidator();

    // Perform Test
    List<Alert> alerts = pgc.validate();

    // Verify
    assertThat(alerts.size(), is(0));
  }
}
