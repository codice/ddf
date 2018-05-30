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
package org.codice.ddf.catalog.ui.metacard.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.activation.MimeType;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.ui.metacard.internal.StorableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Splits a ZIP file into its immediate children. Does not recurse through each child item. */
public class ZipSplitter extends AbstractSplitter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ZipSplitter.class);

  public ZipSplitter(String id, Set<MimeType> mimeTypes) {
    super(id, mimeTypes);
  }

  @Override
  public Stream<StorableResource> split(
      StorableResource storableResource, Map<String, ? extends Serializable> arguments)
      throws IOException {
    ZipIterator zipIterator = new ZipIterator(storableResource.getInputStream());
    return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(zipIterator, Spliterator.NONNULL), false)
        .onClose(zipIterator::close);
  }

  @Override
  protected Map<String, Object> getAdditionalProperties() {
    return Collections.emptyMap();
  }

  private static class ZipIterator implements Iterator<StorableResource>, AutoCloseable {

    private ZipInputStream zipInputStream;
    private ZipEntry zipEntry;

    ZipIterator(InputStream inputStream) throws IOException {
      zipInputStream = new ZipInputStream(inputStream);
      zipEntry = zipInputStream.getNextEntry();
    }

    @Override
    public boolean hasNext() {
      return zipEntry != null;
    }

    @Override
    public StorableResource next() {
      if (!hasNext()) {
        throw new NoSuchElementException("There are no more items in the ZIP.");
      }
      try {
        String filename = zipEntry.getName();
        StorableResource storableResource = new StorableResourceImpl(zipInputStream, filename);
        safeCloseEntry();
        zipEntry = zipInputStream.getNextEntry();
        return storableResource;
      } catch (IOException e) {
        String message = "Failed to get the next item in the ZIP file.";
        LOGGER.debug(message, e);
        return new StorableResourceImpl(message);
      }
    }

    @Override
    public void close() {
      if (zipInputStream != null) {
        safeCloseEntry();
        IOUtils.closeQuietly(zipInputStream);
        zipInputStream = null;
        zipEntry = null;
      }
    }

    private void safeCloseEntry() {
      try {
        zipInputStream.closeEntry();
      } catch (IOException e) {
        LOGGER.debug("Unable to close zip entry.", e);
      }
    }
  }
}
