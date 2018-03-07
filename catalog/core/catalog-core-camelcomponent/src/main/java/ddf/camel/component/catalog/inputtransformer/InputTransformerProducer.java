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
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeResolutionException;
import ddf.mime.MimeTypeToTransformerMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.camel.Message;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(InputTransformerProducer.class);

  private static final String METACARD_ID_HEADER =
      "org.codice.ddf.camel.transformer.MetacardUpdateId";

  private static final String FILE_EXTENSION_HEADER = "org.codice.ddf.camel.FileExtension";

  /**
   * Constructs the {@link org.apache.camel.Producer} for the custom Camel CatalogComponent. This
   * producer would map to a Camel <to> route node with a URI like <code>catalog:inputtransformer
   * </code>
   *
   * @param endpoint the Camel endpoint that created this consumer
   */
  public InputTransformerProducer(CatalogEndpoint endpoint) {
    super(endpoint);
  }

  protected Object transform(
      Message in, String mimeType, String transformerId, MimeTypeToTransformerMapper mapper)
      throws MimeTypeParseException, CatalogTransformerException {
    InputStream message = in.getBody(InputStream.class);
    if (message == null) {
      throw new CatalogTransformerException("Message body was null; unable to generate Metacard!");
    }

    MimeType derivedMimeType = null;
    if (StringUtils.isEmpty(mimeType)) {
      try (TemporaryFileBackedOutputStream fileBackedOutputStream =
          new TemporaryFileBackedOutputStream()) {
        try {
          IOUtils.copy(message, fileBackedOutputStream);
          derivedMimeType =
              new MimeType(
                  getMimeTypeFromHeader(in, fileBackedOutputStream.asByteSource().openStream()));
        } catch (IOException e) {
          LOGGER.debug("Failed to copy incoming inputStream message", e);
        } finally {
          message = fileBackedOutputStream.asByteSource().openStream();
        }
      } catch (IOException e) {
        LOGGER.debug("Failed to create TemporaryFileBackedOuputStream", e);
      }
    } else if (StringUtils.isNotBlank(transformerId)) {
      derivedMimeType =
          new MimeType(mimeType + ";" + MimeTypeToTransformerMapper.ID_KEY + "=" + transformerId);
    }

    if (derivedMimeType == null) {
      derivedMimeType = new MimeType(mimeType);
    }

    String metacardUpdateID = getHeaderAsStringAndRemove(in, METACARD_ID_HEADER);

    Metacard metacard;
    try {
      metacard = generateMetacard(derivedMimeType, mapper, message, metacardUpdateID);
    } catch (MetacardCreationException e) {
      throw new CatalogTransformerException(
          "Did not find an InputTransformer for MIME Type ["
              + mimeType
              + "] and "
              + MimeTypeToTransformerMapper.ID_KEY
              + " ["
              + transformerId
              + "]",
          e);
    } finally {
      IOUtils.closeQuietly(message);
    }

    return metacard;
  }

  private Metacard generateMetacard(
      MimeType mimeType, MimeTypeToTransformerMapper mapper, InputStream message, String metacardId)
      throws MetacardCreationException {
    LOGGER.trace("ENTERING: generateMetacard");

    List<InputTransformer> listOfCandidates = mapper.findMatches(InputTransformer.class, mimeType);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("List of matches for mimeType [{}]: {}", mimeType, listOfCandidates);
    }

    Metacard generatedMetacard = null;

    try (TemporaryFileBackedOutputStream fileBackedOutputStream =
        new TemporaryFileBackedOutputStream()) {

      try {
        IOUtils.copy(message, fileBackedOutputStream);
      } catch (IOException e) {
        throw new MetacardCreationException("Could not copy bytes of content message.", e);
      }

      // Multiple InputTransformers may be found that match the mime type.
      // Need to try each InputTransformer until we find one that can successfully transform
      // the input stream's data into a metacard. Once an InputTransformer is found that
      // can create the metacard, then do not need to try any remaining InputTransformers.
      for (InputTransformer transformer : listOfCandidates) {

        try (InputStream inputStreamMessageCopy =
            fileBackedOutputStream.asByteSource().openStream()) {
          if (StringUtils.isEmpty(metacardId)) {
            generatedMetacard = transformer.transform(inputStreamMessageCopy);
          } else {
            generatedMetacard = transformer.transform(inputStreamMessageCopy, metacardId);
          }
        } catch (IOException | CatalogTransformerException e) {
          LOGGER.debug("Transformer [{}] could not create metacard.", transformer, e);
        }
        if (generatedMetacard != null) {
          break;
        }
      }

      if (generatedMetacard == null) {
        throw new MetacardCreationException(
            "Could not create metacard with mimeType "
                + mimeType
                + ". No valid transformers found.");
      }

      LOGGER.trace("EXITING: generateMetacard");
    } catch (IOException e) {
      throw new MetacardCreationException("Could not create metacard.", e);
    }

    return generatedMetacard;
  }

  private String getMimeTypeFromHeader(Message in, InputStream is) {
    String fileExtension = getHeaderAsStringAndRemove(in, FILE_EXTENSION_HEADER);
    if (fileExtension == null) {
      return null;
    }

    try {
      return ((CatalogEndpoint) getEndpoint()).getMimeTypeMapper().guessMimeType(is, fileExtension);
    } catch (MimeTypeResolutionException e) {
      LOGGER.debug(
          "Failed to get mimeType for file extension [{}] received from exchange headers.");
      return null;
    }
  }

  private String getHeaderAsStringAndRemove(Message message, String key) {
    String value = message.getHeader(key, String.class);
    if (value != null) {
      LOGGER.trace(
          "Retrieved and removed header [{}] from exchange message [{}]", message.getMessageId());
      message.removeHeader(key);
      return value;
    }
    return null;
  }
}
