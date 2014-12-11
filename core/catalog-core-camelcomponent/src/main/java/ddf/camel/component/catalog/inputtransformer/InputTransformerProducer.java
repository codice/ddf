/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.camel.component.catalog.inputtransformer;

import com.google.common.io.FileBackedOutputStream;
import ddf.camel.component.catalog.CatalogEndpoint;
import ddf.camel.component.catalog.transformer.TransformerProducer;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeToTransformerMapper;
import org.apache.camel.Message;
import org.apache.camel.Producer;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Producer for the custom Camel CatalogComponent. This {@link org.apache.camel.Producer} would map
 * to a Camel <to> route node with a URI like <code>catalog:inputtransformer</code>
 * 
 * @author Hugh Rodgers, Lockheed Martin
 * @author William Miller, Lockheed Martin
 * @author ddf.isgs@lmco.com
 * 
 */
public class InputTransformerProducer extends TransformerProducer {
    private static final transient Logger LOGGER = LoggerFactory
            .getLogger(InputTransformerProducer.class);

    /**
     * Constructs the {@link Producer} for the custom Camel CatalogComponent. This producer would
     * map to a Camel <to> route node with a URI like <code>catalog:inputtransformer</code>
     * 
     * @param endpoint
     *            the Camel endpoint that created this consumer
     */
    public InputTransformerProducer(CatalogEndpoint endpoint) {
        super(endpoint);
    }

    protected Object transform(Message in, Object obj, String mimeType, String transformerId,
            MimeTypeToTransformerMapper mapper) throws MimeTypeParseException, IOException,
        CatalogTransformerException {
        // Look up the InputTransformer for the request's mime type.
        // If a transformer is found, then transform the request's payload into
        // a Metacard.
        // Otherwise, throw an exception.

        MimeType derivedMimeType = new MimeType(mimeType);

        if (transformerId != null) {
            derivedMimeType = new MimeType(mimeType + ";" + MimeTypeToTransformerMapper.ID_KEY
                    + "=" + transformerId);
        }

        Metacard metacard = null;
        try {
            metacard = generateMetacard(derivedMimeType, mapper, in.getBody(InputStream.class));
        } catch (MetacardCreationException e) {
            throw new CatalogTransformerException(
                    "Did not find an InputTransformer for MIME Type [" + mimeType + "] and "
                            + MimeTypeToTransformerMapper.ID_KEY + " [" + transformerId + "]", e);
        }

        return metacard;
    }

    private Metacard generateMetacard(MimeType mimeType, MimeTypeToTransformerMapper mapper,
            InputStream message) throws MetacardCreationException {
        LOGGER.trace("ENTERING: generateMetacard");

        List<InputTransformer> listOfCandidates = mapper.findMatches(InputTransformer.class,
                mimeType);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("List of matches for mimeType [" + mimeType + "]:" + listOfCandidates);
        }

        Metacard generatedMetacard = null;

        FileBackedOutputStream fileBackedOutputStream = new FileBackedOutputStream(1000000);

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

            try (InputStream inputStreamMessageCopy = fileBackedOutputStream.asByteSource().openStream()) {
                generatedMetacard = transformer.transform(inputStreamMessageCopy);
            } catch (IOException | CatalogTransformerException e) {
                LOGGER.debug("Transformer [" + transformer + "] could not create metacard.", e);
            }
            if (generatedMetacard != null) {
                break;
            }
        }

        if (generatedMetacard == null) {
            throw new MetacardCreationException("Could not create metacard with mimeType "
                    + mimeType + ". No valid transformers found.");
        }

        LOGGER.trace("EXITING: generateMetacard");

        return generatedMetacard;
    }

}
