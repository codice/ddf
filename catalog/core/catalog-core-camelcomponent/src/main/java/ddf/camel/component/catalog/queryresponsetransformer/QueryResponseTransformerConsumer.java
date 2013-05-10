/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.camel.component.catalog.queryresponsetransformer;

import java.io.Serializable;
import java.util.Map;

import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.camel.component.catalog.CatalogEndpoint;
import ddf.camel.component.catalog.transformer.TransformerConsumer;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;


/**
 * Consumer for the custom Camel CatalogComponent. This {@link Consumer} would map to
 * a Camel <from> route node with a URI like <code>catalog:queryresponsetransformer</code>
 * 
 * @author William Miller, Lockheed Martin
 * @author ddf.isgs@lmco.com
 *
 */
public class QueryResponseTransformerConsumer extends TransformerConsumer implements QueryResponseTransformer {

	private static final transient Logger LOGGER = LoggerFactory.getLogger( QueryResponseTransformerConsumer.class );    

    /**
     * Constructs the consumer for the custom Camel CatalogComponent. This {@link org.apache.camel.Consumer} would map to
     * a Camel <from> route node with a URI like <code>catalog:queryresponsetransformer</code>
     * 
     * @param endpoint the Camel endpoint that created this consumer
     * @param processor
     */
    public QueryResponseTransformerConsumer(CatalogEndpoint endpoint, Processor processor) {
        super(QueryResponseTransformer.class, endpoint, processor);

        if ( LOGGER.isDebugEnabled() ) {
            LOGGER.debug( "INSIDE QueryResponseTransformerConsumer constructor for " + endpoint.getTransformerId() );
        }
    }

    /* (non-Javadoc)
     * @see ddf.catalog.transform.QueryResponseTransformer#transform(SourceResponse upstreamResponse, Map<String, Serializable> arguments)
     */
    @Override
	public BinaryContent transform(SourceResponse upstreamResponse, Map<String, Serializable> arguments) 
			throws CatalogTransformerException {
        return transform(BinaryContent.class, upstreamResponse, arguments);
    }
}
