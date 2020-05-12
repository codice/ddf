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

import static ddf.catalog.Constants.CDM_LOGGER_NAME;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
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
 * <p>Every time the AsyncFileAlterationObserver is polled by calling {@code checkAndNotify()}, if
 * there are no files currently being processed, the observer will check all the {@link File}'s and
 * {@link AsyncFileEntry}'s that are under the directory being monitored, and call the {@link
 * AsyncFileAlterationListener}'s corresponding methods
 *
 * <p>if there are files being processed or a thread already inside {@code checkAndNotify()}, check
 * and notify will immediately return false
 *
 * <p>Known Limitations:
 *
 * <ul>
 *   <li>if a file becomes a directory then it's contents will not be deleted from the catalog
 *   <li>if a directory becomes a file, then it's contents will not be created in the catalog
 * </ul>
 *
 * @see AsyncFileAlterationListener
 */
public class AsyncFileAlterationObserver {

  private static final Logger LOGGER = LoggerFactory.getLogger(CDM_LOGGER_NAME);

  private final AsyncFileEntry rootFile;
  private AsyncFileAlterationListener listener = null;
  private final Set<AsyncFileEntry> processing = ConcurrentHashMap.newKeySet();
  private final Object listenerLock = new Object();
  private final ObjectPersistentStore serializer;
  private final Object processingLock = new Object();

  private boolean isProcessing = false;

  public AsyncFileAlterationObserver(File fileToObserve, ObjectPersistentStore serializer) {
    if (fileToObserve == null || serializer == null) {
      throw new IllegalArgumentException("Arguments can not be null");
    }
    this.serializer = serializer;
    rootFile = new AsyncFileEntry(fileToObserve);

    Timer timer = new Timer();
    timer.scheduleAtFixedRate(new Processing(), 500, 5000);
  }

  private AsyncFileAlterationObserver(AsyncFileEntry entry, ObjectPersistentStore serializer) {
    if (entry == null) {
      throw new IllegalArgumentException("Arguments can not be null");
    }
    rootFile = entry;
    rootFile.initialize();
    this.serializer = serializer;
  }

  /**
   * @param observedFile
   * @param store
   * @return returns a AsyncFileAlterationObserver if there was one serialized by an {@link
   *     ObjectPersistentStore} Otherwise returns {@code null}
   */
  public static @Nullable AsyncFileAlterationObserver load(
      File observedFile, ObjectPersistentStore store) {
    if (observedFile == null || store == null) {
      throw new IllegalArgumentException("Arguments can not be null");
    }
    AsyncFileEntry temp = store.load(observedFile.getName(), AsyncFileEntry.class);
    if (temp == null) {
      return null;
    }
    return new AsyncFileAlterationObserver(temp, store);
  }

  /**
   * Initializes the object state of the Observer.
   *
   * @throws IllegalStateException when the observer fails to initialize and initialization should
   *     be retried
   */
  public void initialize() throws IllegalStateException {
    initChildEntries(rootFile);
    serializer.store(rootFile.getName(), rootFile);
  }

  public void destroy() {
    rootFile.destroy();
  }

  public void setListener(final AsyncFileAlterationListener listener) {
    synchronized (listenerLock) {
      this.listener = listener;
    }
  }

  public void removeListener() {
    synchronized (listenerLock) {
      this.listener = null;
    }
  }

