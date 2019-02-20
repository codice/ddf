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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * The core of this file is based upon {@link org.apache.commons.io.monitor.FileEntry}, modified to
 * update only on successful async operations.
 *
 * <p>This class is not entirely thread safe. Getting the children and parents are thread safe,
 * However, {@link #commit()} and {@link #hasChanged()} are not thread safe.
 *
 * <p>A wrapper class for a Java {@link File} object. This essentially keeps meta-snapshots taken
 * when ${@link AsyncFileEntry#commit()} is called. ${@link AsyncFileEntry#hasChanged()} will
 * compare the current value of the file to the last time it's been committed, or the snapshot.
 *
 * @apiNote once ${@link AsyncFileEntry#hasChanged()} returns true, the user must call ${@link
 *     AsyncFileEntry#commit()} once the file is finished processing to create a new meta-snapshot.
 * @see AsyncFileEntry#initialize()
 */
public class AsyncFileEntry implements Comparable<AsyncFileEntry> {

  private final File contentFile;
  private boolean exists;
  private long lastModified;
  private boolean directory;
  private long length;

  private final Set<AsyncFileEntry> children = new ConcurrentSkipListSet<>();
  //  Leaving transient to avoid loops
  @Nullable private transient AsyncFileEntry parent;

  private String name;

  public AsyncFileEntry(File file) {
    this(null, file);
  }

  public AsyncFileEntry(@Nullable AsyncFileEntry parent, File file) {
    this.parent = parent;
    contentFile = file;
    refresh();
  }

  //  For GSON serialization
  private AsyncFileEntry() {
    contentFile = null;
  }

  /**
   * Must be called when a {@link AsyncFileEntry} is loaded from a json file.
   *
   * @throws IllegalStateException if a {@link File} is null
   */
  public void initialize() {
    initializeFileEntryHelper(this);
  }

  public boolean hasChanged() {

    //  Checks to see if the file has been changed
    return exists != snapExist()
        || lastModified != snapLastModified()
        || directory != snapDirectory()
        || length != snapLength();
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
   * Checking the network by checking the directory under the file. This works under two
   * assumptions:
   *
   * <ol>
   *   <li>The root directory will NOT be deleted
   *   <li>There will NOT be a symlink to another NFS
   * </ol>
   */
  public boolean checkNetwork() {
    AsyncFileEntry rootParent = parent;

    if (rootParent == null) {
      return snapExist();
    }

    while (rootParent.getParent().isPresent()) {
      rootParent = rootParent.getParent().get();
    }

    return rootParent.getFile().exists();
  }

  /**
   * Tells the AsyncFileEntry that it's successfully been processed and to take a new meta-snapshot
   */
  public void commit() {
    refresh();
  }

  /**
   * Note: returns a new List to avoid sync access exceptions
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

  @Override
  public int compareTo(@NotNull AsyncFileEntry o) {
    return getFile().compareTo(o.getFile());
  }

  /**
   * @param o object to compare equality
   * @return true if the {@link File} being wrapped by a {@link AsyncFileEntry} equals another
   *     {@link File} False if the object is not a {@link AsyncFileEntry} or a {@link File}
   */
  @Override
  public boolean equals(@NotNull Object o) {
    if (o instanceof AsyncFileEntry) {
      return contentFile.equals(((AsyncFileEntry) o).getFile());
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
    children.clear();
  }

  //  Serializing to JSON doesn't allow infinite loops. Thus we
  //  Make the parent null and allow users to re-initialize after loading
  //  from a json.
  private void setParent(@Nullable AsyncFileEntry parentalUnit) {
    parent = parentalUnit;
  }

  private void initializeFileEntryHelper(AsyncFileEntry toInit) {
    for (AsyncFileEntry child : toInit.getChildren()) {
      if (child.getFile() == null) {
        throw new IllegalStateException(
            "File cannot be null. There was a problem initializing the File entries from JSON");
      }
      child.setParent(toInit);
      initializeFileEntryHelper(child);
    }
  }

  private boolean hasChild(AsyncFileEntry child) {
    return children.contains(child);
  }

  private long snapLastModified() {
    return contentFile.exists() ? contentFile.lastModified() : 0;
  }

  private long snapLength() {
    return contentFile.exists() && !contentFile.isDirectory() ? contentFile.length() : 0;
  }

  private boolean snapDirectory() {
    return contentFile.exists() && contentFile.isDirectory();
  }

  private boolean snapExist() {
    return contentFile.exists();
  }

  private String snapName() {
    return contentFile.getName();
  }

  public boolean hasChildren() {
    return !children.isEmpty();
  }

  private void refresh() {
    name = snapName();
    exists = snapExist();
    lastModified = snapLastModified();
    directory = snapDirectory();
    length = snapLength();
  }
}
