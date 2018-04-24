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
package org.codice.ddf.catalog.harvest.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.codice.ddf.catalog.harvest.HarvestedResource;
import org.codice.ddf.catalog.harvest.Listener;
import org.codice.ddf.catalog.harvest.common.HarvestedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectoryHarvesterListenerAdaptor extends FileAlterationListenerAdaptor {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DirectoryHarvesterListenerAdaptor.class);

  private final Set<Listener> listeners;

  DirectoryHarvesterListenerAdaptor(Set<Listener> listeners) {
    this.listeners = listeners;
  }

  @Override
  public void onFileCreate(final File file) {
    HarvestedResource harvestedResource = createHarvestedResource(file);
    if (harvestedResource != null) {
      listeners.forEach(listener -> listener.onCreate(harvestedResource));
    }
  }

  @Override
  public void onFileChange(final File file) {
    HarvestedResource harvestedResource = createHarvestedResource(file);
    if (harvestedResource != null) {
      listeners.forEach(listener -> listener.onUpdate(harvestedResource));
    }
  }

  @Override
  public void onFileDelete(final File file) {
    listeners.forEach(listener -> listener.onDelete(file.toURI().toASCIIString()));
  }

  @Nullable
  private HarvestedResource createHarvestedResource(File file) {
    try {
      return new HarvestedFile(
          new FileInputStream(file), file.getName(), file.toURI().toASCIIString());
      // TODO audit
    } catch (FileNotFoundException e) {
      LOGGER.debug(
          "Failed to get input stream from file [{}]. Create event will not be sent to listener",
          file.toURI(),
          e);
      return null;
    }
  }
}
