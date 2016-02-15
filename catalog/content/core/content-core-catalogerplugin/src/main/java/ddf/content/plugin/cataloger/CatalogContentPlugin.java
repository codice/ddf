/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.content.plugin.cataloger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.activation.MimeType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.subject.Subject;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import com.google.common.io.FileBackedOutputStream;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.content.data.ContentItem;
import ddf.content.data.impl.ContentMetacardType;
import ddf.content.operation.CreateResponse;
import ddf.content.operation.DeleteResponse;
import ddf.content.operation.UpdateResponse;
import ddf.content.operation.impl.CreateResponseImpl;
import ddf.content.operation.impl.DeleteResponseImpl;
import ddf.content.operation.impl.UpdateResponseImpl;
import ddf.content.plugin.ContentPlugin;
import ddf.content.plugin.PluginExecutionException;
import ddf.mime.MimeTypeToTransformerMapper;
import ddf.security.SubjectUtils;

public class CatalogContentPlugin implements ContentPlugin {
    private static final XLogger LOGGER =
            new XLogger(LoggerFactory.getLogger(CatalogContentPlugin.class));

    private static final String CATALOG_ID = "Catalog-ID";

    private static final String DEFAULT_METACARD_TRANSFORMER = "geojson";

    private static final String CHECKSUM_PROPERTY_NAME = "Resource Checksum";

    private static final String CHECKSUM_ALGORITHM_PROPERTY_NAME = "Resource Checksum Algorithm";

    private final CatalogFramework catalogFramework;

    private Cataloger cataloger;

    private MimeTypeToTransformerMapper mimeTypeToTransformerMapper;

    public CatalogContentPlugin(CatalogFramework catalogFramework,
            MimeTypeToTransformerMapper mimeTypeToTransformerMapper) {
        LOGGER.trace("INSIDE: CatalogContentPlugin constructor");

        this.catalogFramework = catalogFramework;
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
            throw new PluginExecutionException("Unable to read InputStream in created content item.",
                    e);
        }

        if (stream == null) {
            throw new PluginExecutionException("InputStream is null in created content item.");
        }

        try {
            Metacard metacard = generateMetacard(createdContentItem,
                    mimeType,
                    createdContentItem.getUri(),
                    stream,
                    input.getRequest()
                            .getProperties());
            String catalogId = cataloger.createMetacard(metacard);
            LOGGER.debug("catalogId = " + catalogId);
            Map<String, String> properties = response.getResponseProperties();
            properties.put(CATALOG_ID, catalogId);
            response.setResponseProperties(properties);
            if (metacard != null) {
                try {
                    BinaryContent binaryContent = catalogFramework.transform(metacard,
                            DEFAULT_METACARD_TRANSFORMER,
                            null);
                    response.setCreatedMetadata(binaryContent.getByteArray());
                    response.setCreatedMetadataMimeType(binaryContent.getMimeType()
                            .toString());
                } catch (IOException | CatalogTransformerException e) {
                    LOGGER.warn("Unable to transform metacard to readable metadata.", e);
                }
            }
        } catch (MetacardCreationException e) {
            LOGGER.warn(e.getMessage(), e);
            throw new PluginExecutionException(e.getMessage(), e);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
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
            throw new PluginExecutionException("Unable to read InputStream in updated content item.",
                    e);
        }

        if (stream == null) {
            throw new PluginExecutionException("InputStream is null in updated content item. ");
        }

        try {
            Metacard metacard = generateMetacard(updatedContentItem,
                    mimeType,
                    updatedContentItem.getUri(),
                    stream,
                    null);
            String catalogId = cataloger.updateMetacard(updatedContentItem.getUri(), metacard);
            LOGGER.debug("catalogId = " + catalogId);
            Map<String, String> properties = response.getResponseProperties();
            properties.put(CATALOG_ID, catalogId);
            response.setResponseProperties(properties);
            if (metacard != null) {
                try {
                    BinaryContent binaryContent = catalogFramework.transform(metacard,
                            DEFAULT_METACARD_TRANSFORMER,
                            null);
                    response.setUpdatedMetadata(binaryContent.getByteArray());
                    response.setUpdatedMetadataMimeType(binaryContent.getMimeType()
                            .toString());
                } catch (IOException | CatalogTransformerException e) {
                    LOGGER.warn("Unable to transform metacard to readable metadata.", e);
                }
            }
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

        String catalogId = cataloger.deleteMetacard(input.getContentItem()
                .getUri());
        if (catalogId != null && !catalogId.isEmpty()) {
            // Create response indicating file (actually, catalog entry) was deleted
            response = new DeleteResponseImpl(input.getRequest(),
                    input.getContentItem(),
                    true,
                    input.getResponseProperties(),
                    input.getProperties());
        }

        LOGGER.debug("catalogId = " + catalogId);
        Map<String, String> properties = response.getResponseProperties();
        properties.put(CATALOG_ID, catalogId);
        response.setResponseProperties(properties);

        LOGGER.trace("EXITING: process(DeleteResponse)");

        return response;
    }

