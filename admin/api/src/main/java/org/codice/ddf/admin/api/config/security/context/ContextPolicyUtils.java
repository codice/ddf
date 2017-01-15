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

package org.codice.ddf.admin.api.config.security.context;

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createInvalidFieldMsg;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.common.util.StringUtils;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;

public class ContextPolicyUtils {

    public static List<ConfigurationMessage> validateContextPath(String contextPath) {
        List<ConfigurationMessage> msgs = new ArrayList<>();
        if (StringUtils.isEmpty(contextPath) || contextPath.startsWith("/")) {
            msgs.add(createInvalidFieldMsg("Improperly formatted context path", contextPath));
        }
        // TODO: tbatie - 1/14/17 - We can check for other characters that shouldn't be present in a url
        return msgs;
    }
}
