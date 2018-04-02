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
package org.codice.felix.cm.file;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import org.apache.felix.cm.PersistenceManager;
import org.codice.felix.cm.internal.ConfigurationStoragePlugin;

/**
 * Basic wrapper class for enhancing functionality of any persistence manager with additional layers
 * of processing. Now supports a one-time registration of a {@link ConfigurationStoragePlugin} to
 * replace the default {@link org.apache.felix.cm.file.FilePersistenceManager}.
 *
 * <p><b>See FELIX-4005 & FELIX-4556. This class cannot utilize Java 8 language constructs due to
 * maven bundle plugin 2.3.7</b>
 */
public class WrappedPersistenceManager implements PersistenceManager, AutoCloseable {

  private final PersistenceManager persistenceManager;

  private ConfigurationStoragePlugin storagePlugin;

  WrappedPersistenceManager(PersistenceManager persistenceManager) {
    if (persistenceManager == null) {
      throw new IllegalArgumentException("PersistenceManager cannot be null");
    }
    this.persistenceManager = persistenceManager;
    this.storagePlugin = null;
  }

  protected PersistenceManager getInnerPersistenceManager() {
    return persistenceManager;
  }

  protected void setStoragePlugin(ConfigurationStoragePlugin storagePlugin) {
    this.storagePlugin = storagePlugin;
  }

  @Override
  public void close() throws Exception {
    if (persistenceManager instanceof AutoCloseable) {
      ((WrappedPersistenceManager) persistenceManager).close();
    }
  }

  @Override
  public boolean exists(String pid) {
    if (storagePlugin != null) {
      return storagePlugin.exists(pid);
    }
    return persistenceManager.exists(pid);
  }

  @Override
  public Dictionary load(String pid) throws IOException {
    if (storagePlugin != null) {
      return storagePlugin.load(pid);
    }
    return persistenceManager.load(pid);
  }

  @Override
  public Enumeration getDictionaries() throws IOException {
    if (storagePlugin != null) {
      return storagePlugin.getDictionaries();
    }
    return persistenceManager.getDictionaries();
  }

  @Override
  public void store(String pid, Dictionary properties) throws IOException {
    if (storagePlugin != null) {
      storagePlugin.store(pid, properties);
      return;
    }
    persistenceManager.store(pid, properties);
  }

  @Override
  public void delete(String pid) throws IOException {
    if (storagePlugin != null) {
      storagePlugin.delete(pid);
      return;
    }
    persistenceManager.delete(pid);
  }
}
