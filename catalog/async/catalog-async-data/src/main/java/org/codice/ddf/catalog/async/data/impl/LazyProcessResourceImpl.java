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
package org.codice.ddf.catalog.async.data.impl;

import static org.codice.ddf.catalog.async.data.impl.ProcessResourceImpl.DEFAULT_MIME_TYPE;
import static org.codice.ddf.catalog.async.data.impl.ProcessResourceImpl.DEFAULT_NAME;

import com.google.common.io.Closer;
import ddf.catalog.resource.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Supplier;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.catalog.async.data.api.internal.InaccessibleResourceException;
import org.codice.ddf.catalog.async.data.api.internal.ProcessResource;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LazyProcessResourceImpl implements ProcessResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(LazyProcessResourceImpl.class);

  private static final int FILE_BACKED_OUTPUT_STREAM_THRESHOLD = 32 * 1024; // 32 kilobytes

  private URI uri;

  private String name = DEFAULT_NAME;

  private String mimeType = DEFAULT_MIME_TYPE;

  private volatile long size = UNKNOWN_SIZE;

  private InputStream inputStream;

  private TemporaryFileBackedOutputStream resourceDataCache;

  private Closer streamCloser;

  private String qualifier;

  private boolean isModified = false;

  private boolean isResourceLoaded = false;

  private Supplier<Resource> resourceSupplier;

  private String metacardId;

  /**
   * Creates a {@link ProcessResource} with a {@link Supplier} used to load the {@link Resource}.
   * The resource will not be loaded until the first time one of the following fields is accessed:
   * inputStream mimeType name
   *
   * @param metacardId schema specific part of {@link URI}, throws {@link IllegalArgumentException}
   *     if empty or null
   * @param resourceSupplier a {@link Supplier} used to load the resource
   * @throws IllegalArgumentException if the input provided is not valid
   */
  public LazyProcessResourceImpl(String metacardId, Supplier<Resource> resourceSupplier) {
    if (StringUtils.isBlank(metacardId)) {
      throw new IllegalArgumentException(
          "ProcessResourceImpl argument \"metacardId\" may not be null or empty.");
    }
    if (resourceSupplier == null) {
      throw new IllegalArgumentException(
          "LazyProcessResourceImpl must have a non null resource supplier");
    }

    this.isModified = false;
    this.metacardId = metacardId;
    this.resourceSupplier = resourceSupplier;
    this.isResourceLoaded = false;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This will load the resource.
   */
  @Override
  public synchronized InputStream getInputStream() throws IOException {
    if (resourceDataCache == null) {
      loadResource();

      if (inputStream == null) {
        throw new IOException(String.format("Tried to get input stream for %s but was null", name));
      }
      resourceDataCache = new TemporaryFileBackedOutputStream(FILE_BACKED_OUTPUT_STREAM_THRESHOLD);
      IOUtils.copyLarge(inputStream, resourceDataCache);
      IOUtils.closeQuietly(inputStream);
      streamCloser.register(resourceDataCache);
    }
    InputStream newInputStream = resourceDataCache.asByteSource().openStream();
    streamCloser.register(newInputStream);

    return newInputStream;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This will load the resource.
   */
  @Override
  public synchronized String getMimeType() {
    loadResource();
    return mimeType;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This will load the resource.
   */
  @Override
  public synchronized String getName() {
    loadResource();
    return name;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This value should not require the resources to be loaded.
   */
  @Override
  public String getQualifier() {
    return qualifier;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This value should not require the resources to be loaded.
   */
  @Override
  public long getSize() {
    return size;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This value should not require the resources to be loaded.
   */
  @Override
  public URI getUri() {
    return uri;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public void setUri(URI uri) {
    this.uri = uri;

    if (uri != null && StringUtils.isNotBlank(uri.getFragment())) {
      this.qualifier = uri.getFragment();
    }
  }

  @Override
  public boolean isModified() {
    return isModified;
  }

  @Override
  public synchronized void close() {
    IOUtils.closeQuietly(streamCloser);
  }

  public void markAsModified() {
    isModified = true;
  }

  private synchronized void loadResource() {
    if (!isResourceLoaded) {
      populateProcessResource(resourceSupplier.get());
    }
  }

  private synchronized void populateProcessResource(Resource resource) {
    if (resource == null) {
      String message =
          "Error loading resource for metacard id: "
              + metacardId
              + ", URI: "
              + uri
              + ". Null resource was returned by supplier. The resource hasn't been loaded.";
      LOGGER.debug(message);

      throw new InaccessibleResourceException(message);
    }

    this.inputStream = resource.getInputStream();
    this.streamCloser = Closer.create();
    this.streamCloser.register(inputStream);

    if (this.size == UNKNOWN_SIZE) {
      this.size = resource.getSize();
    }

    String resourceName = resource.getName();
    if (StringUtils.isNotBlank(resourceName)) {
      this.name = resourceName;
    }

    String resourceMimeType = resource.getMimeTypeValue();
    if (StringUtils.isNotBlank(resourceMimeType)) {
      this.mimeType = resourceMimeType;
    }

    isResourceLoaded = true;
  }
}
