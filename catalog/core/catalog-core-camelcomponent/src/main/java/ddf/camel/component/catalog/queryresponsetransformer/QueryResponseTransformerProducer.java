/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.camel.component.catalog.queryresponsetransformer;

import ddf.camel.component.catalog.CatalogEndpoint;
import ddf.camel.component.catalog.transformer.TransformerProducer;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.mime.MimeTypeToTransformerMapper;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.camel.Message;
import org.codice.ddf.catalog.transform.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer for the custom Camel CatalogComponent. This {@link org.apache.camel.Producer} would map
 * to a Camel <to> route node with a URI like <code>catalog:queryresponsetransformer</code>
 *
 * @author William Miller, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public class QueryResponseTransformerProducer extends TransformerProducer {
  private static final transient Logger LOGGER =
      LoggerFactory.getLogger(QueryResponseTransformerProducer.class);

  private Transform transform;

  /**
   * Constructs the {@link Producer} for the custom Camel CatalogComponent. This producer would map
   * to a Camel <to> route node with a URI like <code>catalog:queryresponsetransformer</code>
   *
   * @param endpoint the Camel endpoint that created this consumer
   */
  public QueryResponseTransformerProducer(CatalogEndpoint endpoint, Transform transform) {
    super(endpoint);
    this.transform = transform;
  }

  protected Object transform(Message in, Object obj, String mimeType, String transformerId)
      throws MimeTypeParseException, CatalogTransformerException {
    // Look up the QueryResponseTransformer for the request's mime type.
    // If a transformer is found, then transform the request's payload into a BinaryContent
    // Otherwise, throw an exception.
    MimeType derivedMimeType = new MimeType(mimeType);

    if (transformerId != null) {
      derivedMimeType =
          new MimeType(mimeType + ";" + MimeTypeToTransformerMapper.ID_KEY + "=" + transformerId);
    }

    SourceResponse srcResp = in.getBody(SourceResponse.class);

    try {
      if (srcResp != null) {
        Map<String, Serializable> arguments = new HashMap<>();
        for (Entry<String, Object> entry : in.getHeaders().entrySet()) {
          if (entry.getValue() instanceof Serializable) {
            arguments.put(entry.getKey(), (Serializable) entry.getValue());
          }
        }
        return transform.transform(srcResp, derivedMimeType, arguments);
      }
    } catch (IllegalArgumentException e) {
      LOGGER.debug("Did not find an QueryResponseTransformer for [{}]", transformerId);
      throw new CatalogTransformerException(
          "Did not find an QueryResponseTransformer for [" + transformerId + "]");
    }

    return null;
  }
}
