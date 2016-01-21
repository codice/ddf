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
package org.codice.ddf.configuration;

public class SystemInfo {

    public static final String SITE_NAME = "org.codice.ddf.system.siteName";

    public static final String SITE_CONTACT = "org.codice.ddf.system.siteContact";

    public static final String VERSION = "org.codice.ddf.system.version";

    public static final String ORGANIZATION = "org.codice.ddf.system.organization";

    private SystemInfo() {

    }

    public static String getSiteName() {
        return System.getProperty(SITE_NAME, "");
    }

    public static String getSiteContatct() {
        return System.getProperty(SITE_CONTACT, "");
    }

    public static String getVersion() {
        return System.getProperty(VERSION, "");
    }

    public static String getOrganization() {
        return System.getProperty(ORGANIZATION, "");
    }
}
