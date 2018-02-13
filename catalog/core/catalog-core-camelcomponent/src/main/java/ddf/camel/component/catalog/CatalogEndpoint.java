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
package ddf.camel.component.catalog;

import ddf.camel.component.catalog.framework.FrameworkProducer;
import ddf.camel.component.catalog.ingest.PostIngestConsumer;
import ddf.camel.component.catalog.inputtransformer.InputTransformerConsumer;
import ddf.camel.component.catalog.inputtransformer.InputTransformerProducer;
import ddf.camel.component.catalog.metacardtransformer.MetacardTransformerProducer;
import ddf.camel.component.catalog.queryresponsetransformer.QueryResponseTransformerConsumer;
import ddf.camel.component.catalog.queryresponsetransformer.QueryResponseTransformerProducer;
import ddf.catalog.CatalogFramework;
import org.apache.camel.Consumer;
import org.apache.camel.ExchangePattern;
import org.apache.camel.MultipleConsumersSupport;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.codice.ddf.catalog.transform.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Camel endpoint for the custom <code>catalog</code> Camel route node.
 *
 * @author Hugh Rodgers, Lockheed Martin
 * @author William Miller, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public class CatalogEndpoint extends DefaultEndpoint implements MultipleConsumersSupport {
  private static final transient Logger LOGGER = LoggerFactory.getLogger(CatalogEndpoint.class);

  private static final String INPUT_TRANSFORMER = "inputtransformer";

  private static final String QUERYRESPONSE_TRANSFORMER = "queryresponsetransformer";

  private static final String METACARD_TRANSFORMER = "metacardtransformer";

  private static final String POST_INGEST_CONSUMER = "postingest";

  private static final String FRAMEWORK = "framework";

  private String transformerId;

  private String contextPath;

  private String mimeType;

  private Transform transform;

  private CatalogFramework catalogFramework;

  /**
   * Constructs a CatalogEndpoint for the specified custom <code>catalog</code> component.
   *
   * @param uri the endpoint's URI
   * @param component the route node catalog component
   * @param transformerId the {@link ddf.catalog.transform.InputTransformer} ID, id=xml
   * @param mimeType the registered service mime-type
   * @param contextPath the context path of the catalog route node, which is the portion of the URI
   *     after the <code>catalog</code> scheme, e.g., <code>inputtransformer</code>, which indicates
   *     how to interpret the <code>catalog</code> route node
   * @param catalogFramework the catalog framework
   */
  public CatalogEndpoint(
      String uri,
      CatalogComponent component,
      String transformerId,
      String mimeType,
      String contextPath,
      CatalogFramework catalogFramework,
      Transform transform) {
    super(uri, component);
    LOGGER.trace(
        "INSIDE CamelCatalogEndpoint(uri, component, transformerId, contextPath, catalogFramework, transform) constructor");
    this.transformerId = transformerId;
    this.mimeType = mimeType;
    this.contextPath = contextPath;
    this.catalogFramework = catalogFramework;
    this.transform = transform;
    setSynchronous(true);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.camel.impl.DefaultEndpoint#getComponent()
   */
  @Override
  public CatalogComponent getComponent() {
    return (CatalogComponent) super.getComponent();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.camel.impl.DefaultEndpoint#getExchangePattern()
   */
  @Override
  public ExchangePattern getExchangePattern() {
    return ExchangePattern.InOut;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.camel.Endpoint#createProducer()
   */
  public Producer createProducer() {
    LOGGER.debug("INSIDE createProducer");

    // Camel Producers map to <to> route nodes.
    Producer producer = null;

    // Create the Producer corresponding to the CatalogComponent's context
    // path.
    // The context path is the portion of the route node's URI after the
    // "catalog" scheme,
    // e.g., <from uri="catalog:inputtransformer?mimeType=text/xml&amp;=id=xml" />
    if (contextPath.equals(INPUT_TRANSFORMER)) {
      producer = new InputTransformerProducer(this, transform);
    } else if (contextPath.equals(QUERYRESPONSE_TRANSFORMER)) {
      producer = new QueryResponseTransformerProducer(this, transform);
    } else if (contextPath.equals(FRAMEWORK)) {
      producer = new FrameworkProducer(this, catalogFramework);
    } else if (contextPath.equals(METACARD_TRANSFORMER)) {
      producer = new MetacardTransformerProducer(this, transform);
    } else {
      LOGGER.debug("Unable to create producer for context path [{}]", contextPath);
      throw new IllegalArgumentException(
          "Unable to create producer for context path [" + contextPath + "]");
    }

    return producer;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.camel.Endpoint#createConsumer(org.apache.camel.Processor)
   */
  public Consumer createConsumer(Processor processor) {
    LOGGER.debug("INSIDE createConsumer");

    // Camel Consumers map to <from> route nodes.
    Consumer consumer = null;

    // Create the Consumer corresponding to the CatalogComponent's context
    // path.
    // The context path is the portion of the route node's URI after the
    // "catalog" scheme,
    // e.g., <from uri="catalog:inputtransformer?mimeType=text/xml&amp;id=xml" />
    if (contextPath.equals(INPUT_TRANSFORMER)) {
      consumer = new InputTransformerConsumer(this, processor);
    } else if (contextPath.equals(QUERYRESPONSE_TRANSFORMER)) {
      consumer = new QueryResponseTransformerConsumer(this, processor);
    } else if (contextPath.equals(POST_INGEST_CONSUMER)) {
      consumer = new PostIngestConsumer(this, processor);
    } else {
      LOGGER.debug("Unable to create consumer for context path [{}]", contextPath);
      throw new IllegalArgumentException(
          "Unable to create consumer for context path [" + contextPath + "]");
    }

    return consumer;
  }

  /** @return ID of the transformer specified as a parameter in the Camel route node */
  public String getTransformerId() {
    return transformerId;
  }

  public String getMimeType() {
    return mimeType;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.camel.IsSingleton#isSingleton()
   */
  public boolean isSingleton() {
    return true;
  }

  @Override
  public boolean isMultipleConsumersSupported() {
    return true;
  }
}
