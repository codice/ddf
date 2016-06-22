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
package org.codice.ddf.catalog.transformer.zip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.ResourceRequestByProductUri;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

public class ZipCompression implements QueryResponseTransformer {

    public CatalogFramework catalogFramework;

    public static final String METACARD_PATH = "metacards" + File.separator;

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipCompression.class);

    /**
     * Transforms a SourceResponse with a list of {@link Metacard}s into a {@link BinaryContent} item
     * with an {@link InputStream}.  This transformation expects a key-value pair "fileName"-zipFileName to be present.
     *
     * @param upstreamResponse - a SourceResponse with a list of {@link Metacard}s to compress
     * @param arguments        - a map of arguments to use for processing.  This method expects "fileName" to be set
     * @return - a {@link BinaryContent} item with the {@link InputStream} for the Zip file
     * @throws CatalogTransformerException when the transformation fails
     */
    @Override
    public BinaryContent transform(SourceResponse upstreamResponse,
            Map<String, Serializable> arguments) throws CatalogTransformerException {

        if (upstreamResponse == null || CollectionUtils.isEmpty(upstreamResponse.getResults())) {
            throw new CatalogTransformerException("No Metacards were found to transform.");
        }

        if (MapUtils.isEmpty(arguments) || !arguments.containsKey(ZipDecompression.FILE_PATH)) {
            throw new CatalogTransformerException("No 'filePath' argument found in arguments map.");
        }

        ZipFile zipFile;
        String filePath = (String) arguments.get(ZipDecompression.FILE_PATH);
        try {
            zipFile = new ZipFile(filePath);
        } catch (ZipException e) {
            LOGGER.debug("Unable to create zip file with path : {}", filePath, e);
            throw new CatalogTransformerException(String.format("Unable to create zip file at %s",
                    filePath), e);
        }

        List<Result> resultList = upstreamResponse.getResults();

        Map<Resource, String> resourceMap = new HashMap<>();

        resultList.stream()
                .map(Result::getMetacard)
                .forEach(metacard -> {
                    ZipParameters zipParameters = new ZipParameters();
                    zipParameters.setSourceExternalStream(true);
                    zipParameters.setFileNameInZip(METACARD_PATH + metacard.getId());

                    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            ObjectOutputStream objectOutputStream = new ObjectOutputStream(
                                    byteArrayOutputStream)) {

                        objectOutputStream.writeObject(new MetacardImpl(metacard));
                        InputStream inputStream =
                                new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                        zipFile.addStream(inputStream, zipParameters);

                        if (hasLocalResources(metacard)) {
                            resourceMap.putAll(getAllMetacardContent(metacard));
                        }

                    } catch (IOException | ZipException e) {
                        LOGGER.debug("Failed to add metacard with id {}.", metacard.getId(), e);
                    }
                });

        resourceMap.forEach((resource, filename) -> {
            try {
                ZipParameters zipParameters = new ZipParameters();
                zipParameters.setSourceExternalStream(true);
                zipParameters.setFileNameInZip(filename);
                zipFile.addStream(resource.getInputStream(), zipParameters);
            } catch (ZipException e) {
                LOGGER.debug("Failed to add resource with id {} to zip.", resource.getName(), e);
            }
        });

        BinaryContent binaryContent;
        try {
            InputStream fileInputStream =
                    new ZipInputStream(new FileInputStream(zipFile.getFile()));
            binaryContent = new BinaryContentImpl(fileInputStream);
        } catch (FileNotFoundException e) {
            throw new CatalogTransformerException("Unable to get ZIP file from ZipInputStream.", e);
        }
        return binaryContent;
    }

    private boolean hasLocalResources(Metacard metacard) {
        return metacard.getSourceId()
                .equals(SystemInfo.getSiteName()) && (metacard.getResourceURI() != null
                || metacard.getAttribute(Metacard.DERIVED_RESOURCE_URI) != null);
    }

    private Map<Resource, String> getAllMetacardContent(Metacard metacard) {
        Map<Resource, String> resourceList = new HashMap<>();
        Attribute attribute = metacard.getAttribute(Metacard.DERIVED_RESOURCE_URI);

        if (attribute != null) {

            List<Serializable> serializables = attribute.getValues();
            serializables.forEach(serializable -> {
                String fragment = ZipDecompression.CONTENT + File.separator;
                try {
                    URI uri = new URI((String) serializable);
                    String derivedResourceFragment = uri.getFragment();
                    if (StringUtils.isNotBlank(derivedResourceFragment)) {
                        fragment += derivedResourceFragment + File.separator;
                    }
                } catch (URISyntaxException e) {
                    LOGGER.debug("Invalid Derived Resource URI Syntax for metacard : {}",
                            metacard.getId(),
                            e);
                }

                Resource resource = getResource(serializable, metacard);
                if (resource != null) {
                    resourceList.put(resource,
                            fragment + metacard.getId() + "-" + resource.getName());
                }
            });
        }

        URI resourceUri = metacard.getResourceURI();
        if (resourceUri != null) {
            Resource resource = getResource(resourceUri.toString(), metacard);

            if (resource != null) {
                resourceList.put(resource,
                        ZipDecompression.CONTENT + File.separator + metacard.getId() + "-"
                                + resource.getName());
            }
        }
        return resourceList;
    }

    private Resource getResource(Serializable serializable, Metacard metacard) {
        Resource resource = null;
        URI fileUri;

        try {
            fileUri = new URI((String) serializable);
            ResourceRequest resourceRequest = new ResourceRequestByProductUri(fileUri);
            ResourceResponse resourceResponse = catalogFramework.getLocalResource(resourceRequest);
            resource = resourceResponse.getResource();
        } catch (URISyntaxException | IOException | ResourceNotFoundException | ResourceNotSupportedException e) {
            LOGGER.debug("Unable to retrieve content from metacard resource uri : {}",
                    metacard.getResourceURI(),
                    e);
        }
        return resource;
    }

    public void setCatalogFramework(CatalogFramework catalogFramework) {
        this.catalogFramework = catalogFramework;
    }

    public CatalogFramework getCatalogFramework() {
        return catalogFramework;
    }
}