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
package ddf.camel.component.catalog.metacardtransformer;

import com.google.common.annotations.VisibleForTesting;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.MetacardTransformer;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Camel Producer that will transform a metacard from the body, using the transformer id provided in
 * the route header and set the outgoing body as the transformed data.
 */
public class MetacardTransformerProducer extends DefaultProducer {
  public static final String METACARD_HEADER = "metacard";

  public static final String TRANSFORMER_ID = "transformerId";

  private static final Logger LOGGER = LoggerFactory.getLogger(MetacardTransformerProducer.class);

  public MetacardTransformerProducer(Endpoint endpoint) {
    super(endpoint);
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    Message in = exchange.getIn();
    BinaryContent transformedData = null;
    Object incomingData = in.getBody();
    String transformerId = in.getHeader(TRANSFORMER_ID, String.class);

    if (incomingData instanceof Metacard) {
      Metacard metacard = (Metacard) incomingData;
      exchange.getOut().setHeader(METACARD_HEADER, metacard);
      if (transformerId != null) {
        in.removeHeader(TRANSFORMER_ID);
        MetacardTransformer transformer = lookupTransformerReference(transformerId);
        if (transformer != null) {
          transformedData = transformer.transform(metacard, Collections.emptyMap());
        }
      } else {
        LOGGER.debug("metacard transformer id not present in: {} parameter", TRANSFORMER_ID);
      }
    }

    // Set the response output to the Metacard from the transformation
    exchange.getOut().setHeaders(in.getHeaders());

    if (transformedData != null) {
      exchange.getOut().setBody(transformedData.getByteArray());
    } else {
      exchange.getOut().setBody(null);
    }
  }

  @VisibleForTesting
  MetacardTransformer lookupTransformerReference(String metacardTransformerId) {
    Bundle bundle = FrameworkUtil.getBundle(this.getClass());
    if (bundle != null) {
      BundleContext bundleContext = bundle.getBundleContext();
      try {
        Collection<ServiceReference<MetacardTransformer>> transformerReference =
            bundleContext.getServiceReferences(
                MetacardTransformer.class, "(id=" + metacardTransformerId + ")");
        return bundleContext.getService(transformerReference.iterator().next());
      } catch (InvalidSyntaxException | NoSuchElementException e) {
        LOGGER.warn(
            "Unable to resolve MetacardTransformer {}.  Backup will not be performed.",
            metacardTransformerId,
            e);
      }
    }
    return null;
  }
}
