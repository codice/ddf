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
package org.codice.ddf.platform.filter.clientinfo.constants;

/**
 * Constants for working with the client info map.
 *
 * The information currently all comes from the servlet API; specifically a select few getters
 * within {@link javax.servlet.ServletRequest}. The format of the keys follows the format of java
 * beans. The keys are camel-cased names without the preceeding 'get' found in the method name.
 *
 * For example, the key associated with {@link javax.servlet.ServletRequest#getRemoteAddr()} would
 * be the string {@code remoteAddr}.
 *
 * The only exception to this rule, {@link ClientInfoKeys#CLIENT_INFO_KEY}, which holds a value string
 * of {@code client-info}, is the key used to access the entire client information map. It may contain
 * different kinds of data that does not necessarily correlate to the servlet API.
 */
public class ClientInfoKeys {
    public static final String CLIENT_INFO_KEY = "client-info";

    public static final String SERVLET_REMOTE_ADDR = "remoteAddr";

    public static final String SERVLET_REMOTE_HOST = "remoteHost";

    public static final String SERVLET_SCHEME = "scheme";

    public static final String SERVLET_CONTEXT_PATH = "contextPath";
}
