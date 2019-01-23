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
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeResolutionException;
import ddf.mime.MimeTypeToTransformerMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
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
 * @author Hugh Rodgers
 * @author William Miller
 */
public class InputTransformerProducer extends TransformerProducer {
  private static final Logger LOGGER = LoggerFactory.getLogger(InputTransformerProducer.class);

  private static final String METACARD_ID_HEADER =
      "org.codice.ddf.camel.transformer.MetacardUpdateId";

  private static final String FILE_EXTENSION_HEADER = "org.codice.ddf.camel.FileExtension";

  private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

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
    MimeType derivedMimeType = null;
    try (InputStream message = in.getBody(InputStream.class);
        TemporaryFileBackedOutputStream tfbos = new TemporaryFileBackedOutputStream()) {
      if (message == null) {
        throw new CatalogTransformerException(
            "Message body was null; unable to generate Metacard!");
      }

      // First try to get mimeType from file extension passed in the Camel Message headers
      IOUtils.copy(message, tfbos);
      String fileExtensionHeader = getHeaderAsStringAndRemove(in, FILE_EXTENSION_HEADER);
      if (StringUtils.isNotEmpty(fileExtensionHeader)) {
        Optional<String> fileMimeType =
            getMimeTypeFor(tfbos.asByteSource().openBufferedStream(), fileExtensionHeader);
        if (fileMimeType.isPresent()) {
          LOGGER.trace(
              "Setting mimetype to [{}] from Message header [{}]",
              fileMimeType.get(),
              FILE_EXTENSION_HEADER);
          derivedMimeType = new MimeType(fileMimeType.get());
        }
      }

      if (derivedMimeType == null) {
        if (StringUtils.isNotEmpty(mimeType)) {
          // We didn't get mimeType from file extension header, try from CatalogEndpoint configured
          // mimeType and tranformerId
          if (StringUtils.isNotEmpty(transformerId)) {
            derivedMimeType =
                new MimeType(
                    mimeType + ";" + MimeTypeToTransformerMapper.ID_KEY + "=" + transformerId);
            LOGGER.trace("Using mimeType to [{}]", derivedMimeType);
          } else {
            LOGGER.trace("Using CatalogEndpoint's configured mimeType [{}]", mimeType);
            derivedMimeType = new MimeType(mimeType);
          }
        } else {
          LOGGER.debug("Unable to determine mimeType. Defaulting to [{}]", DEFAULT_MIME_TYPE);
          derivedMimeType = new MimeType(DEFAULT_MIME_TYPE);
        }
      }

      String metacardUpdateID = getHeaderAsStringAndRemove(in, METACARD_ID_HEADER);
      return generateMetacard(derivedMimeType, mapper, tfbos, metacardUpdateID)
          .orElseThrow(
              () ->
                  new CatalogTransformerException(
                      String.format(
                          "Did not find an InputTransformer for MIME Type [%s] and %s [%s]",
                          mimeType, MimeTypeToTransformerMapper.ID_KEY, transformerId)));
    } catch (IOException e) {
      throw new CatalogTransformerException("Unable to transform incoming product", e);
    }
  }

  private Optional<Metacard> generateMetacard(
      MimeType mimeType,
      MimeTypeToTransformerMapper mapper,
      TemporaryFileBackedOutputStream tfbos,
      String metacardId) {
    LOGGER.trace("ENTERING: generateMetacard");

    List<InputTransformer> listOfCandidates = mapper.findMatches(InputTransformer.class, mimeType);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("List of matches for mimeType [{}]: {}", mimeType, listOfCandidates);
    }

    Metacard generatedMetacard = null;

    // Multiple InputTransformers may be found that match the mime type.
    // Need to try each InputTransformer until we find one that can successfully transform
    // the input stream's data into a metacard. Once an InputTransformer is found that
    // can create the metacard, then do not need to try any remaining InputTransformers.
    for (InputTransformer transformer : listOfCandidates) {

      try (InputStream inputStreamMessageCopy = tfbos.asByteSource().openStream()) {
        if (StringUtils.isEmpty(metacardId)) {
          generatedMetacard = transformer.transform(inputStreamMessageCopy);
        } else {
          generatedMetacard = transformer.transform(inputStreamMessageCopy, metacardId);
        }
      } catch (CatalogTransformerException e) {
        LOGGER.debug("Transformer [{}] could not create metacard.", transformer, e);
      } catch (IOException e) {
        LOGGER.debug("Could not open input stream", e);
      }
      if (generatedMetacard != null) {
        break;
      }
    }

    LOGGER.trace("EXITING: generateMetacard");
    return Optional.ofNullable(generatedMetacard);
  }

  private Optional<String> getMimeTypeFor(InputStream is, String fileExtension) {
    try {
      return Optional.ofNullable(
          ((CatalogEndpoint) getEndpoint()).getMimeTypeMapper().guessMimeType(is, fileExtension));
    } catch (MimeTypeResolutionException e) {
      LOGGER.debug("Failed to get mimeType for file extension [{}].", fileExtension);
      return Optional.empty();
    }
  }

  private String getHeaderAsStringAndRemove(Message message, String key) {
    String value = message.getHeader(key, String.class);
    if (value != null) {
      LOGGER.trace(
          "Retrieved and removed header [{}] from exchange message [{}]",
          key,
          message.getMessageId());
      message.removeHeader(key);
      return value;
    }
    return null;
  }
}
