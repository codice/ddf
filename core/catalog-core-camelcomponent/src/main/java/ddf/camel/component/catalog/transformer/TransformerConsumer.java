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
package ddf.camel.component.catalog.transformer;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.camel.component.catalog.CatalogEndpoint;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.mime.MimeTypeToTransformerMapper;

/**
 * Consumer for the custom Camel CatalogComponent. This {@link org.apache.camel.Consumer} would map
 * to a Camel <from> route node with a URI like <code>catalog:queryresponsetransformer</code>
 * 
 * @author William Miller, Lockheed Martin
 * @author ddf.isgs@lmco.com
 * 
 */
public class TransformerConsumer extends DefaultConsumer {
    private static final transient Logger LOGGER = LoggerFactory
            .getLogger(TransformerConsumer.class);

    private final CatalogEndpoint endpoint;

    private ServiceRegistration registration;

    private Class<?> transformerClass;

    /**
     * Constructs the consumer for the custom Camel CatalogComponent. This {@link Consumer} would
     * map to a Camel <from> route node with a URI like
     * <code>catalog:queryresponsetransformer</code>
     * 
     * @param endpoint
     *            the Camel endpoint that created this consumer
     * @param processor
     */
    public TransformerConsumer(Class<?> transformerClass, CatalogEndpoint endpoint,
            Processor processor) {
        super(endpoint, processor);
        this.transformerClass = transformerClass;
        this.endpoint = endpoint;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("INSIDE TransformerConsumer constructor for "
                    + endpoint.getTransformerId());
        }
    }

    @Override
    protected void doStart() throws CatalogTransformerException {
        LOGGER.debug("ENTERING: doStart");

        try {
            super.doStart();
        } catch (Exception e) {
            throw new CatalogTransformerException("Failed to start Transformer Consumer", e);
        }
        Hashtable<String, String> props = new Hashtable<String, String>();
        if (endpoint.getTransformerId() != null) {
            props.put(MimeTypeToTransformerMapper.ID_KEY, endpoint.getTransformerId());
        }
        if (endpoint.getMimeType() != null) {
            props.put(MimeTypeToTransformerMapper.MIME_TYPE_KEY, endpoint.getMimeType());
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Registering as QueryResponseTransformer with id="
                    + endpoint.getTransformerId());
        }

        // Register this Catalog Consumer as an QueryResponseTransformer in the OSGi registry.
        // The transformer ID (e.g., id=text/xml;id=xml) specified in the Camel route node
        // is used as the "id" key for this QueryResponseTransformer in the OSGi registry.
        // (This is how the CatalogContentPlugin will be able to look up this transformer by
        // mimetype)
        registration = endpoint.getComponent().getBundleContext()
                .registerService(transformerClass.getName(), this, props);

        LOGGER.debug("EXITING: doStart");
    }

    @Override
    protected void doStop() throws CatalogTransformerException {
        LOGGER.debug("ENTERING: doStop");

        if (registration != null) {
            LOGGER.debug("Unregistering");
            registration.unregister();
        }
        try {
            super.doStop();
        } catch (Exception e) {
            throw new CatalogTransformerException("Failed to stop Transformer Consumer", e);
        }

        LOGGER.debug("EXITING: doStop");
    }

    protected <T> T transform(Class<T> objClass, Object upstreamResponse,
            Map<String, Serializable> arguments) throws CatalogTransformerException {
        LOGGER.debug("ENTERING: transform");

        T content = null;

        Exchange exchange = endpoint.createExchange();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("exchange pattern = " + exchange.getPattern());
        }

        // Copy SourceResponse into message body and arguments into headers
        Message in = exchange.getIn();
        in.setBody(upstreamResponse);
        for (Entry<String, Serializable> entry : arguments.entrySet()) {
            in.setHeader(entry.getKey(), entry.getValue());
        }

        try {
            // Send message to next processor in the route. This is configured to be a blocking
            // call and will wait until the entire route completes and returns the result.
            getProcessor().process(exchange);
            LOGGER.debug("AFTER process(exchange)");

            // Entire route has completed - get the output from the last node in the route.
            content = exchange.getOut().getBody(objClass);

            // Result should be a BinaryContent - getBody(BinaryContent) will return null if it
            // isn't
            // and it cannot be converted
            if (null == content) {
                LOGGER.debug("Unable to create " + objClass.getName() + " - throwing exception");
                throw new CatalogTransformerException("Unable to create t" + objClass.getName());
            }
        } catch (Exception e) {
            Exception e2 = exchange.getException();
            if (e2 instanceof CatalogTransformerException) {
                throw (CatalogTransformerException) e2;
            }
            getExceptionHandler().handleException("Error processing exchange", exchange,
                    exchange.getException());
        } finally {
            // log exception if an exception occurred and was not handled
            if (exchange.getException() != null) {
                getExceptionHandler().handleException("Error processing exchange", exchange,
                        exchange.getException());
            }
        }

        LOGGER.debug("EXITING: transform");

        return content;
    }

}
