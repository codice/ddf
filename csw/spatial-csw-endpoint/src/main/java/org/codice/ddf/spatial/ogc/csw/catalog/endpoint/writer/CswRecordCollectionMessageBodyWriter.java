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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.writer;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.transformer.TransformerManager;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * CswRecordCollectionMessageBodyWriter generates an xml response for a {@link CswRecordCollection}
 *
 */
@Provider
public class CswRecordCollectionMessageBodyWriter implements MessageBodyWriter<CswRecordCollection> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(CswRecordCollectionMessageBodyWriter.class);

    private final TransformerManager transformerManager;

    private static final List<String> XML_MIME_TYPES = Arrays
            .asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML);

    public CswRecordCollectionMessageBodyWriter(TransformerManager manager) {
        this.transformerManager = manager;
    }
    
    @Override
    public long getSize(CswRecordCollection recordCollection, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return CswRecordCollection.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(CswRecordCollection recordCollection, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream outStream)
        throws IOException, WebApplicationException {

        final String mimeType = recordCollection.getMimeType();
        LOGGER.debug(
                "Attempting to transform RecordCollection with mime-type: {} & outputSchema: {}",
                mimeType, recordCollection.getOutputSchema());
        QueryResponseTransformer transformer;
        Map<String, Serializable> arguments = new HashMap<String, Serializable>();
        if (StringUtils.isBlank(recordCollection.getOutputSchema()) && StringUtils
                .isNotBlank(mimeType) && !XML_MIME_TYPES.contains(mimeType)) {
            transformer = transformerManager
                    .getTransformerByMimeType(mimeType);
        } else {
            transformer = transformerManager.getCswQueryResponseTransformer();
            if (recordCollection.getElementName() != null) {
                arguments.put(CswConstants.ELEMENT_NAMES,
                        recordCollection.getElementName().toArray());
            }
            arguments.put(CswConstants.OUTPUT_SCHEMA_PARAMETER, recordCollection.getOutputSchema());
            arguments.put(CswConstants.ELEMENT_SET_TYPE, recordCollection.getElementSetType());
            arguments.put(CswConstants.IS_BY_ID_QUERY, recordCollection.isById());
            arguments.put(CswConstants.IS_VALIDATE_QUERY, recordCollection.isValidateQuery());
            arguments.put(CswConstants.GET_RECORDS, recordCollection.getRequest());
            arguments.put(CswConstants.RESULT_TYPE_PARAMETER, recordCollection.getResultType().value());
        }

        if (transformer == null) {
            throw new WebApplicationException(new CatalogTransformerException("Unable to locate Transformer."));
        }

        BinaryContent content = null;
        try {
            content = transformer.transform(recordCollection.getSourceResponse(), arguments);
        } catch (CatalogTransformerException e) {
            LOGGER.warn("Failed to Transform CswRecordCollection.", e);
            throw new WebApplicationException(e);
        }

        if (content != null) {
            IOUtils.copy(content.getInputStream(), outStream);
        } else {
            throw new WebApplicationException();
        }

    }
}
