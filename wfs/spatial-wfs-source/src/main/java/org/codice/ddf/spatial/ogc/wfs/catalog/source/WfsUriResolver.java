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
package org.codice.ddf.spatial.ogc.wfs.catalog.source;

import org.apache.commons.lang.StringUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.resolver.DefaultURIResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * This class is used by {@link XmlSchema} to override the {@link DefaultURIResolver}. It is used to
 * resolve schemas defined by "import" and"include".
 * 
 */
public class WfsUriResolver extends DefaultURIResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(WfsUriResolver.class);
    private String gmlNamespace;
    private String wfsNamespace;
    

    /**
     * Try to resolve a schema location to some data. If the namespace is that of WFS or GML ignore
     * it because we already have our own bindings and this will only slow the schema creation down.
     * 
     * @param namespace
     *            target namespace.
     * @param schemaLocation
     *            system ID.
     * @param baseUri
     *            base URI for the schema.
     */
    public InputSource resolveEntity(String namespace, String schemaLocation, String baseUri) {
        if (StringUtils.isEmpty(gmlNamespace) || StringUtils.isEmpty(wfsNamespace)) {
            LOGGER.error("Defined constant GML or WFS namespace has not been set.");
            return null;
        }
        else if (gmlNamespace.equals(namespace) || wfsNamespace.equals(namespace)) {
            LOGGER.debug("Found WFS or GML namespace.");
            return null;
        }

        return super.resolveEntity(namespace, schemaLocation, baseUri);
    }
    
    public void setGmlNamespace(String gmlNamespace) {
        this.gmlNamespace = gmlNamespace;
    }

    public void setWfsNamespace(String wfsNamespace) {
        this.wfsNamespace = wfsNamespace;
    }
    
}
