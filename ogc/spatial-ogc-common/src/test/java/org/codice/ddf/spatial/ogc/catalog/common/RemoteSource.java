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
package org.codice.ddf.spatial.ogc.catalog.common;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.client.WebClient;

/**
 * Generic implementation of TrustedRemoteSource to for unit testing.
 * 
 */

public class RemoteSource extends TrustedRemoteSource implements GenericRemoteSource {

    private GenericRemoteSource grs;

    public RemoteSource(String url, boolean disableCnCheck) {
        grs = createClientBean(GenericRemoteSource.class, url, null, null,
                disableCnCheck, null, getClass().getClassLoader());
    }

    public void setKeystores(String keyStorePath, String keyStorePassword, String trustStorePath,
            String trustStorePassword) {
        this.configureKeystores(WebClient.client(grs), keyStorePath, keyStorePassword,
                trustStorePath, trustStorePassword);
    }

    @GET
    @Consumes({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    public String get() {
        return grs.get();
    }

}
