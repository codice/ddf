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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;

import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.codice.ddf.security.common.OutgoingSubjectRetrievalInterceptor;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSubscribe;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.writer.CswRecordCollectionMessageBodyWriter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.event.DeliveryMethod;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
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

    private final URL callbackUrl;

    private final String outputSchema;

    private final ElementSetType elementSetType;

    private final List<QName> elementName;

    private final String mimeType;

    private final GetRecordsType request;

    private final ResultType resultType;

    private final QueryRequest query;

    private volatile Subject subject;

    SecureCxfClientFactory<CswSubscribe> cxfClientFactory;

    public SendEvent(TransformerManager transformerManager, GetRecordsType request,
            QueryRequest query) throws CswException {

        URL deliveryMethodUrl;
        if (request.getResponseHandler() != null && !request.getResponseHandler()
                .isEmpty()) {

            try {
                deliveryMethodUrl = new URL(request.getResponseHandler()
                        .get(0));

            } catch (MalformedURLException e) {
                throw new CswException("Invalid ResponseHandler URL", e);
            }
            if (!"https".equals(deliveryMethodUrl.getProtocol())) {
                throw new CswException(
                        "Invalid protocol for response handler expected https but was "
                                + deliveryMethodUrl.getProtocol());
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

        List providers = ImmutableList.builder().add(new CswRecordCollectionMessageBodyWriter(transformerManager)).build();

        cxfClientFactory = new SecureCxfClientFactory<>(callbackUrl.toString(),
                CswSubscribe.class,
                providers,
                null,
                false,
                false);
        cxfClientFactory.addOutInterceptors(new OutgoingSubjectRetrievalInterceptor());
    }

    private Response sendEvent(String operation, Metacard... metacards) {
        try {
            List<Result> results = Arrays.asList(metacards)
                    .stream()
                    .map(ResultImpl::new)
                    .collect(Collectors.toList());

            QueryResponse queryResponse = new QueryResponseImpl(query,
                    results,
                    true,
                    metacards.length);
            CswRecordCollection recordCollection = new CswRecordCollection();

            recordCollection.setElementName(elementName);
            recordCollection.setElementSetType(elementSetType);
            recordCollection.setById(false);
            recordCollection.setRequest(request);
            recordCollection.setResultType(resultType);
            recordCollection.setDoWriteNamespaces(false);
            recordCollection.setMimeType(mimeType);
            recordCollection.setOutputSchema(outputSchema);

            if (subject == null && !ping()) {
                return null;
            }
            queryResponse.getRequest()
                    .getProperties()
                    .put(SecurityConstants.SECURITY_SUBJECT, subject);

            for (AccessPlugin plugin : getAccessPlugins()) {

                queryResponse = plugin.processPostQuery(queryResponse);
            }

            if (queryResponse.getResults()
                    .isEmpty()) {
                return null;
            }
            recordCollection.setSourceResponse(queryResponse);

            return send(operation, recordCollection);
        } catch (StopProcessingException | InvalidSyntaxException e) {
            LOGGER.debug("Unable to send event error running AccessPlugin processPostQuery. ", e);
        }
        return null;
    }

    private Response send(String operation, CswRecordCollection recordCollection) {
        WebClient webClient = cxfClientFactory.getWebClient();
        Response response = webClient.invoke(operation, recordCollection);
        subject = (Subject) response.getHeaders()
                .getFirst(Subject.class.toString());
        return response;
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

    public boolean ping() {
        send(HttpMethod.HEAD, null);
        return subject != null;
    }

    List<AccessPlugin> getAccessPlugins() throws InvalidSyntaxException {
        BundleContext bundleContext =
                FrameworkUtil.getBundle(OutgoingSubjectRetrievalInterceptor.class)
                        .getBundleContext();
        Collection<ServiceReference<AccessPlugin>> serviceCollection =
                bundleContext.getServiceReferences(AccessPlugin.class, null);
        return serviceCollection.stream()
                .map(bundleContext::getService)
                .collect(Collectors.toList());
    }

}
