/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.spatial.ogc.wfs.catalog.common;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WfsQnameBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(WfsQnameBuilder.class);

    public static QName buildQName(String metacardTypeName, String contentTypeName) {
        if (StringUtils.isEmpty(metacardTypeName) || StringUtils.isEmpty(contentTypeName)) {
            return null;
        }
        // ensure no "illegal" characters are used
        metacardTypeName = metacardTypeName.replace(WfsConstants.NAMESPACE_DELIMITER,
                WfsConstants.UNDERSCORE);
        contentTypeName = contentTypeName.replace(WfsConstants.NAMESPACE_DELIMITER,
                WfsConstants.UNDERSCORE);
        // LOGGER.debug("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ original content type name: " +
        // contentTypeName);
        if (contentTypeName.contains(" ")) {
            contentTypeName = contentTypeName.replaceAll(" ", "");
            // LOGGER.debug("######$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ converted contentTypeName: "
            // + contentTypeName);
        }

        // Build the QName to uniquely identify this content type
        String namespace;
        String prefix;
        if (metacardTypeName.equals(contentTypeName)) {
            prefix = contentTypeName;
            namespace = WfsConstants.NAMESPACE_URN_ROOT + metacardTypeName;
        } else {
            prefix = metacardTypeName + WfsConstants.DECIMAL + contentTypeName;
            namespace = WfsConstants.NAMESPACE_URN_ROOT + metacardTypeName + WfsConstants.DECIMAL
                    + contentTypeName;
        }
        // LOGGER.debug("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ Building new QName with namespace: "
        // + namespace + " contentTypeName: " + contentTypeName + " prefix: " + prefix);
        QName qname = new QName(namespace, contentTypeName, prefix);
        return qname;
    }

}
