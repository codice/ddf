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

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;
import javax.xml.namespace.QName;

import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
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

public class SendEvent implements DeliveryMethod {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendEvent.class);

    private final TransformerManager transformerManager;

    private final URI callbackUrl;

    private final String outputSchema;

    private final ElementSetType elementSetType;

    private final List<QName> elementName;

    private final String mimeType;

    private final GetRecordsType request;

    private final ResultType resultType;

    private final QueryRequest query;

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
    }

    private String sendEvent(Operation operation, Metacard... metacards) {
        try (CloseableHttpClient httpClient = getClosableHttpClient()) {
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
            Map<String, Serializable> arguments = new HashMap<>();
            if (elementName != null) {
                arguments.put(CswConstants.ELEMENT_NAMES, elementName.toArray());
            }

            BinaryContent binaryContent = null;
            if (!CswConstants.CSW_OUTPUT_SCHEMA.equals(outputSchema)) {
                throw new WebApplicationException(new CatalogTransformerException(
                        "Unable to locate Transformer."));
            }
            arguments.put(CswConstants.OUTPUT_SCHEMA_PARAMETER, outputSchema);
            arguments.put(CswConstants.ELEMENT_SET_TYPE, elementSetType);
            arguments.put(CswConstants.IS_BY_ID_QUERY, false);
            arguments.put(CswConstants.GET_RECORDS, request);
            arguments.put(CswConstants.RESULT_TYPE_PARAMETER, resultType);
            arguments.put(CswConstants.WRITE_NAMESPACES, false);
            QueryResponseTransformer transformer = transformerManager.getTransformerBySchema(
                    outputSchema);
            binaryContent = transformer.transform(queryResponse, arguments);

            if (binaryContent == null) {
                throw new WebApplicationException(new CatalogTransformerException(
                        "Transformer returned null."));
            }

            HttpEntityEnclosingRequestBase httpMethod = getHttpRequest(operation);
            httpMethod.addHeader("Cache-Control", "no-cache, no-store");
            httpMethod.addHeader("Pragma", "no-cache");
            httpMethod.setEntity(new ByteArrayEntity(binaryContent.getByteArray()));
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            return httpClient.execute(httpMethod, responseHandler);
        } catch (Exception e) {
            LOGGER.error("Error sending event to {}.", callbackUrl, e);
        }
        return null;
    }

    private HttpEntityEnclosingRequestBase getHttpRequest(Operation operation) {
        HttpEntityEnclosingRequestBase httpMethod;
        switch (operation) {
        case UPDATE:
            httpMethod = new HttpPut(callbackUrl);
            break;
        case DELETE:
            httpMethod = new HttpDeleteWithBody(callbackUrl);
            break;
        default:
            httpMethod = new HttpPost(callbackUrl);
        }
        return httpMethod;
    }

    @Override
    public void created(Metacard newMetacard) {

        LOGGER.debug("Created {}", newMetacard);
        sendEvent(Operation.CREATE, newMetacard);
    }

    @Override
    public void updatedHit(Metacard newMetacard, Metacard oldMetacard) {
        LOGGER.debug("Updated Hit {} {}", newMetacard, oldMetacard);
        sendEvent(Operation.UPDATE, newMetacard);
    }

    @Override
    public void updatedMiss(Metacard newMetacard, Metacard oldMetacard) {
        LOGGER.debug("Updated Miss {} {}", newMetacard, oldMetacard);
        sendEvent(Operation.UPDATE, newMetacard);
    }

    @Override
    public void deleted(Metacard oldMetacard) {
        LOGGER.debug("Deleted {}", oldMetacard);
        sendEvent(Operation.DELETE, oldMetacard);

    }

    //for unit testing
    CloseableHttpClient getClosableHttpClient() {
        return HttpClients.createDefault();
    }

    public enum Operation {
        CREATE("CREATE"),
        UPDATE("UPDATE"),
        DELETE("DELETE");

        private final String name;

        Operation(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }
    }

    //Their is no delete with body as part of the apache http client but it is allowed in http 1.1 spec..
    @NotThreadSafe
    static class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
        public static final String METHOD_NAME = "DELETE";

        public String getMethod() {
            return METHOD_NAME;
        }

        public HttpDeleteWithBody(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        public HttpDeleteWithBody(final URI uri) {
            super();
            setURI(uri);
        }

        public HttpDeleteWithBody() {
            super();
        }
    }

}
