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
package org.codice.ddf.commands.platform;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;

@Service
@Command(
    scope = PlatformCommands.NAMESPACE,
    name = "describe",
    description = "Provides a description of the platform")
public class DescribeCommand extends PlatformCommands {
  private String format = "%s=%s%n";

  @Override
  @SuppressWarnings("squid:s106" /* Console command */)
  public Object execute() throws Exception {
    System.out.printf(format, "Protocol", SystemBaseUrl.EXTERNAL.getProtocol());
    System.out.printf(format, "Host", SystemBaseUrl.EXTERNAL.getHost());
    System.out.printf(format, "Port", SystemBaseUrl.EXTERNAL.getPort());
    System.out.printf(format, "Root Context", SystemBaseUrl.EXTERNAL.getRootContext());
    System.out.printf(format, "External Http Port", SystemBaseUrl.EXTERNAL.getHttpPort());
    System.out.printf(format, "External Https Port", SystemBaseUrl.EXTERNAL.getHttpsPort());
    System.out.printf(
        format, "Internal Http Port", System.getProperty(SystemBaseUrl.INTERNAL.getHttpPort()));
    System.out.printf(
        format, "Internal Https Port", System.getProperty(SystemBaseUrl.INTERNAL.getHttpsPort()));
    System.out.printf(format, "Internal Protocol", SystemBaseUrl.INTERNAL.getProtocol());
    System.out.printf(format, "Internal Host", SystemBaseUrl.INTERNAL.getHost());
    System.out.printf(format, "Internal Port", SystemBaseUrl.INTERNAL.getPort());

    System.out.printf(format, "Site Name", SystemInfo.getSiteName());
    System.out.printf(format, "Organization", SystemInfo.getOrganization());
    System.out.printf(format, "Contact", SystemInfo.getSiteContatct());
    System.out.printf(format, "Version", SystemInfo.getVersion());
    return null;
  }
}
