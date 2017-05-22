/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.commands.platform;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;

@Service
@Command(scope = PlatformCommands.NAMESPACE, name = "describe", description = "Provides a description of the platform")
public class DescribeCommand extends PlatformCommands {

    @Override
    public Object execute() throws Exception {
        System.out.printf("%s=%s%n", "Protocol", SystemBaseUrl.getProtocol());
        System.out.printf("%s=%s%n", "Host", SystemBaseUrl.getHost());
        System.out.printf("%s=%s%n", "Port", SystemBaseUrl.getPort());
        System.out.printf("%s=%s%n", "Root Context", SystemBaseUrl.getRootContext());
        System.out.printf("%s=%s%n", "External Http Port", SystemBaseUrl.getHttpPort());
        System.out.printf("%s=%s%n", "External Https Port", SystemBaseUrl.getHttpsPort());
        System.out.printf("%s=%s%n",
                "Internal Http Port",
                System.getProperty("org.codice.ddf.system.internalHttpPort"));
        System.out.printf("%s=%s%n",
                "Internal Https Port",
                System.getProperty("org.codice.ddf.system.internalHttpsPort"));

        System.out.printf("%s=%s%n", "Site Name", SystemInfo.getSiteName());
        System.out.printf("%s=%s%n", "Organization", SystemInfo.getOrganization());
        System.out.printf("%s=%s%n", "Contact", SystemInfo.getSiteContatct());
        System.out.printf("%s=%s%n", "Version", SystemInfo.getVersion());
        return null;
    }
}
