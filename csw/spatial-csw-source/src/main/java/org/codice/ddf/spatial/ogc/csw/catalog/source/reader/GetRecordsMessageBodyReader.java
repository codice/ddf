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
package org.codice.ddf.spatial.ogc.csw.catalog.source.reader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.RecordConverterFactory;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.impl.GetRecordsResponseConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.xml.WstxDriver;

/**
 * Custom JAX-RS MessageBodyReader for parsing a CSW GetRecords response, extracting the search
 * results and CSW records.
 * 
 * @author rodgersh
 * 
 */
public class GetRecordsMessageBodyReader implements MessageBodyReader<CswRecordCollection> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetRecordsMessageBodyReader.class);

    private XStream xstream;

    public GetRecordsMessageBodyReader(List<RecordConverterFactory> factories, CswSourceConfiguration configuration) {
        xstream = new XStream(new WstxDriver());
        xstream.setClassLoader(this.getClass().getClassLoader());
        GetRecordsResponseConverter converter = new GetRecordsResponseConverter(factories);
        converter.setUnmarshalConverterSchema(configuration.getOutputSchema(),
                configuration.getMetacardCswMappings(), configuration.getProductRetrievalMethod(),
                configuration.getResourceUriMapping(), configuration.getThumbnailMapping(),
                configuration.isLonLatOrder());
        xstream.registerConverter(converter);
        xstream.alias("GetRecordsResponse", CswRecordCollection.class);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {

        return CswRecordCollection.class.isAssignableFrom(type);
    }

    @Override
    public CswRecordCollection readFrom(Class<CswRecordCollection> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream inStream) throws IOException,
        WebApplicationException {

        // Save original input stream for any exception message that might need to be
        // created
        String originalInputStream = IOUtils.toString(inStream, "UTF-8");
        LOGGER.debug("Converting to CswRecordCollection: \n {}", originalInputStream);

        // Re-create the input stream (since it has already been read for potential
        // exception message creation)
        inStream = new ByteArrayInputStream(originalInputStream.getBytes("UTF-8"));

        CswRecordCollection cswRecords = null;

        try {
            cswRecords = (CswRecordCollection) xstream.fromXML(inStream);
        } catch (XStreamException e) {
            // If an ExceptionReport is sent from the remote CSW site it will be sent with an
            // JAX-RS "OK" status, hence the ErrorResponse exception mapper will not fire.
            // Instead the ExceptionReport will come here and be treated like a GetRecords
            // response, resulting in an XStreamException since ExceptionReport cannot be
            // unmarshalled. So this catch clause is responsible for catching that XStream
            // exception and creating a JAX-RS response containing the original stream
            // (with the ExceptionReport) and rethrowing it as a WebApplicatioNException,
            // which CXF will wrap as a ClientException that the CswSource catches, converts
            // to a CswException, and logs.
            ByteArrayInputStream bis = new ByteArrayInputStream(originalInputStream.getBytes());
            ResponseBuilder responseBuilder = Response.ok(bis);
            responseBuilder.type("text/xml");
            Response response = responseBuilder.build();
            throw new WebApplicationException(e, response);
        } finally {
            IOUtils.closeQuietly(inStream);
        }

        return cswRecords;
    }

}
