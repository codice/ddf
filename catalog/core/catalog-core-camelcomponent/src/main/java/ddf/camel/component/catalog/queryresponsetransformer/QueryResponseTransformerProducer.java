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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.camel.component.catalog.CatalogEndpoint;
import ddf.camel.component.catalog.transformer.TransformerProducer;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.mime.MimeTypeToTransformerMapper;


/**
 * Producer for the custom Camel CatalogComponent. This {@link org.apache.camel.Producer} would map to
 * a Camel <to> route node with a URI like <code>catalog:queryresponsetransformer</code>
 * 
 * @author William Miller, Lockheed Martin
 * @author ddf.isgs@lmco.com
 *
 */
public class QueryResponseTransformerProducer extends TransformerProducer 
{
    private static final transient Logger LOGGER = LoggerFactory.getLogger(QueryResponseTransformerProducer.class);
    
    /**
     * Constructs the {@link Producer} for the custom Camel CatalogComponent. This producer would map to
     * a Camel <to> route node with a URI like <code>catalog:queryresponsetransformer</code>
     * 
     * @param endpoint the Camel endpoint that created this consumer
     */
    public QueryResponseTransformerProducer(CatalogEndpoint endpoint) {
        super(endpoint);
    }
    
	protected Object transform(Message in, Object obj, String mimeType,
			String transformerId, MimeTypeToTransformerMapper mapper)
			throws MimeTypeParseException, CatalogTransformerException {
		// Look up the QueryResponseTransformer for the request's mime type.
		// If a transformer is found, then transform the request's payload into a BinaryContent
		// Otherwise, throw an exception.
		MimeType derivedMimeType = new MimeType(mimeType);

		if (transformerId != null) {
			derivedMimeType = new MimeType(mimeType + ";" + MimeTypeToTransformerMapper.ID_KEY + "="
					+ transformerId);
		}

		List<QueryResponseTransformer> matches = mapper.findMatches(QueryResponseTransformer.class, derivedMimeType);
		Object binaryContent = null;
		
		if ( matches != null && matches.size() == 1 )
		{
			Map<String, Serializable> arguments = new HashMap<String, Serializable>();
			for(Entry<String, Object> entry : in.getHeaders().entrySet()) {
				if(entry.getValue() instanceof Serializable) {
					arguments.put(entry.getKey(), (Serializable) entry.getValue());
				}
			}
			
		    LOGGER.debug( "Found a matching QueryResponseTransformer for [" + transformerId + "]" );
		    QueryResponseTransformer transformer = matches.get( 0 );
		    binaryContent = transformer.transform( in.getBody( SourceResponse.class ), arguments);
		}
		else
		{
		    LOGGER.debug( "Did not find an QueryResponseTransformer for [" + transformerId + "]" );
		    throw new CatalogTransformerException( "Did not find an QueryResponseTransformer for [" + transformerId + "]" );
		}
		return binaryContent;
	}

}