    private Metacard generateMetacard(ContentItem contentItem, MimeType mimeType, String uri,
            InputStream message, Map<String, Serializable> properties)
            throws MetacardCreationException {
        LOGGER.trace("ENTERING: generateMetacard");

        List<InputTransformer> listOfCandidates = mimeTypeToTransformerMapper.findMatches(
                InputTransformer.class,
                mimeType);

        LOGGER.debug("List of matches for mimeType [ {} ]: {}", mimeType, listOfCandidates);

        Metacard generatedMetacard = null;
        Metacard contentMetacard = null;
        try (FileBackedOutputStream fileBackedOutputStream = new FileBackedOutputStream(1000000)) {

            long size;
            try {
                size = IOUtils.copyLarge(message, fileBackedOutputStream);
                LOGGER.debug("Copied {} bytes of file in content framework", size);
            } catch (IOException e) {
                throw new MetacardCreationException("Could not copy bytes of content message.", e);
            }

            // Multiple InputTransformers may be found that match the mime type.
            // Need to try each InputTransformer until we find one that can successfully transform
            // the input stream's data into a metacard. Once an InputTransformer is found that
            // can create the metacard, then do not need to try any remaining InputTransformers.
            for (InputTransformer transformer : listOfCandidates) {

                try (InputStream inputStreamMessageCopy = fileBackedOutputStream.asByteSource()
                        .openStream()) {
                    generatedMetacard = transformer.transform(inputStreamMessageCopy);
                    contentMetacard = new MetacardImpl(new ContentMetacardType());

                    //copy attributes in loop
                    for (AttributeDescriptor descriptor : generatedMetacard.getMetacardType()
                            .getAttributeDescriptors()) {
                        Attribute attribute = generatedMetacard.getAttribute(descriptor.getName());
                        if (attribute != null) {
                            contentMetacard.setAttribute(attribute);
                        }
                    }

                    try {
                        Subject subject = SecurityUtils.getSubject();
                        if (subject != null) {
                            contentMetacard.setAttribute(new AttributeImpl(Metacard.POINT_OF_CONTACT,
                                    SubjectUtils.getName(subject)));
                        }
                    } catch (IllegalStateException e) {
                        LOGGER.debug("Unable to retrieve user from request.", e);
                    } catch (UnavailableSecurityManagerException e) {
                        LOGGER.debug("Unable to retrieve Security Manager.", e);
                    }

                    if (uri != null) {
                        //Setting the non-transformer specific information not including creation and modification dates/times
                        contentMetacard.setAttribute(new AttributeImpl(Metacard.RESOURCE_URI, uri));
                        contentMetacard.setAttribute(new AttributeImpl(Metacard.RESOURCE_SIZE,
                                String.valueOf(size)));
                    } else {
                        LOGGER.debug("Metacard had a null uri");
                    }
                    if (StringUtils.isBlank(contentMetacard.getTitle())) {
                        LOGGER.debug("Metacard title was blank. Setting title to filename.");
                        contentMetacard.setAttribute(new AttributeImpl(Metacard.TITLE,
                                contentItem.getFilename()));
                    }
                    break;
                } catch (IOException | CatalogTransformerException e) {
                    LOGGER.debug("Transformer [" + transformer + "] could not create metacard.", e);
                }

            }

            if (contentMetacard == null) {
                throw new MetacardCreationException(
                        "Could not create metacard with mimeType " + mimeType
                                + ". No valid transformers found.");
            }

            LOGGER.trace("EXITING: generateMetacard");
        } catch (IOException e) {
            LOGGER.debug("Error encountered while using filed backed stream.", e);
        }

        if (properties != null) {
            if (properties.containsKey(ContentMetacardType.RESOURCE_CHECKSUM)) {
                String checksumValue =
                        (String) properties.get(ContentMetacardType.RESOURCE_CHECKSUM);
                contentMetacard.setAttribute(new AttributeImpl(ContentMetacardType.RESOURCE_CHECKSUM,
                        checksumValue));
            }

            if (properties.containsKey(ContentMetacardType.RESOURCE_CHECKSUM_ALGORITHM)) {
                String checksumAlgrotithm =
                        (String) properties.get(ContentMetacardType.RESOURCE_CHECKSUM_ALGORITHM);
                contentMetacard.setAttribute(new AttributeImpl(ContentMetacardType.RESOURCE_CHECKSUM_ALGORITHM,
                        checksumAlgrotithm));
            }
        }

        return contentMetacard;
    }

}
