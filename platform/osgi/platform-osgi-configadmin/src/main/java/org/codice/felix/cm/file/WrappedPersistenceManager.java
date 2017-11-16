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

/**
 * Basic wrapper class for enhancing functionality of any persistence manager with additional layers
 * of processing.
 *
 * <p><b>See FELIX-4005 & FELIX-4556. This class cannot utilize Java 8 language constructs due to
 * maven bundle plugin 2.3.7</b>
 */
public class WrappedPersistenceManager implements PersistenceManager, AutoCloseable {

  private final PersistenceManager persistenceManager;

  WrappedPersistenceManager(PersistenceManager persistenceManager) {
    if (persistenceManager == null) {
      throw new IllegalArgumentException("PersistenceManager cannot be null");
    }
    this.persistenceManager = persistenceManager;
  }

  protected PersistenceManager getInnerPersistenceManager() {
    return persistenceManager;
  }

  @Override
  public void close() throws Exception {
    if (persistenceManager instanceof AutoCloseable) {
      ((WrappedPersistenceManager) persistenceManager).close();
    }
  }

  @Override
  public boolean exists(String pid) {
    return persistenceManager.exists(pid);
  }

  @Override
  public Dictionary load(String pid) throws IOException {
    return persistenceManager.load(pid);
  }

  @Override
  public Enumeration getDictionaries() throws IOException {
    return persistenceManager.getDictionaries();
  }

  @Override
  public void store(String pid, Dictionary properties) throws IOException {
    persistenceManager.store(pid, properties);
  }

  @Override
  public void delete(String pid) throws IOException {
    persistenceManager.delete(pid);
  }
}
