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
package org.codice.ddf.catalog.content.monitor;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.validation.constraints.NotNull;

/**
 * The core of this file is based upon {@link org.apache.commons.io.monitor.FileEntry}, modified to
 * update only on successful async operations.
 *
 * <p>A wrapper class for a Java {@link File} object. This essentially keeps meta-snapshots taken
 * when commit() is called. hasChanged will compare the current value of the file to the last time
 * it's been committed, or the snapshot.
 *
 * @apiNote once hasChanged returns true, the user must commit the file once it's finished
 *     processing to create a new meta-snapshot.
 */
public class AsyncFileEntry implements Serializable, Comparable<AsyncFileEntry> {

  private final File contentFile;
  private boolean exists;
  private long lastModified;
  private boolean directory;
  private long length;

  private final Set<AsyncFileEntry> children = new ConcurrentSkipListSet<>();

  private AsyncFileEntry parent;

  private String name;

  public AsyncFileEntry(File file) {
    this(null, file);
  }

  public AsyncFileEntry(AsyncFileEntry parent, File file) {
    this.parent = parent;
    contentFile = file;
    refresh();
  }

  private void refresh() {
    name = contentFile.getName();
    exists = contentFile.exists();
    lastModified = exists ? contentFile.lastModified() : 0;
    directory = contentFile.exists() && contentFile.isDirectory();
    length = getLength();
  }

  public synchronized boolean hasChanged() {

    final boolean snapExist = contentFile.exists();
    final long snapLastModified = snapExist ? contentFile.lastModified() : 0;
    final boolean snapOrigDirectory = snapExist && contentFile.isDirectory();
    final long snapLength = getLength();

    //  Checks to see if the file has been changed
    return exists != snapExist
        || lastModified != snapLastModified
        || directory != snapOrigDirectory
        || length != snapLength;
  }

  public String getName() {
    return name;
  }

  public File getFile() {
    return contentFile;
  }

  public boolean isDirectory() {
    return directory;
  }

  public Optional<AsyncFileEntry> getParent() {
    return Optional.ofNullable(parent);
  }

  /**
   * checkNetwork:
   *
   * <p>Checking the network by checking the directory under the file. This works under two
   * assumptions:
   *
   * <ol>
   *   <li>The root directory will NOT be deleted
   *   <li>There will NOT be a semlink to another NFS
   * </ol>
   */
  public boolean checkNetwork() {
    AsyncFileEntry rootParent = parent;
    while (rootParent != null && rootParent.getParent().isPresent()) {
      rootParent = rootParent.getParent().get();
    }
    if (rootParent == null) {
      return contentFile.exists();
    }

    return rootParent.getFile().exists();
  }

  /**
   * commit:
   *
   * <p>Tells the AsyncFileEntry that it's successfully been processed and to take a new
   * meta-snapshot
   */
  public void commit() {
    refresh();
  }

  public long getLength() {
    return contentFile.exists() && !contentFile.isDirectory() ? contentFile.length() : 0;
  }

  public boolean exists() {
    return exists;
  }

  /**
   * getChildren:
   *
   * <p>Note: returns a new List to avoid sync access exceptions
   *
   * @return A new sorted List
   */
  public List<AsyncFileEntry> getChildren() {
    return new ArrayList<>(children);
  }

  public void addChild(AsyncFileEntry child) {
    children.add(child);
  }

  public void removeChild(AsyncFileEntry child) {
    children.remove(child);
  }

  public boolean hasChild(AsyncFileEntry child) {
    return children.contains(child);
  }

  @Override
  public int compareTo(@NotNull AsyncFileEntry o) {
    return getFile().compareTo(o.getFile());
  }

  /**
   * equals:
   *
   * @param o object to compare equality
   * @return true if the {@link File} being wrapped by a {@link AsyncFileEntry} equals another
   *     {@link File} False if the object is not a {@link AsyncFileEntry} or a {@link File}
   */
  @Override
  public boolean equals(@NotNull Object o) {
    if (o instanceof AsyncFileEntry) {
      return contentFile.equals(((AsyncFileEntry) o).getFile());
    } else if (o instanceof File) {
      return contentFile.equals(o);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return contentFile.hashCode();
  }

  public int compareToFile(File file) {
    return contentFile.compareTo(file);
  }

  public void destroy() {
    exists = false;
  }

  /**
   * getFromParent:
   *
   * <p>gets the {@link AsyncFileEntry} from the parent if it exists.
   *
   * @return the entry from the parent or null.
   */
  public AsyncFileEntry getFromParent() {
    if (getParent().isPresent()) {
      if (getParent().get().hasChild(this)) {
        List<AsyncFileEntry> children = getParent().get().getChildren();
        return children.get(children.indexOf(this));
      }
    }
    return null;
  }
}
