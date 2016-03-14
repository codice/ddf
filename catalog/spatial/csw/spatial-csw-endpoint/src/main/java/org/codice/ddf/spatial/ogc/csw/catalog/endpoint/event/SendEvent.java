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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSubscribe;
import org.codice.ddf.spatial.ogc.csw.catalog.transformer.TransformerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.event.DeliveryMethod;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.ResultType;

/**
 * SendEvent provides a implementation of {@link DeliveryMethod} for sending events to a CSW subscription event endpoint
 */
public class SendEvent implements DeliveryMethod {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendEvent.class);

    private static final List<String> XML_MIME_TYPES = Collections.unmodifiableList(Arrays.asList(
            MediaType.APPLICATION_XML,
            MediaType.TEXT_XML));

    private final TransformerManager transformerManager;

    private final URI callbackUrl;

    private final String outputSchema;

    private final ElementSetType elementSetType;

    private final List<QName> elementName;

    private final String mimeType;

    private final GetRecordsType request;

    private final ResultType resultType;

    private final QueryRequest query;

    WebClient webClient;

    public SendEvent(TransformerManager transformerManager, GetRecordsType request,
            QueryRequest query) throws CswException {

        URI deliveryMethodUrl;
        if (request.getResponseHandler() != null && !request.getResponseHandler()
                .isEmpty()) {

            try {
                deliveryMethodUrl = new URI(request.getResponseHandler()
                        .get(0));
            } catch (URISyntaxException e) {
                throw new CswException("Invalid ResponseHandler URL", e);
            }
        } else {
            String msg = "Subscriptions require a ResponseHandler URL to be specified";
            LOGGER.error(msg);
            throw new CswException(msg);
        }
        this.transformerManager = transformerManager;
        this.query = query;
        this.callbackUrl = deliveryMethodUrl;
        this.request = request;
        this.outputSchema = request.getOutputSchema();
        this.mimeType = request.getOutputFormat();
        QueryType queryType = (QueryType) request.getAbstractQuery()
                .getValue();
        this.elementName = queryType.getElementName();
        this.elementSetType = (queryType.getElementSetName() != null) ?
                queryType.getElementSetName()
                        .getValue() :
                null;
        this.resultType =
                request.getResultType() == null ? ResultType.HITS : request.getResultType();
        SecureCxfClientFactory<CswSubscribe> cxfClientFactory = new SecureCxfClientFactory<>(
                callbackUrl.toString(),
                CswSubscribe.class);
        webClient = cxfClientFactory.getWebClient();
    }

    private Response sendEvent(String operation, Metacard... metacards) {
        try {
            List<Result> results = Arrays.asList(metacards)
                    .stream()
                    .map(ResultImpl::new)
                    .collect(Collectors.toList());

            SourceResponse queryResponse = new QueryResponseImpl(query,
                    results,
                    true,
                    metacards.length);

            LOGGER.debug("Attempting to transform an Event with mime-type: {} & outputSchema: {}",
                    mimeType,
                    outputSchema);

            QueryResponseTransformer transformer;
            Map<String, Serializable> arguments = new HashMap<>();
            if (StringUtils.isBlank(outputSchema) && StringUtils.isNotBlank(mimeType)
                    && !XML_MIME_TYPES.contains(mimeType)) {
                transformer = transformerManager.getTransformerByMimeType(mimeType);
            } else {
                transformer =
                        transformerManager.getTransformerBySchema(CswConstants.CSW_OUTPUT_SCHEMA);
                if (elementName != null) {
                    arguments.put(CswConstants.ELEMENT_NAMES, elementName.toArray());
                }
                arguments.put(CswConstants.OUTPUT_SCHEMA_PARAMETER, outputSchema);
                arguments.put(CswConstants.ELEMENT_SET_TYPE, elementSetType);
                arguments.put(CswConstants.IS_BY_ID_QUERY, false);
                arguments.put(CswConstants.GET_RECORDS, request);
                arguments.put(CswConstants.RESULT_TYPE_PARAMETER, resultType);
                arguments.put(CswConstants.WRITE_NAMESPACES, false);
            }
            if (transformer == null) {
                throw new WebApplicationException(new CatalogTransformerException(
                        "Unable to locate Transformer."));
            }

            BinaryContent binaryContent = transformer.transform(queryResponse, arguments);

            if (binaryContent == null) {
                throw new WebApplicationException(new CatalogTransformerException(
                        "Transformer returned null."));
            }

            return webClient.invoke(operation, binaryContent.getByteArray());
        } catch (IOException | CatalogTransformerException | RuntimeException e) {
            LOGGER.error("Error sending event to {}.", callbackUrl, e);
        }
        return null;
    }

    @Override
    public void created(Metacard newMetacard) {

        LOGGER.debug("Created {}", newMetacard);
        sendEvent(HttpMethod.POST, newMetacard);
    }

    @Override
    public void updatedHit(Metacard newMetacard, Metacard oldMetacard) {
        LOGGER.debug("Updated Hit {} {}", newMetacard, oldMetacard);
        sendEvent(HttpMethod.PUT, newMetacard, oldMetacard);
    }

    @Override
    public void updatedMiss(Metacard newMetacard, Metacard oldMetacard) {
        LOGGER.debug("Updated Miss {} {}", newMetacard, oldMetacard);
        sendEvent(HttpMethod.PUT, newMetacard, oldMetacard);
    }

    @Override
    public void deleted(Metacard oldMetacard) {
        LOGGER.debug("Deleted {}", oldMetacard);
        sendEvent(HttpMethod.DELETE, oldMetacard);

    }

}
