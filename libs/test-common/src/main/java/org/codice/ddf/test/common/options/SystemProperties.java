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

public class SystemProperties {

  private SystemProperties() {}

  public static final String SYSTEM_PROPERTIES_FILE_PATH = "etc/custom.system.properties";

  public static final String HTTPS_PORT_PROPERTY = "org.codice.ddf.system.httpsPort";

  public static final String HTTP_PORT_PROPERTY = "org.codice.ddf.system.httpPort";

  public static final String FTP_PORT_PROPERTY = "org.codice.ddf.catalog.ftp.port";
}
