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
package ddf.catalog.impl.operations;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeToTransformerMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class for creating metacards for the {@code CatalogFrameworkImpl}.
 *
 * <p>This factory class contains methods specific to metacard creation for the CFI and its support
 * classes. No operations/support methods should be added to this class except in support of CFI
 * metacard creation.
 */
public class MetacardFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(MetacardFactory.class);

  //
  // Injected properties
  //
  private final MimeTypeToTransformerMapper mimeTypeToTransformerMapper;

  private UuidGenerator uuidGenerator;

  public MetacardFactory(
      MimeTypeToTransformerMapper mimeTypeToTransformerMapper, UuidGenerator uuidGenerator) {
    this.mimeTypeToTransformerMapper = mimeTypeToTransformerMapper;
    this.uuidGenerator = uuidGenerator;
  }

  Metacard generateMetacard(String mimeTypeRaw, String id, String fileName, Path tmpContentPath)
      throws MetacardCreationException, MimeTypeParseException {

    Metacard generatedMetacard = null;

    MimeType mimeType = new MimeType(mimeTypeRaw);

    List<InputTransformer> listOfCandidates =
        mimeTypeToTransformerMapper.findMatches(InputTransformer.class, mimeType);
    List<String> stackTraceList = new ArrayList<>();

    LOGGER.debug("List of matches for mimeType [{}]: {}", mimeType, listOfCandidates);

    for (InputTransformer candidate : listOfCandidates) {
      try (InputStream transformerStream =
          com.google.common.io.Files.asByteSource(tmpContentPath.toFile()).openStream()) {
        generatedMetacard = candidate.transform(transformerStream);
      } catch (CatalogTransformerException | IOException e) {
        List<String> stackTraces = Arrays.asList(ExceptionUtils.getRootCauseStackTrace(e));
        stackTraceList.add(String.format("Transformer [%s] could not create metacard.", candidate));
        stackTraceList.addAll(stackTraces);
        LOGGER.debug("Transformer [{}] could not create metacard.", candidate, e);
      }
      if (generatedMetacard != null) {
        break;
      }
    }

    if (generatedMetacard == null) {
      throw new MetacardCreationException(
          String.format(
              "Could not create metacard with mimeType %s : %s",
              mimeTypeRaw, StringUtils.join(stackTraceList, "\n")));
    }

    if (id != null) {
      generatedMetacard.setAttribute(new AttributeImpl(Core.ID, id));
    } else {
      generatedMetacard.setAttribute(new AttributeImpl(Core.ID, uuidGenerator.generateUuid()));
    }

    if (StringUtils.isBlank(generatedMetacard.getTitle())) {
      generatedMetacard.setAttribute(new AttributeImpl(Core.TITLE, fileName));
    }

    return generatedMetacard;
  }
}