  /**
   * Called when the observer should compare the snapshot state to the actual state of the directory
   * being monitored.
   */
  public boolean checkAndNotify() {

    AsyncFileAlterationListener listenerCopy;
    synchronized (processingLock) {
      if (!processing.isEmpty()) {
        LOGGER.debug(
            "{} files are still processing. Waiting until the list is empty", processing.size());
        return false;
      } else if (isProcessing) {
        LOGGER.debug("Another thread is currently running, returning until next poll");
        return false;
      }

      isProcessing = true;
      //  You cannot change listeners in the middle of executions.
      synchronized (listenerLock) {
        if (listener == null) {
          isProcessing = false;
          return false;
        }
        listenerCopy = listener;
      }
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

    synchronized (processingLock) {
      isProcessing = false;
    }
    return true;
  }

  @VisibleForTesting
  AsyncFileEntry getRootFile() {
    return rootFile;
  }

  /**
   * Fire directory/file created events to the registered listeners.
   *
   * @param entry The file entry
   */
  private void doCreate(AsyncFileEntry entry, final AsyncFileAlterationListener listenerCopy) {

    processing.add(entry);

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
  private void commitCreate(AsyncFileEntry entry, boolean success) {

    LOGGER.debug("commitCreate({},{}): Starting...", entry.getName(), success);
    if (success) {
      entry.commit();
      entry.getParent().ifPresent(e -> e.addChild(entry));
      LOGGER.debug(
          "File {} committed to {}",
          entry.getName(),
          entry.getParent().map(AsyncFileEntry::getName).orElse("parent"));
    } else {
      LOGGER.debug("Create task failed for {}", entry.getName());
    }
    onFinish(entry);
  }

  /**
   * Fire directory/file change events to the registered listeners.
   *
   * @param entry The previous file system entry
   */
  private void doMatch(AsyncFileEntry entry, final AsyncFileAlterationListener listenerCopy) {

    if (!entry.hasChanged()) {
      return;
    }

    processing.add(entry);

    LOGGER.trace("{} has changed", entry.getName());
    if (!entry.getFile().isDirectory()) {
      LOGGER.trace("Sending Match Request for {}...", entry.getName());
      listenerCopy.onFileChange(
          entry.getFile(), new CompletionSynchronization(entry, this::commitMatch));
    } else {
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
    if (success) {
      LOGGER.debug("commitMatch({},{}): Starting...", entry.getName(), success);
      entry.commit();
      LOGGER.debug("{} committed", entry.getName());
    } else {
      LOGGER.debug("Match task failed for {}", entry.getName());
    }
    onFinish(entry);
  }

  /**
   * Fire directory/file delete events to the registered listeners.
   *
   * @param entry The file entry
   */
  private void doDelete(AsyncFileEntry entry, final AsyncFileAlterationListener listenerCopy) {
    if (!entry.isDirectory()) {
      processing.add(entry);
      LOGGER.trace("Sending Delete Request for {}...", entry.getName());
      listenerCopy.onFileDelete(
          entry.getFile(), new CompletionSynchronization(entry, this::commitDelete));
    }
    //  Once there are no more children we can delete directories.
    //  Check that there are no children, and that no locked files have it as it's parent.
    else if (!entry.hasChildren()) {
      processing.add(entry);
      commitDelete(entry, true);
    }
    //  If there are still children, we're going to keep it within the tree until all the
    //  children are successfully deleted
  }

  /**
   * Callback to allow successful children to remove themselves from their parent directory and put
   * themselves in a state where they have never been committed.
   *
   * @param entry The AsyncFileEntry wrapping the file being listened to.
   * @param success Boolean that shows if the task failed or completed successfully
   */
  private void commitDelete(AsyncFileEntry entry, boolean success) {
    if (success) {
      LOGGER.debug("commitDelete({},{}): Starting...", entry.getName(), success);
      entry.getParent().ifPresent(e -> e.removeChild(entry));
      entry.destroy();
      LOGGER.debug(
          "{} was removed from {}",
          entry.getName(),
          entry.getParent().map(AsyncFileEntry::getName).orElse("parent"));
    } else {
      LOGGER.debug("Delete task failed for {}", entry.getName());
    }
    onFinish(entry);
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
          //  The file may still exist but it's the network that's down.
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

  private void onFinish(AsyncFileEntry entry) {
    synchronized (processingLock) {
      processing.remove(entry);
      if (processing.isEmpty()) {
        serializer.store(rootFile.getName(), rootFile);
        isProcessing = false;
      }
    }
  }

  private class Processing extends TimerTask {

    /** Log files still in processing at scheduled intervals */
    public void run() {
      if (LOGGER.isTraceEnabled() && !processing.isEmpty()) {
        String files =
            processing
                .stream()
                .map(AsyncFileEntry::getName)
                .collect(Collectors.toList())
                .toString();
        LOGGER.trace("{} files being processed: {}", processing.size(), files);
      }
    }
  }
}
