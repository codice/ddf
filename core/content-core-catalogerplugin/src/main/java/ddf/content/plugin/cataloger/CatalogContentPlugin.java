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
package ddf.content.plugin.cataloger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.activation.MimeType;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.content.data.ContentItem;
import ddf.content.operation.CreateResponse;
import ddf.content.operation.DeleteResponse;
import ddf.content.operation.UpdateResponse;
import ddf.content.operation.impl.CreateResponseImpl;
import ddf.content.operation.impl.DeleteResponseImpl;
import ddf.content.operation.impl.UpdateResponseImpl;
import ddf.content.plugin.ContentPlugin;
import ddf.content.plugin.PluginExecutionException;
import ddf.mime.MimeTypeToTransformerMapper;

public class CatalogContentPlugin implements ContentPlugin {
    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(CatalogContentPlugin.class));

    private static final String CATALOG_ID = "Catalog-ID";

    private Cataloger cataloger;

    private MimeTypeToTransformerMapper mimeTypeToTransformerMapper;

    public CatalogContentPlugin(CatalogFramework catalogFramework,
            MimeTypeToTransformerMapper mimeTypeToTransformerMapper) {
        LOGGER.trace("INSIDE: CatalogContentPlugin constructor");

        this.cataloger = new Cataloger(catalogFramework);
        this.mimeTypeToTransformerMapper = mimeTypeToTransformerMapper;
    }

    @Override
    public CreateResponse process(CreateResponse input) throws PluginExecutionException {
        LOGGER.trace("ENTERING: process(CreateResponse)");

        ContentItem createdContentItem = input.getCreatedContentItem();
        CreateResponseImpl response = new CreateResponseImpl(input);
        MimeType mimeType = createdContentItem.getMimeType();
        InputStream stream = null;
        try {
            stream = createdContentItem.getInputStream();
        } catch (IOException e) {
            throw new PluginExecutionException(
                    "Unable to read InputStream in created content item.", e);
        }

        if (stream == null) {
            throw new PluginExecutionException("InputStream is null in created content item.");
        }

        try {
            Metacard metacard = generateMetacard(mimeType, createdContentItem.getUri(), stream);
            String catalogId = cataloger.createMetacard(metacard);
            LOGGER.debug("catalogId = " + catalogId);
            Map<String, String> properties = response.getResponseProperties();
            properties.put(CATALOG_ID, catalogId);
            response.setResponseProperties(properties);
        } catch (MetacardCreationException e) {
            LOGGER.warn(e.getMessage(), e);
            throw new PluginExecutionException(e.getMessage(), e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }

        LOGGER.trace("EXITING: process(CreateResponse)");

        return response;
    }

    @Override
    public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
        LOGGER.trace("ENTERING: process(UpdateResponse)");

        ContentItem updatedContentItem = input.getUpdatedContentItem();
        UpdateResponseImpl response = new UpdateResponseImpl(input);
        MimeType mimeType = updatedContentItem.getMimeType();
        InputStream stream = null;
        try {
            stream = updatedContentItem.getInputStream();
        } catch (IOException e) {
            throw new PluginExecutionException(
                    "Unable to read InputStream in updated content item.", e);
        }

        if (stream == null) {
            throw new PluginExecutionException("InputStream is null in updated content item.");
        }

        try {
            Metacard metacard = generateMetacard(mimeType, updatedContentItem.getUri(), stream);
            String catalogId = cataloger.updateMetacard(updatedContentItem.getUri(), metacard);
            LOGGER.debug("catalogId = " + catalogId);
            Map<String, String> properties = response.getResponseProperties();
            properties.put(CATALOG_ID, catalogId);
            response.setResponseProperties(properties);
        } catch (MetacardCreationException e) {
            LOGGER.warn(e.getMessage(), e);
            throw new PluginExecutionException(e.getMessage(), e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }

        LOGGER.trace("EXITING: process(UpdateResponse)");

        return response;
    }

    @Override
    public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
        LOGGER.trace("ENTERING: process(DeleteResponse)");

        DeleteResponseImpl response = new DeleteResponseImpl(input);

        String catalogId = cataloger.deleteMetacard(input.getContentItem().getUri());
        if (catalogId != null && !catalogId.isEmpty()) {
            // Create response indicating file (actually, catalog entry) was deleted
            response = new DeleteResponseImpl(input.getRequest(), input.getContentItem(), true,
                    input.getResponseProperties(), input.getProperties());
        }

        LOGGER.debug("catalogId = " + catalogId);
        Map<String, String> properties = response.getResponseProperties();
        properties.put(CATALOG_ID, catalogId);
        response.setResponseProperties(properties);

        LOGGER.trace("EXITING: process(DeleteResponse)");

        return response;
    }

    private Metacard generateMetacard(MimeType mimeType, String uri, InputStream message)
        throws MetacardCreationException {
        LOGGER.trace("ENTERING: generateMetacard");

        List<InputTransformer> listOfCandidates = mimeTypeToTransformerMapper.findMatches(
                InputTransformer.class, mimeType);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("List of matches for mimeType [" + mimeType + "]:" + listOfCandidates);
        }

        Metacard generatedMetacard = null;

        byte[] messageBytes;
        try {
            messageBytes = IOUtils.toByteArray(message);
        } catch (IOException e) {
            throw new MetacardCreationException("Could not copy bytes of content message.", e);
        }

        // Multiple InputTransformers may be found that match the mime type.
        // Need to try each InputTransformer until we find one that can successfully transform
        // the input stream's data into a metacard. Once an InputTransformer is found that
        // can create the metacard, then do not need to try any remaining InputTransformers.
        for (InputTransformer transformer : listOfCandidates) {

            InputStream inputStreamMessageCopy = new ByteArrayInputStream(messageBytes);

            try {
                generatedMetacard = transformer.transform(inputStreamMessageCopy);
            } catch (CatalogTransformerException e) {
                LOGGER.debug("Transformer [" + transformer + "] could not create metacard.", e);
            } catch (IOException e) {
                LOGGER.debug("Transformer [" + transformer + "] could not create metacard. ", e);
            }
            if (generatedMetacard != null) {
            	//Setting the non-transformer specific information not including creation and modification dates/times
            	generatedMetacard.setAttribute(new AttributeImpl(Metacard.RESOURCE_SIZE, String.valueOf(messageBytes.length)));
            	if (uri != null) {
                    generatedMetacard.setAttribute(new AttributeImpl(Metacard.RESOURCE_URI, uri));
                } else {
                    LOGGER.debug("Metacard had a null uri");
            	break;
                }
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
