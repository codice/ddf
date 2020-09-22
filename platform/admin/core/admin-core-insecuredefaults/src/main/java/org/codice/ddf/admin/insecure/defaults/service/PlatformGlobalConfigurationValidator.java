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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.admin.insecure.defaults.service.Alert.Level;
import org.codice.ddf.configuration.SystemBaseUrl;

public class PlatformGlobalConfigurationValidator implements Validator {

  static final String PROTCOL_IN_PLATFORM_GLOBAL_CONFIG_IS_HTTP =
      "The protocol in the system properties is set to [http].";

  private static final String HTTP_PROTOCOL = "http://";

  private List<Alert> alerts;

  public PlatformGlobalConfigurationValidator() {
    alerts = new ArrayList<>();
  }

  @Override
  public List<Alert> validate() {
    alerts = new ArrayList<>();
    validateHttpIsDisabled();
    return alerts;
  }

  private void validateHttpIsDisabled() {
    String protocol = SystemBaseUrl.EXTERNAL.getProtocol();

    if (StringUtils.equalsIgnoreCase(protocol, HTTP_PROTOCOL)) {
      alerts.add(new Alert(Level.WARN, PROTCOL_IN_PLATFORM_GLOBAL_CONFIG_IS_HTTP));
    }
  }
}
