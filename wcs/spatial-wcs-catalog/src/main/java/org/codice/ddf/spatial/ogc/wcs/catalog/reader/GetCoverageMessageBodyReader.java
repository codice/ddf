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
package org.codice.ddf.spatial.ogc.wcs.catalog.reader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import org.codice.ddf.spatial.ogc.wcs.catalog.GetCoverageResponse;

/**
 * Custom JAX-RS MessageBodyReader for parsing a WCS GetCoverage response, extracting the raw
 * product data and the Content-Disposition HTTP header.
 * 
 * @author rodgersh
 * 
 */
public class GetCoverageMessageBodyReader implements MessageBodyReader<GetCoverageResponse> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {

        return GetCoverageResponse.class.isAssignableFrom(type);
    }

    @Override
    public GetCoverageResponse readFrom(Class<GetCoverageResponse> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
        throws IOException, WebApplicationException {

        GetCoverageResponse response = new GetCoverageResponse();
        response.setInputStream(entityStream);

        String contentDisposition = httpHeaders.getFirst("Content-Disposition");
        response.setContentDisposition(contentDisposition);

        return response;
    }

}
