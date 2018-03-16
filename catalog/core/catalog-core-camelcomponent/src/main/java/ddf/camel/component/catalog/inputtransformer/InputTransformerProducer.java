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
package ddf.camel.component.catalog.inputtransformer;

import ddf.camel.component.catalog.CatalogEndpoint;
import ddf.camel.component.catalog.transformer.TransformerProducer;
import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.camel.Message;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.transform.Transform;
import org.codice.ddf.catalog.transform.TransformResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * org.apache.camel.Producer for the custom Camel CatalogComponent. This {@link
 * org.apache.camel.Producer} would map to a Camel <to> route node with a URI like <code>
 * catalog:inputtransformer</code>
 *
 * @author Hugh Rodgers, Lockheed Martin
 * @author William Miller, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public class InputTransformerProducer extends TransformerProducer {
  private static final transient Logger LOGGER =
      LoggerFactory.getLogger(InputTransformerProducer.class);

  private final Transform transform;

  /**
   * Constructs the {@link org.apache.camel.Producer} for the custom Camel CatalogComponent. This
   * producer would map to a Camel <to> route node with a URI like <code>catalog:inputtransformer
   * </code>
   *
   * @param endpoint the Camel endpoint that created this consumer
   */
  public InputTransformerProducer(CatalogEndpoint endpoint, Transform transform) {
    super(endpoint);
    this.transform = transform;
  }

  protected Object transform(Message in, Object obj, String mimeType, String transformerId)
      throws MimeTypeParseException, IOException, CatalogTransformerException {
    // Look up the InputTransformer for the request's mime type.
    // If a transformer is found, then transform the request's payload into
    // a Metacard.
    // Otherwise, throw an exception.

    MimeType derivedMimeType = new MimeType(mimeType);

    if (transformerId != null) {
      derivedMimeType = new MimeType(mimeType + ";" + Constants.SERVICE_ID + "=" + transformerId);
    }

    InputStream message = null;
    Metacard metacard = null;
    try {
      message = in.getBody(InputStream.class);
      if (null != message) {
        metacard = generateMetacard(derivedMimeType, message);
      } else {
        throw new CatalogTransformerException(
            "Message body was null; unable to generate Metacard!");
      }
    } catch (MetacardCreationException e) {
      throw new CatalogTransformerException(
          "Did not find an InputTransformer for MIME Type ["
              + mimeType
              + "] and "
              + Constants.SERVICE_ID
              + " ["
              + transformerId
              + "]",
          e);
    } finally {
      if (null != message) {
        IOUtils.closeQuietly(message);
      }
    }

    return metacard;
  }

  private Metacard generateMetacard(MimeType mimeType, InputStream message)
      throws MetacardCreationException {
    LOGGER.trace("ENTERING: generateMetacard");

    TransformResponse transformResponse =
        transform.transform(mimeType, null, null, message, null, Collections.emptyMap());

    Optional<Metacard> optionalMetacard = transformResponse.getParentMetacard();

    if (!optionalMetacard.isPresent()) {
      throw new MetacardCreationException("Could not create metacard with mimeType " + mimeType);
    }

    return optionalMetacard.get();
  }
}
