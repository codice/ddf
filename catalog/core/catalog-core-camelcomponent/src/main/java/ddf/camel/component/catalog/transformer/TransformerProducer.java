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
package ddf.camel.component.catalog.transformer;

import ddf.camel.component.catalog.CatalogComponent;
import ddf.camel.component.catalog.CatalogEndpoint;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.IOException;
import javax.activation.MimeTypeParseException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.codice.ddf.catalog.transform.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer for the custom Camel CatalogComponent. This {@link org.apache.camel.Producer} would map
 * to a Camel <to> route node with a URI like <code>catalog:inputtransformer</code>
 *
 * @author Hugh Rodgers, Lockheed Martin
 * @author William Miller, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public abstract class TransformerProducer extends DefaultProducer {
  private static final transient Logger LOGGER = LoggerFactory.getLogger(TransformerProducer.class);

  private CatalogEndpoint endpoint;

  /**
   * Constructs the {@link Producer} for the custom Camel CatalogComponent. This producer would map
   * to a Camel <to> route node with a URI like <code>catalog:inputtransformer</code>
   *
   * @param endpoint the Camel endpoint that created this consumer
   */
  public TransformerProducer(CatalogEndpoint endpoint) {
    super(endpoint);
    this.endpoint = endpoint;

    LOGGER.debug(
        "\"INSIDE InputTransformerProducer constructor for {}", endpoint.getTransformerId());
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.camel.Processor#process(org.apache.camel.Exchange)
   */
  public void process(Exchange exchange)
      throws CatalogTransformerException, MimeTypeParseException, IOException {

    LOGGER.debug("ENTERING: process");

    LOGGER.debug("exchange pattern = {}", exchange.getPattern());

    Message in = exchange.getIn();
    Object metacard = null;

    // Get the MIME Type and ID of the transformer to use to transform the
    // request's payload from the endpoint that manages this producer

    String transformerId = in.getHeader(CatalogComponent.ID_PARAMETER, String.class);

    String mimeType = in.getHeader(CatalogComponent.MIME_TYPE_PARAMETER, String.class);

    if (transformerId != null) {
      LOGGER.debug("transformerId retrieved from message header");
      in.removeHeader(CatalogComponent.ID_PARAMETER);
    } else {
      LOGGER.debug("transformerId retrieved from CamelCatalogEndpoint");
      transformerId = endpoint.getTransformerId();
    }

    LOGGER.debug("transformerId = {}", transformerId);

    if (mimeType != null) {
      in.removeHeader(CatalogComponent.MIME_TYPE_PARAMETER);
    } else {
      LOGGER.debug("MIME Type retrieved from CamelCatalogEndpoint");
      mimeType = endpoint.getMimeType();
    }

    LOGGER.debug("MIME Type = {}", mimeType);

    Transform transform = endpoint.getComponent().getTransform();

    if (transform != null) {
      LOGGER.trace("Got a Transform service");

      metacard = transform(in, metacard, mimeType, transformerId);
    } else {
      throw new CatalogTransformerException("Did not find a Transform service");
    }

    // Set the response output to the Metacard from the transformation
    exchange.getOut().setBody(metacard);

    LOGGER.debug("EXITING: process");
  }

  protected abstract Object transform(
      Message in, Object metacard, String mimeType, String transformerId)
      throws MimeTypeParseException, IOException, CatalogTransformerException;
}
