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
package ddf.catalog.cache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimeType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.cache.CacheException;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;

/**
 * Contains the details of a Resource including where it is stored and how to retrieve that Resource
 * locally.
 */
public class CachedResource implements Resource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedResource.class);

    /** Directory for products cached to file system */
    private String productCacheDirectory;

    private String filePath;

    private MimeType mimeType;

    private String resourceName;

    private long size = -1L;

    private String key;

    public CachedResource(String productCacheDirectory) {
        this.productCacheDirectory = productCacheDirectory;
    }

    public String store(Metacard metacard, ResourceResponse resourceResponse) throws CacheException {

        Resource resource = resourceResponse.getResource();

        // TODO check resource if null

        CacheKey keyMaker = new CacheKey(metacard, resourceResponse.getRequest());

        key = keyMaker.generateKey();

        LOGGER.debug("ENTERING: store() where key {} ", key);

        filePath = FilenameUtils.concat(productCacheDirectory, key);

        LOGGER.debug("Copying resource to filepath = " + filePath);
        try {
            // Copy product's bytes from InputStream to product cache file.
            // Resource cache file will be created if it doesn't exist, or
            // overwritten if
            // it already exists.
            // The product's InputStream will be closed when copy completes.
            InputStream source = resource.getInputStream();
            try {

                FileOutputStream output = FileUtils.openOutputStream(new File(filePath));
                try {
                    size = IOUtils.copyLarge(source, output);
                } finally {
                    IOUtils.closeQuietly(output);
                }
            } finally {
                IOUtils.closeQuietly(source);
            }

        } catch (IOException e) {
            throw new CacheException("Unable to store product file " + filePath, e);
        }

        // copy the rest of the contents of the resource
        mimeType = resource.getMimeType();
        resourceName = resource.getName();

        LOGGER.debug("EXITING: store()");

        return filePath;
    }

    @Override
    public byte[] getByteArray() throws IOException {

        return IOUtils.toByteArray(getProduct());
    }

    /**
     * {@inheritDoc} Creates a new inputStream upon request.
     * 
     * @return InputStream of the product, otherwise {@code null} if could not be retrieved.
     */
    public InputStream getInputStream() {
        try {
            return getProduct();
        } catch (IOException e) {
            LOGGER.info("Could not retrieve file {" + filePath + " }.", e);
            return null;
        }
    }

    public InputStream getProduct() throws IOException {
        return FileUtils.openInputStream(new File(filePath));
    }

    @Override
    public MimeType getMimeType() {
        return mimeType;
    }

    @Override
    public String getMimeTypeValue() {
        return mimeType != null ? mimeType.getBaseType() : null;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public String getName() {
        return resourceName;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     * Key is also filename of where file is stored.
     * 
     * @return key
     */
    public String getKey() {

        return key;
    }

}
