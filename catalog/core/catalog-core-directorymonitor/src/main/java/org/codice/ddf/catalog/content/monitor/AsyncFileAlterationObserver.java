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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import org.apache.commons.io.FileUtils;
import org.codice.ddf.catalog.content.monitor.synchronizations.CompletionSynchronization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AsyncFileAlterationObserver
 *
 * <p>Based upon {@link org.apache.commons.io.monitor.FileAlterationObserver}, except modified to
 * only update the observer state on successful async request
 *
 * <p>This implementation only works with one AsyncFileAlterationListener.
 *
 * <p>Every time the AsyncFileAlterationObserver is polled by calling {@code checkAndNotify()}, the
 * observer will check all the {@link File}'s and {@link AsyncFileEntry}'s that are under the
 * directory being monitored, and call the {@link AsyncFileAlterationListener}'s corresponding
 * methods
 *
 * @apiNote The AsyncFileAlterationListener is expected to either call the callback function passed
 *     with {@code null} or pass the callback function into an {@link org.apache.camel.Exchange}.
 * @apiNote If this is loaded from a file after the system has been shut down, the user
 *     <strong>MUST</strong> call {@code clearCache()} in order to clear the currently processing
 *     request.
 */
public class AsyncFileAlterationObserver implements Serializable {

  private final AsyncFileEntry rootFile;
  private AsyncFileAlterationListener listener = null;
  private final Set<AsyncFileEntry> processing = new ConcurrentSkipListSet<>();
  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncFileAlterationObserver.class);

  public AsyncFileAlterationObserver(File fileToObserve) {
    rootFile = new AsyncFileEntry(fileToObserve);
  }

  //  Only for testing
  protected AsyncFileEntry getRootFile() {
    return rootFile;
  }

  public void addListener(final AsyncFileAlterationListener listener) {
    if (this.listener != null) {
      throw new IllegalStateException("Cannot be two listeners");
    }
    this.listener = listener;
  }

  public void removeListener() {
    //  To be truly thread safe, this should block until processes is empty.
    if (listener != null) {
      this.listener = null;
    } else {
      throw new IllegalStateException("Cannot remove a listener from an empty list");
    }
  }

  /**
   * Fire directory/file created events to the registered listeners.
   *
   * @param entry The file entry
   */
  private void doCreate(AsyncFileEntry entry) {

    if (!addToProcessors(entry)) {
      return;
    }

    AsyncFileEntry temp = entry.getFromParent();

    if (temp != null) {
      //  Someone already committed us. so we're done.
      removeFromProcessors(entry);
      return;
    }

    if (!entry.getFile().isDirectory()) {

      LOGGER.trace("Sending create Request for {}", entry.getName());

      listener.onFileCreate(
          entry.getFile(), new CompletionSynchronization(entry, this::commitCreate));
    } else {
      // Directories are always committed and added to the parent IF they
      // don't already exist

      File[] children = listFiles(entry.getFile());
      for (File child : children) {
        doCreate(new AsyncFileEntry(entry, child));
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
    }
    removeFromProcessors(entry);
  }

  /**
   * Fire directory/file change events to the registered listeners.
   *
   * @param entry The previous file system entry
   */
  private void doMatch(AsyncFileEntry entry) {

    if (entry.hasChanged()) {
      LOGGER.trace("{} has changed", entry.getName());
      if (!entry.getFile().isDirectory()) {
        if (!addToProcessors(entry)) {
          return;
        }
        if (!entry.hasChanged()) {
          //  Another thread just committed this. No work is needed.
          removeFromProcessors(entry);
          return;
        }
        LOGGER.trace("Sending Match Request for {}...", entry.getName());
        listener.onFileChange(
            entry.getFile(), new CompletionSynchronization(entry, this::commitMatch));
      } else {
        entry.commit();
      }
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
    removeFromProcessors(entry);
  }

  /**
   * Fire directory/file delete events to the registered listeners.
   *
   * @param entry The file entry
   */
  private void doDelete(AsyncFileEntry entry) {

    if (!entry.getFile().isDirectory() && !entry.isDirectory()) {
      if (!addToProcessors(entry)) {
        return;
      }
      //  If we don't exist in the parent another thread beat us here.
      if (entry.getFromParent() == null) {
        removeFromProcessors(entry);
        return;
      }
      LOGGER.trace("Sending Delete Request for {}...", entry.getName());
      listener.onFileDelete(
          entry.getFile(), new CompletionSynchronization(entry, this::commitDelete));
    }
    //  Once there are no more children we can delete directories.
    else if (entry.getChildren().isEmpty()) {
      commitDelete(entry, true);
    }
  }

  /**
   * Callback to allow successful children to remove themselves from their parent directory and
   * putting themselves in a state where they have never been committed.
   *
   * @param entry The AsyncFileEntry wrapping the file being listened to.
   * @param success Boolean that shows if the task failed or completed successfully
   */
  private void commitDelete(AsyncFileEntry entry, boolean success) {
    LOGGER.debug("commitDelete({},{}): Starting...", entry.getName(), success);
    if (success) {
      entry.getParent().ifPresent(e -> e.removeChild(entry));
      entry.destroy();
    }
    removeFromProcessors(entry);
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
      final AsyncFileEntry parent, final List<AsyncFileEntry> previous, final File[] files) {
    int c = 0;
    for (final AsyncFileEntry entry : previous) {
      while (c < files.length && entry.compareToFile(files[c]) > 0) {
        doCreate(new AsyncFileEntry(parent, files[c]));
        c++;
      }
      if (c < files.length && entry.compareToFile(files[c]) == 0) {
        doMatch(entry);
        checkAndNotify(entry, entry.getChildren(), listFiles(files[c]));
        c++;
      } else {
        //  Do Delete
        if (!entry.checkNetwork()) {
          //  The file MAY still exist but it's the network that's down.
          return;
        }
        checkAndNotify(entry, entry.getChildren(), FileUtils.EMPTY_FILE_ARRAY);
        doDelete(entry);
      }
    }
    for (; c < files.length; c++) {
      doCreate(new AsyncFileEntry(parent, files[c]));
    }
  }

  /**
   * Called when the observer should compare the snapshot state to the actual state of the directory
   * being monitored.
   */
  public void checkAndNotify() {

    if (listener == null) {
      return;
    }

    /* fire onStart() */
    listener.onStart(this);

    /* fire directory/file events */
    final File root = rootFile.getFile();
    if (root.exists()) {
      checkAndNotify(rootFile, rootFile.getChildren(), listFiles(rootFile.getFile()));
    } else {
      //  The file doesn't exist.
      //  We assume this can't happen so this must be a network disconnect.
    }

    /* fire onStop() */
    listener.onStop(this);
  }

  /**
   * listFiles:
   *
   * <p>Note: returns a new Array to avoid sync access exceptions
   *
   * @return A new sorted File Array
   */
  private File[] listFiles(File file) {
    if (file.isDirectory()) {
      File[] temp = file.listFiles();
      Arrays.sort(temp);
      return temp;
    }
    return FileUtils.EMPTY_FILE_ARRAY;
  }

  /**
   * Processing:
   *
   * <p>A list of files that are currently being processed by the CDM
   *
   * <p>Holds all the synchronized tags within this file. Potentially should be another class?
   */

  //  SHOULD be used when the File Observer is loaded from a file.
  //  Since this happens at boot time, there will not be anything currently processing.
  public void clearCache() {
    synchronized (processing) {
      processing.clear();
    }
  }

  private void removeFromProcessors(AsyncFileEntry entry) {
    synchronized (processing) {
      if (!processing.remove(entry)) {
        LOGGER.info("Could not remove Entry: {} from list of processing items.", entry);
      }
    }
  }

  private boolean addToProcessors(AsyncFileEntry entry) {
    synchronized (processing) {
      if (processing.contains(entry)) {
        return false;
      }
      return processing.add(entry);
    }
  }
}
