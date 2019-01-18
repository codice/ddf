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
 * AsyncFileEntry
 *
 * <p>The core of this file is based upon {@link org.apache.commons.io.monitor.FileEntry}, modified
 * update only on successful async operations.
 *
 * <p>A wrapper class for a Java {@link File} object. This essentially keeps meta-snapshots when
 * hasChanged is called. If a change occurs, then the {@link AsyncFileEntry} will enter a
 * non-commited state. A AsyncFileEntry in a non-committed state will always return true on a
 * hasChanged call because it has not been committed. In addition, the AsyncFileEntry's metadata
 * snapshot will NOT update while in a non-committed state to avoid files "showing" they've been
 * updated when the changes haven't gone through yet. Once the changes are committed and finalized
 * then it's up to the user to tell the AsyncFileEntry that the changes were successful by
 * calling commit().
 *
 * @apiNote Once hasChanged() returns true it's up to the user to put the {@link AsyncFileEntry}
 *     back into a finalized state by calling commit().
 * @apiNote isInitCommit() will tell if the file has ever been committed. Useful for sending
 *     additional create operations even if it appears from a child.
 */
public class AsyncFileEntry implements Serializable, Comparable<AsyncFileEntry> {

  private final File contentFile;
  private boolean exists;
  private long lastModified;
  private boolean directory;
  private long length;

  private boolean committed = false;

  private boolean initCommit = false;

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
    directory = exists && contentFile.isDirectory();
    length = exists && !directory ? contentFile.length() : 0;
  }

  public synchronized boolean hasChanged() {

    //  If the file is not committed it "hasChanged" and we don't want to update the snapshot (case
    // it changes again)
    if (!committed) {
      return true;
    }

    // cache original values
    final boolean origExists = exists;
    final long origLastModified = lastModified;
    final boolean origDirectory = directory;
    final long origLength = length;

    // refresh the values
    refresh();

    //  Checks to see if the file has been changed
    boolean changed =
        exists != origExists
            || lastModified != origLastModified
            || directory != origDirectory
            || length != origLength;

    if (changed) {
      committed = false;
    }

    return changed;
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
   * <ol>
   *     <li>The root directory will NOT be deleted</li>
   *     <li>There will NOT be a semlink to another NFS</li>
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
   * <p>Tells the AsyncFileEntry that it's successfully been processed and to] put it back into a
   * non-committed state.
   */
  public void commit() {
    committed = true;
    initCommit = true;
  }

  public boolean isCommitted() {
    return committed;
  }

  /**
   * isInitCommit:
   *
   * <p>Checks to see if the file has ever been committed
   *
   * @return true if commit() has been called.
   */
  public boolean isInitCommit() {
    return initCommit;
  }

  public long getLength() {
    return contentFile.length();
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
    initCommit = false;
  }
}
