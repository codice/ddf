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
package ddf.camel.component.catalog.inputtransformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;

import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.camel.component.catalog.CatalogEndpoint;
import ddf.camel.component.catalog.transformer.TransformerConsumer;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;

/**
 * Consumer for the custom Camel CatalogComponent. This {@link Consumer} would map to a Camel <from>
 * route node with a URI like <code>catalog:inputtransformer</code>
 * 
 * @author Hugh Rodgers, Lockheed Martin
 * @author William Miller, Lockheed Martin
 * @author ddf.isgs@lmco.com
 * 
 */
public class InputTransformerConsumer extends TransformerConsumer implements InputTransformer {
    private static final transient Logger LOGGER = LoggerFactory
            .getLogger(InputTransformerConsumer.class);

    /**
     * Constructs the consumer for the custom Camel CatalogComponent. This
     * {@link org.apache.camel.Consumer} would map to a Camel <from> route node with a URI like
     * <code>catalog:inputtransformer</code>
     * 
     * @param endpoint
     *            the Camel endpoint that created this consumer
     * @param processor
     */
    public InputTransformerConsumer(CatalogEndpoint endpoint, Processor processor) {
        super(InputTransformer.class, endpoint, processor);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("INSIDE InputTransformerConsumer constructor for "
                    + endpoint.getTransformerId());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.transform.InputTransformer#transform(java.io.InputStream)
     */
    @Override
    public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
        return transform(input, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.transform.InputTransformer#transform(java.io.InputStream, java.lang.String)
     */
    @Override
    public Metacard transform(InputStream input, String id) throws IOException,
        CatalogTransformerException {
        return transform(Metacard.class, input, new HashMap<String, Serializable>());
    }

}
