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

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.codice.ddf.catalog.content.monitor.synchronizations.CompletionSynchronization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Based on {@link org.apache.commons.io.monitor.FileAlterationObserver}, except modified to only
 * update the observer state on successful async request
 *
 * <p>This implementation only works with one AsyncFileAlterationListener.
 *
 * <p>Every time the AsyncFileAlterationObserver is polled by calling {@code checkAndNotify()}, the
 * observer will check all the {@link File}'s and {@link AsyncFileEntry}'s that are under the
 * directory being monitored, and call the {@link AsyncFileAlterationListener}'s corresponding
 * methods
 *
 * @see AsyncFileAlterationListener
 */
public class AsyncFileAlterationObserver {

  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncFileAlterationObserver.class);

  private final AsyncFileEntry rootFile;
  private AsyncFileAlterationListener listener = null;
  private final Set<AsyncFileEntry> fileLocks = new ConcurrentSkipListSet<>();
  private final Object listenerLock = new Object();

  public AsyncFileAlterationObserver(File fileToObserve) {
    if (fileToObserve == null) {
      throw new IllegalArgumentException();
    }
    rootFile = new AsyncFileEntry(fileToObserve);
  }

  private AsyncFileAlterationObserver(AsyncFileEntry entry) {
    if (entry == null) {
      throw new IllegalArgumentException("entry can not be null");
    }
    rootFile = entry;
    rootFile.initialize();
  }

  public static AsyncFileAlterationObserver load(File observedFile, ObjectPersistentStore store) {
    if (observedFile == null || store == null) {
      throw new IllegalArgumentException("Arguments can not be null");
    }
    AsyncFileEntry temp = store.load(observedFile.getName(), AsyncFileEntry.class);
    if (temp == null) {
      return null;
    }
    return new AsyncFileAlterationObserver(temp);
  }

  public void store(ObjectPersistentStore objectPersistentStore) {
    if (objectPersistentStore == null) {
      throw new IllegalArgumentException(
          "Cannot store this object without an ObjectPersistentStore");
    }
    objectPersistentStore.store(rootFile.getName(), rootFile);
  }

  /**
   * Initializes the object state of the Observer.
   *
   * @throws IllegalStateException when the observer fails to initialize and initialization should
   *     be retried
   */
  public void initialize() throws IllegalStateException {
    initChildEntries(rootFile);
  }

  public void destroy() {
    rootFile.destroy();
  }

  @VisibleForTesting
  AsyncFileEntry getRootFile() {
    return rootFile;
  }

  public void setListener(final AsyncFileAlterationListener listener) {
    synchronized (listenerLock) {
      this.listener = listener;
    }
  }

  public void removeListener() {
    //  You cannot remove the listener until the checkAndNotify method is done using it.
    synchronized (listenerLock) {
      this.listener = null;
    }
  }

  /**
   * Called when the observer should compare the snapshot state to the actual state of the directory
   * being monitored.
   */
  public synchronized void checkAndNotify() {

    //  You cannot change listeners in the middle of executions.
    //  Instead of just checking for nulls if a listener is removed mid execution we're going to use
    //  the one we had when we started the method.
    AsyncFileAlterationListener listenerCopy;
    synchronized (listenerLock) {
      if (listener == null) {
        return;
      }
      listenerCopy = listener;
    }

    /* fire directory/file events */
    if (rootFile.checkNetwork()) {
      checkAndNotify(rootFile, rootFile.getChildren(), listFiles(rootFile.getFile()), listenerCopy);
    } else {
      //  If we can't connect to the network then the file doesn't exist to us now.
      LOGGER.debug(
          "The monitored file [{}] does not exist. No file fileLocks will be done through the CDM",
          rootFile.getName());
    }
  }

  /**
   * Fire directory/file created events to the registered listeners.
   *
   * @param entry The file entry
   */
  private void doCreate(AsyncFileEntry entry, final AsyncFileAlterationListener listenerCopy) {

    if (!getFileLock(entry)) {
      //  Return if you cannot get the lock.
      return;
    }

    //  Since we only get a lock on the child and not the parent, we need to make sure
    //  that the parent hasn't been updated from the "previous" child.
    if (entry.isChildOfParent()) {
      removeFileLock(entry);
      return;
    }
    //  At this point this file is stable.

    if (!entry.getFile().isDirectory()) {

      LOGGER.trace("Sending create Request for {}", entry.getName());

      listenerCopy.onFileCreate(
          entry.getFile(), new CompletionSynchronization(entry, this::commitCreate));
    } else {
      // Directories are always committed and added to the parent IF they
      // don't already exist

      File[] children = listFiles(entry.getFile());
      for (File child : children) {
        doCreate(new AsyncFileEntry(entry, child), listenerCopy);
      }

      commitCreate(entry, true);
    }
  }

  /**
   * Callback to allow successful ContentFiles to commit themselves
   *
   * @param entry The AsyncFileEntry wrapping the file being listened to.
   * @param success Boolean that shows if the task failed or completed successfully
   */
  private synchronized void commitCreate(AsyncFileEntry entry, boolean success) {

    LOGGER.debug("commitCreate({},{}): Starting...", entry.getName(), success);
    if (success) {
      entry.commit();
      entry.getParent().ifPresent(e -> e.addChild(entry));
    }
    removeFileLock(entry);
  }

  /**
   * Fire directory/file change events to the registered listeners.
   *
   * @param entry The previous file system entry
   */
  private void doMatch(AsyncFileEntry entry, final AsyncFileAlterationListener listenerCopy) {
    if (!getFileLock(entry)) {
      return;
    }

    if (!entry.hasChanged()) {
      removeFileLock(entry);
      return;
    }

    LOGGER.trace("{} has changed", entry.getName());
    if (!entry.getFile().isDirectory()) {
      LOGGER.trace("Sending Match Request for {}...", entry.getName());
      listenerCopy.onFileChange(
          entry.getFile(), new CompletionSynchronization(entry, this::commitMatch));
    } else {
      //  If a file becomes a directory then we need to delete the contents in the catalog
      //  if a directory becomes a file, we need to create the entry in the catalog
      commitMatch(entry, true);
    }
  }

  /**
   * Callback to allow successful {@link AsyncFileEntry}'s to commit their changes
   *
   * @param entry The AsyncFileEntry wrapping the file being listened to.
   * @param success Boolean that shows if the task failed or completed successfully
   */
  private void commitMatch(AsyncFileEntry entry, boolean success) {
    LOGGER.debug("commitMatch({},{}): Starting...", entry.getName(), success);
    if (success) {
      entry.commit();
    }
    removeFileLock(entry);
  }

  /**
   * Fire directory/file delete events to the registered listeners.
   *
   * @param entry The file entry
   */
  private void doDelete(AsyncFileEntry entry, final AsyncFileAlterationListener listenerCopy) {
    if (!getFileLock(entry)) {
      //  Return if you cannot get the lock.
      return;
    }

    //  Since we only get a lock on the child and not the parent, we need to make sure
    //  that the parent hasn't been updated from the "previous" child.
    if (!entry.isChildOfParent()) {
      removeFileLock(entry);
      return;
    }
    //  At this point this file is stable.

    if (!entry.isDirectory()) {
      LOGGER.trace("Sending Delete Request for {}...", entry.getName());
      listenerCopy.onFileDelete(
          entry.getFile(), new CompletionSynchronization(entry, this::commitDelete));
    }
    //  Once there are no more children we can delete directories.
    //  Check that there are no children, and that no locked files have it as it's parent.
    else if (!entry.hasChildren() && !checkLocks(entry)) {
      commitDelete(entry, true);
    } else {
      //  If there are still children, we're going to keep it within the tree until all the
      //  children are successfully deleted
      removeFileLock(entry);
    }
  }

  /**
   * Callback to allow successful children to remove themselves from their parent directory and put
   * themselves in a state where they have never been committed.
   *
   * @param entry The AsyncFileEntry wrapping the file being listened to.
   * @param success Boolean that shows if the task failed or completed successfully
   */
  private synchronized void commitDelete(AsyncFileEntry entry, boolean success) {
    LOGGER.debug("commitDelete({},{}): Starting...", entry.getName(), success);
    if (success) {
      entry.getParent().ifPresent(e -> e.removeChild(entry));
      entry.destroy();
    }
    removeFileLock(entry);
  }

  /**
   * Steps file by file comparing the snapshot state to the current state of the directory being
   * monitored.
   *
   * @param parent The parent directory (Wrapped in a AsyncFileEntry)
   * @param previous The list of all children of the parent directory (In sorted order)
   * @param files The list of current files (in sorted order)
   */
  private void checkAndNotify(
      final AsyncFileEntry parent,
      final List<AsyncFileEntry> previous,
      @Nullable final File[] files,
      final AsyncFileAlterationListener listenerCopy) {
    //  If there was an IO error then just stop.
    if (files == null) {
      return;
    }

    int c = 0;
    for (final AsyncFileEntry entry : previous) {
      while (c < files.length && entry.compareToFile(files[c]) > 0) {
        doCreate(new AsyncFileEntry(parent, files[c]), listenerCopy);
        c++;
      }
      if (c < files.length && entry.compareToFile(files[c]) == 0) {
        doMatch(entry, listenerCopy);
        checkAndNotify(entry, entry.getChildren(), listFiles(files[c]), listenerCopy);
        c++;
      } else {
        //  Do Delete
        if (!entry.checkNetwork()) {
          //  The file MAY still exist but it's the network that's down.
          return;
        }
        checkAndNotify(entry, entry.getChildren(), FileUtils.EMPTY_FILE_ARRAY, listenerCopy);
        doDelete(entry, listenerCopy);
      }
    }
    for (; c < files.length; c++) {
      doCreate(new AsyncFileEntry(parent, files[c]), listenerCopy);
    }
  }

  /**
   * Note: returns a new Array to avoid sync access exceptions
   *
   * @param file file to retrieve files from.
   * @return A new sorted File Array if {@code file} is a directory, an empty Array if the file is
   *     not a directory, and null if there is an error retrieving the children files.
   */
  private File[] listFiles(File file) {
    if (file.isDirectory()) {
      File[] temp = file.listFiles();
      if (temp != null) {
        Arrays.sort(temp);
        return temp;
      }
      LOGGER.info("There was a problem reading the files contained within [{}]", file.getName());
      return null;
    }
    return FileUtils.EMPTY_FILE_ARRAY;
  }

  private void initChildEntries(AsyncFileEntry parent) throws IllegalStateException {
    File[] children = listFiles(parent.getFile());
    if (children == null) {
      LOGGER.debug("Error while initializing children for [{}]", parent.getName());
      throw new IllegalStateException("Failed to initialize the FileObserver");
    }
    for (File child : children) {
      AsyncFileEntry childEntry = new AsyncFileEntry(parent, child);
      parent.addChild(childEntry);
      initChildEntries(childEntry);
    }
  }

  /**
   * A list of files that are currently being processed by the CDM
   *
   * <p>Holds all the synchronized tags within this file. Potentially should be another class?
   */
  private void removeFileLock(AsyncFileEntry entry) {
    fileLocks.remove(entry);
  }

  private boolean getFileLock(AsyncFileEntry entry) {
    return fileLocks.add(entry);
  }

  private boolean checkLocks(AsyncFileEntry entry) {
    for (AsyncFileEntry locked : fileLocks) {
      if (locked.getParent().isPresent()) {
        if (locked.getParent().get().equals(entry)) {
          return true;
        }
      }
    }
    return false;
  }
}
