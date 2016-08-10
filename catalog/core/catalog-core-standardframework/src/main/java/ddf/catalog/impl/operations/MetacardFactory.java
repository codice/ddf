/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.impl.operations;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeToTransformerMapper;
import ddf.security.Subject;
import ddf.security.SubjectUtils;

/**
 * Support class for creating metacards for the {@code CatalogFrameworkImpl}.
 * <p>
 * This factory class contains methods specific to metacard creation for the CFI and its
 * support classes. No operations/support methods should be added to this class except in support
 * of CFI metacard creation.
 */
public class MetacardFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardFactory.class);

    //
    // Injected properties
    //
    private final MimeTypeToTransformerMapper mimeTypeToTransformerMapper;

    public MetacardFactory(MimeTypeToTransformerMapper mimeTypeToTransformerMapper) {
        this.mimeTypeToTransformerMapper = mimeTypeToTransformerMapper;
    }

    Metacard generateMetacard(String mimeTypeRaw, String id, String fileName, Subject subject,
            Path tmpContentPath) throws MetacardCreationException, MimeTypeParseException {

        Metacard generatedMetacard = null;
        InputTransformer transformer = null;
        StringBuilder causeMessage = new StringBuilder("Could not create metacard with mimeType ");
        try {
            MimeType mimeType = new MimeType(mimeTypeRaw);

            List<InputTransformer> listOfCandidates = mimeTypeToTransformerMapper.findMatches(
                    InputTransformer.class,
                    mimeType);

            LOGGER.debug("List of matches for mimeType [{}]: {}", mimeType, listOfCandidates);

            for (InputTransformer candidate : listOfCandidates) {
                transformer = candidate;

                try (InputStream transformerStream = com.google.common.io.Files.asByteSource(
                        tmpContentPath.toFile())
                        .openStream()) {
                    generatedMetacard = transformer.transform(transformerStream);
                }
                if (generatedMetacard != null) {
                    break;
                }
            }
        } catch (CatalogTransformerException | IOException e) {
            causeMessage.append(mimeTypeRaw)
                    .append(". Reason: ")
                    .append(System.lineSeparator())
                    .append(e.getMessage());

            // The caught exception more than likely does not have the root cause message
            // that is needed to inform the caller as to why things have failed.  Therefore
            // we need to iterate through the chain of cause exceptions and gather up
            // all of their message details.
            Throwable cause = e.getCause();
            while (cause != null && cause != cause.getCause()) {
                causeMessage.append(System.lineSeparator())
                        .append(cause.getMessage());
                cause = cause.getCause();
            }
            LOGGER.debug("Transformer [{}] could not create metacard.", transformer, e);
        }

        if (generatedMetacard == null) {
            throw new MetacardCreationException(causeMessage.toString());
        }

        if (id != null) {
            generatedMetacard.setAttribute(new AttributeImpl(Metacard.ID, id));
        } else {
            generatedMetacard.setAttribute(new AttributeImpl(Metacard.ID,
                    UUID.randomUUID()
                            .toString()
                            .replaceAll("-", "")));
        }

        if (StringUtils.isBlank(generatedMetacard.getTitle())) {
            generatedMetacard.setAttribute(new AttributeImpl(Metacard.TITLE, fileName));
        }

        String name = Optional.ofNullable(SubjectUtils.getName(subject))
                .orElse("");

        generatedMetacard.setAttribute(new AttributeImpl(Metacard.POINT_OF_CONTACT, name));

        return generatedMetacard;
    }
}
