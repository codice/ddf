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
package ddf.catalog.resource.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.activation.MimeType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.resource.Resource;

/**
 * The resource that will be stored in the @ResourceCache cache map.
 *
 */
public class ReliableResource implements Resource, Serializable {
    
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ReliableResource.class);
    
    private String filePath;
    private MimeType mimeType;
    private String resourceName;
    private long size = -1L;
    private long lastTouchedMillis = 0L;
    
    // The key used to store this object in the cache map
    private String key;

    private Metacard metacard;
    
    
//    public ReliableResource(String key, String filePath) {
//        this(key, filePath, null, null);
//    }
    
    public ReliableResource(String key, String filePath, MimeType mimeType, String name, Metacard metacard) {
        this.key = key;
        this.filePath = filePath;
        this.mimeType = mimeType;
        this.resourceName = name;
        this.metacard = metacard;
    }
    
    public String getFilePath() {
        return filePath;
    }


    /**
     * Key is also filename of where file is stored.
     *
     * @return key
     */
    public String getKey() {
        return key;
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
            LOGGER.info("Could not retrieve file [{}]", filePath, e);
            return null;
        }
    }

    private InputStream getProduct() throws IOException {
        if (filePath == null) {
            return null;
        }
        LOGGER.info("filePath = {}", filePath);
        return FileUtils.openInputStream(new File(filePath));
    }

    /**
     * Returns true if the product file exists in the product cache directory.
     *
     * @return
     */
    public boolean hasProduct() {
        if (filePath == null) {
            return false;
        }
        return new File(filePath).exists();
    }

    @Override
    public MimeType getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(MimeType mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String getMimeTypeValue() {
        return mimeType != null ? mimeType.getBaseType() : null;
    }

    @Override
    public long getSize() {
        LOGGER.debug("getting size = {}", size);
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public String getName() {
        return resourceName;
    }
    
    public void setName(String name) {
        this.resourceName = name;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public long getLastTouchedMillis() {
        return lastTouchedMillis;
    }

    public void setLastTouchedMillis(long lastTouchedMillis) {
        this.lastTouchedMillis = lastTouchedMillis;
    }

    public Metacard getMetacard() {
        return metacard;
    }
}
