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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.camel.spi.Synchronization;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

@RunWith(JUnit4.class)
public class AsyncFileAlterationObserverTest {

  private static String dummyData = "The duck may swim on the lake...";

  private static String changedData = "the duck.";

  private static int delayBuf = 125;

  private Consumer<Runnable> doTestWrapper = Runnable::run;

  private final AtomicInteger timesToFail = new AtomicInteger(0);

  private int failures = 0;

  private void doTest(Synchronization cb) {
    synchronized (timesToFail) {
      if (timesToFail.intValue() != 0) {
        timesToFail.decrementAndGet();
        failures++;
        cb.onFailure(null);
      } else {
        cb.onComplete(null);
      }
    }
  }

  private File monitoredDirectory;
  private AsyncFileAlterationObserver observer;

  //  NESTED DIRECTORY VARS
  private File childDir;
  private File grandchildDir;
  private File[] childFiles;
  private File[] grandchildFiles;
  private File[] files;
  private File[] grandsiblingsFiles;

  private AsyncFileAlterationListener fileListener;

  private int totalSize;

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setup() throws IOException {

    fileListener = Mockito.mock(AsyncFileAlterationListener.class);
    monitoredDirectory = temporaryFolder.newFolder("inbox");
    observer = new AsyncFileAlterationObserver(monitoredDirectory);
    observer.addListener(fileListener);

    childDir = null;
    grandchildDir = null;
    childFiles = null;
    grandchildFiles = null;
    files = null;
    grandsiblingsFiles = null;
    totalSize = 0;

    init();
  }

  private void init() {
    //  Resets the mock count and fail count for simplicity in numbers.

    reset(fileListener);
    //  Mockito Stuff
    doAnswer(this::mockitoDoTest)
        .when(fileListener)
        .onFileCreate(any(File.class), any(Synchronization.class));

    doAnswer(this::mockitoDoTest)
        .when(fileListener)
        .onFileChange(any(File.class), any(Synchronization.class));

    doAnswer(this::mockitoDoTest)
        .when(fileListener)
        .onFileDelete(any(File.class), any(Synchronization.class));

    timesToFail.set(0);
    failures = 0;
  }

  private Object mockitoDoTest(InvocationOnMock e) {
    Object[] args = e.getArguments();
    doTestWrapper.accept(() -> doTest((Synchronization) args[1]));
    return null;
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullRoot() {
    observer = new AsyncFileAlterationObserver(null);
  }

  @Test(expected = IllegalStateException.class)
  public void testAddingTwoListeners() {
    observer.addListener(fileListener);
  }

  @Test(expected = IllegalStateException.class)
  public void testNoListener() {
    observer.removeListener();
    observer.removeListener();
  }

  @Test
  public void testNoListenerUpdate() throws Exception {
    initFiles(5, monitoredDirectory, "covert00");
    observer.removeListener();
    observer.checkAndNotify();

    assertThat(observer.getRootFile().getChildren().isEmpty(), is(true));
  }

  @Test
  public void testRemovalOfListenerDuringExecution() throws Exception {

    File[] files = initFiles(100, monitoredDirectory, "null00");

    CountDownLatch latch = new CountDownLatch(5);

    Thread[] threads = new Thread[5];
    for (int i = 0; i < threads.length - 1; i++) {
      threads[i] =
          new Thread(
              () -> {
                observer.checkAndNotify();
                latch.countDown();
              });
    }

    threads[4] = new Thread(observer::removeListener);

    for (Thread i : threads) {
      i.start();
    }

    latch.await(1000, TimeUnit.MILLISECONDS);

    verify(fileListener, times(files.length))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testInitialEmptyFile() {
    observer.checkAndNotify();
    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testFileCreation() throws Exception {

    File[] files = initFiles(1, monitoredDirectory, "file00");
    observer.checkAndNotify();
    verify(fileListener, times(files.length))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testCreationFailure() throws Exception {

    File[] files = initFiles(1, monitoredDirectory, "file00");
    timesToFail.set(2);

    observer.checkAndNotify();
    observer.checkAndNotify();

    observer.checkAndNotify();
    observer.checkAndNotify();

    verify(fileListener, times(files.length + failures))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testNoChanges() throws Exception {

    initFiles(1, monitoredDirectory, "file00");
    observer.checkAndNotify();
    init();

    for (int i = 0; i < 10; i++) {
      observer.checkAndNotify();
    }

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testOneChange() throws Exception {

    File[] files = initFiles(1, monitoredDirectory, "file00");
    observer.checkAndNotify();
    init();

    Stream.of(files).forEach(this::changeData);
    observer.checkAndNotify();

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length))
        .onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testOneChangeWithError() throws Exception {

    File[] files = initFiles(1, monitoredDirectory, "file00");
    observer.checkAndNotify();
    init();

    timesToFail.set(2);
    Stream.of(files).forEach(this::changeData);

    for (int i = 0; i < 4; i++) {
      observer.checkAndNotify();
    }

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length + failures))
        .onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testFileDelete() throws Exception {

    File[] files = initFiles(1, monitoredDirectory, "file00");
    observer.checkAndNotify();
    init();

    Stream.of(files).forEach(this::fileDelete);

    observer.checkAndNotify();

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length))
        .onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testIOErrorReadingFiles() throws Exception {

    //  Creation not tested.
    File[] files = initFiles(1, monitoredDirectory, "file00");
    observer.checkAndNotify();
    init();

    assertThat(monitoredDirectory.setReadable(false), is(true));

    Stream.of(files).forEach(this::changeData);

    observer.checkAndNotify();

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    Stream.of(files).forEach(this::fileDelete);

    observer.checkAndNotify();

    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    assertThat(monitoredDirectory.setReadable(true), is(true));

    observer.checkAndNotify();

    verify(fileListener, times(files.length))
        .onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testIOErrorReadingFilesNestedDirectory() throws Exception {

    //  Creation not tested.
    initNestedDirectory(5, 6, 2, 5);
    observer.checkAndNotify();
    init();

    assertThat(grandchildDir.setReadable(false), is(true));

    Stream.of(childFiles).forEach(this::changeData);
    Stream.of(grandchildFiles).forEach(this::changeData);
    Stream.of(grandsiblingsFiles).forEach(this::changeData);
    Stream.of(files).forEach(this::changeData);

    observer.checkAndNotify();

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(totalSize - grandchildFiles.length))
        .onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    assertThat(grandchildDir.setReadable(true), is(true));

    observer.checkAndNotify();

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(totalSize))
        .onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testFileDeleteWithError() throws Exception {

    File[] files = initFiles(1, monitoredDirectory, "file00");
    observer.checkAndNotify();
    init();

    Stream.of(files).forEach(this::fileDelete);
    timesToFail.set(3);
    for (int i = 0; i < 4; i++) {
      observer.checkAndNotify();
    }

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length + failures))
        .onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testCreateThreadSafety() throws Exception {

    File[] files = initFiles(1, monitoredDirectory, "file00");

    runThreads(observer::checkAndNotify, 10);

    verify(fileListener, times(files.length))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testModifyThreadSafety() throws Exception {

    File[] files = initFiles(1, monitoredDirectory, "file00");
    observer.checkAndNotify();
    init();

    Stream.of(files).forEach(this::changeData);

    runThreads(observer::checkAndNotify, 10);

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length))
        .onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testDeleteThreadSafety() throws Exception {

    File[] files = initFiles(1, monitoredDirectory, "file00");
    observer.checkAndNotify();
    init();

    Stream.of(files).forEach(this::fileDelete);

    runThreads(observer::checkAndNotify, 10);

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length))
        .onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testModifyAndDeleteWithDelays() throws Exception {
    int delay = 200;

    File[] files = initFiles(1, monitoredDirectory, "file00");
    observer.checkAndNotify();
    init();

    Stream.of(files).forEach(this::changeData);

    addDelay(delay);

    observer.checkAndNotify();

    Stream.of(files).forEach(this::fileDelete);
    //  Implementation Detail
    //  Because the last operation hasn't finished yet the file should not be deleted in the state.
    //  It will wait until the file has finished it's prior update to remove it.

    observer.checkAndNotify();
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
    Thread.sleep(delay + delayBuf);

    verify(fileListener, times(files.length))
        .onFileChange(any(File.class), any(Synchronization.class));

    removeDelay();

    observer.checkAndNotify();

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length))
        .onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length))
        .onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testCreateAndDeleteWithDelays() throws Exception {
    int delay = 200;

    File[] files = initFiles(1, monitoredDirectory, "file00");

    addDelay(delay);

    observer.checkAndNotify();

    Stream.of(files).forEach(this::fileDelete);
    //  Because the last operation hasn't finished yet the file should not be deleted in the state.
    //  It will wait until the file has finished it's prior update to remove it.

    observer.checkAndNotify();
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    Thread.sleep(delay + delayBuf);

    verify(fileListener, times(files.length))
        .onFileCreate(any(File.class), any(Synchronization.class));

    removeDelay();

    observer.checkAndNotify();

    verify(fileListener, times(files.length))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length))
        .onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testCreatesWithDelay() throws Exception {
    int delay = 200;

    File[] files = new File[10];

    addDelay(delay);

    for (int i = 0; i < 10; i++) {
      files[i] = new File(monitoredDirectory, "file00" + i);
      FileUtils.writeStringToFile(files[i], dummyData, Charset.defaultCharset());
      observer.checkAndNotify();
    }

    runThreads(observer::checkAndNotify, 3);

    Thread.sleep(delay + delayBuf);

    verify(fileListener, times(files.length))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testModifyWithDelay() throws Exception {
    int delay = 200;

    File[] files = initFiles(10, monitoredDirectory, "file00");

    observer.checkAndNotify();
    init();

    addDelay(delay);

    Stream.of(files)
        .forEach(
            f -> {
              changeData(f);
              observer.checkAndNotify();
            });

    runThreads(observer::checkAndNotify, 3);

    Thread.sleep(delay + delayBuf);

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length))
        .onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testDeleteWithDelay() throws Exception {
    int delay = 200;

    File[] files = initFiles(10, monitoredDirectory, "file00");

    observer.checkAndNotify();
    init();

    addDelay(delay);

    Stream.of(files)
        .forEach(
            f -> {
              fileDelete(f);
              observer.checkAndNotify();
            });

    runThreads(observer::checkAndNotify, 3);

    Thread.sleep(delay + delayBuf);

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length))
        .onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testCreateNestedDirectory() throws Exception {

    initNestedDirectory(4, 7, 5, 0);

    observer.checkAndNotify();

    verify(fileListener, times(totalSize))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testCreateNestedDirectoryWithErrors() throws Exception {

    File childDir = new File(monitoredDirectory, "child001");
    assertThat(childDir.mkdir(), is(true));

    File grandchildDir = new File(childDir, "grandchild001");
    assertThat(grandchildDir.mkdir(), is(true));

    File[] childFiles = initFiles(4, childDir, "child-file00");

    observer.checkAndNotify();

    File[] grandchildFiles = initFiles(3, grandchildDir, "grandchild-file00");

    timesToFail.set(grandchildFiles.length);

    observer.checkAndNotify();

    File[] files = initFiles(6, monitoredDirectory, "file00");

    timesToFail.set(files.length);

    for (int i = 0; i < 3; i++) {
      observer.checkAndNotify();
    }

    int totalNoFiles = childFiles.length + grandchildFiles.length + files.length + failures;

    observer.checkAndNotify();

    verify(fileListener, times(totalNoFiles))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testNestedDirectoryInit() throws Exception {
    //  Create the observer after the nested directory. Nothing should be added.
    initNestedDirectory(5, 3, 4, 2);

    observer = new AsyncFileAlterationObserver(monitoredDirectory);
    observer.addListener(fileListener);
    assertThat(observer.initialize(), is(true));
    observer.checkAndNotify();
    verifyNoMoreInteractions(fileListener);
  }

  @Test
  public void testNestedDirectoryCreateAndDestroy() throws Exception {
    //  Create the observer after the nested directory. Nothing should be added.
    initNestedDirectory(5, 3, 4, 2);
    init();
    observer.destroy();
    observer.checkAndNotify();
    verify(fileListener, times(totalSize)).onFileCreate(any(), any());
  }

  @Test
  public void testNestedDirectory() throws Exception {

    initNestedDirectory(4, 3, 6, 0);

    observer.checkAndNotify();

    init();

    FileUtils.writeStringToFile(childFiles[1], changedData, Charset.defaultCharset());
    FileUtils.writeStringToFile(childFiles[3], changedData, Charset.defaultCharset());
    FileUtils.writeStringToFile(files[1], changedData, Charset.defaultCharset());
    FileUtils.writeStringToFile(files[3], changedData, Charset.defaultCharset());
    FileUtils.writeStringToFile(grandchildFiles[1], changedData, Charset.defaultCharset());

    observer.checkAndNotify();

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(5)).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    init();

    Stream.of(grandchildFiles).forEach(this::fileDelete);
    fileDelete(grandchildDir);

    FileUtils.writeStringToFile(files[1], dummyData, Charset.defaultCharset());
    FileUtils.writeStringToFile(childFiles[3], dummyData, Charset.defaultCharset());
    FileUtils.writeStringToFile(
        new File(monitoredDirectory, "aBrandNewFile"), dummyData, Charset.defaultCharset());

    observer.checkAndNotify();

    verify(fileListener, times(1)).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(2)).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(grandchildFiles.length))
        .onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testNestedDirectoryWithDelays() throws Exception {
    int delay = 200;

    initNestedDirectory(4, 3, 6, 0);

    observer.checkAndNotify();

    init();

    addDelay(delay);

    FileUtils.writeStringToFile(childFiles[1], changedData, Charset.defaultCharset());
    FileUtils.writeStringToFile(childFiles[3], changedData, Charset.defaultCharset());

    runThreads(observer::checkAndNotify, 2);

    FileUtils.writeStringToFile(files[1], changedData, Charset.defaultCharset());
    FileUtils.writeStringToFile(files[3], changedData, Charset.defaultCharset());
    FileUtils.writeStringToFile(grandchildFiles[1], changedData, Charset.defaultCharset());

    runThreads(observer::checkAndNotify, 3);

    Thread.sleep(delay + delayBuf);

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(5)).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    init();

    Stream.of(grandchildFiles).forEach(this::fileDelete);
    fileDelete(grandchildDir);

    runThreads(observer::checkAndNotify, 2);

    FileUtils.writeStringToFile(files[1], dummyData, Charset.defaultCharset());
    FileUtils.writeStringToFile(childFiles[3], dummyData, Charset.defaultCharset());
    FileUtils.writeStringToFile(
        new File(monitoredDirectory, "aBrandNewFile"), dummyData, Charset.defaultCharset());

    runThreads(observer::checkAndNotify, 4);

    Thread.sleep(delay + delayBuf);

    verify(fileListener, times(1)).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(2)).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(grandchildFiles.length))
        .onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testMovingDirectoryWithDelays() throws Exception {
    int delay = 200;

    initNestedDirectory(7, 6, 9, 0);

    observer.checkAndNotify();

    init();
    addDelay(delay);

    File siblingDir = new File(monitoredDirectory, "child002");

    observer.checkAndNotify();

    Thread mover = new Thread(() -> assertThat(grandchildDir.renameTo(siblingDir), is(true)));
    mover.start();

    for (int i = 0; i < 4; i++) {
      runThreads(observer::checkAndNotify, 2);
      Thread.sleep(delay / 4);
    }
    observer.checkAndNotify();
    Thread.sleep(delay);

    verify(fileListener, times(grandchildFiles.length))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(grandchildFiles.length))
        .onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testMovingDirectoryThreadSafety() throws Exception {

    initNestedDirectory(5, 8, 9, 0);

    observer.checkAndNotify();

    init();

    File siblingDir = new File(monitoredDirectory, "child002");

    observer.checkAndNotify();

    Thread mover = new Thread(() -> assertThat(grandchildDir.renameTo(siblingDir), is(true)));
    mover.start();

    runThreads(observer::checkAndNotify, 10);

    verify(fileListener, times(grandchildFiles.length))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(grandchildFiles.length))
        .onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testMovingDirectoryWithErrorsAndDelays() throws Exception {
    int delay = 100;

    initNestedDirectory(7, 6, 9, 0);

    observer.checkAndNotify();

    init();
    addDelay(delay);

    File siblingDir = new File(monitoredDirectory, "child002");

    observer.checkAndNotify();

    timesToFail.set(10);

    Thread mover =
        new Thread(
            () -> {
              try {
                Thread.sleep(20);
              } catch (InterruptedException ignored) {
              }
              assertThat(grandchildDir.renameTo(siblingDir), is(true));
            });

    mover.start();

    for (int i = 0; i < 4; i++) {
      runThreads(observer::checkAndNotify, 2);
      Thread.sleep(delay / 4);
    }

    Thread.sleep(delayBuf);

    observer.checkAndNotify();

    Thread.sleep(delay + delayBuf);

    //  These are combined because we don't know what it will fail on. Thus we can only
    //  count the combined total.

    ArgumentCaptor<File> propertyKeyCaptor = ArgumentCaptor.forClass(File.class);
    Mockito.verify(fileListener, atLeast(0)).onFileCreate(propertyKeyCaptor.capture(), any());
    Mockito.verify(fileListener, atLeast(0)).onFileDelete(propertyKeyCaptor.capture(), any());

    assertThat(propertyKeyCaptor.getAllValues().size(), is(grandchildFiles.length * 2 + failures));

    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testCreateNestedDirectoryThreadSafety() throws Exception {

    initNestedDirectory(16, 12, 6, 11);

    runThreads(observer::checkAndNotify, 10);

    verify(fileListener, times(totalSize))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testDeleteNestedDirectoryThreadSafety() throws Exception {

    initNestedDirectory(16, 12, 6, 11);

    observer.checkAndNotify();
    init();

    Stream.of(grandchildFiles).forEach(this::fileDelete);
    Stream.of(grandsiblingsFiles).forEach(this::fileDelete);
    Stream.of(childFiles).forEach(this::fileDelete);
    Stream.of(files).forEach(this::fileDelete);

    fileDelete(grandchildDir);
    fileDelete(childDir);

    runThreads(observer::checkAndNotify, 10);

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(totalSize))
        .onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testCreateWithErrorsAndThreadSafety() throws Exception {

    File[] files = initFiles(9, monitoredDirectory, "file00");

    timesToFail.set(files.length);
    runThreads(observer::checkAndNotify, 10);

    //  Just in case there was a straggler who was unable to successfully finish
    Thread.sleep(delayBuf);

    runThreads(observer::checkAndNotify, 1);

    verify(fileListener, times(files.length + failures))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testDeleteWithErrorsAndThreadSafety() throws Exception {

    File[] files = initFiles(9, monitoredDirectory, "file00");

    observer.checkAndNotify();

    init();

    timesToFail.set(files.length);

    Stream.of(files).forEach(this::fileDelete);

    runThreads(observer::checkAndNotify, 10);

    Thread.sleep(delayBuf);

    //  Just in case there was a straggler who was unable to successfully finish
    runThreads(observer::checkAndNotify, 1);

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length + failures))
        .onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testModifyNestedDirectoryThreadSafety() throws Exception {

    initNestedDirectory(16, 12, 6, 11);

    observer.checkAndNotify();
    init();

    Stream.of(grandchildFiles).forEach(this::changeData);
    Stream.of(grandsiblingsFiles).forEach(this::changeData);
    Stream.of(childFiles).forEach(this::changeData);
    Stream.of(files).forEach(this::changeData);

    runThreads(observer::checkAndNotify, 10);

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(totalSize))
        .onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  //  This test is implementation specific but it feels like a good implementation to follow.
  //  If a create fails and then there is a delete request, it will not send a delete request as the
  //  file never actually succeeded.
  @Test
  public void testCreateAndDeleteWithFailures() throws Exception {

    File[] files = initFiles(9, monitoredDirectory, "file00");

    timesToFail.set(files.length);
    observer.checkAndNotify();

    Stream.of(files).forEach(this::fileDelete);

    observer.checkAndNotify();

    verify(fileListener, times(files.length))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
    //  Does the observer clean up it's state?
    assertThat(observer.getRootFile().getChildren().isEmpty(), is(true));
  }

  //  Test creations, changes, and deletes with a delay, 5 threads, and intermittent errors
  @Test
  public void contentMonitorTest() throws Exception {
    int delay = 80;
    int threads = 5;

    initNestedDirectory(16, 12, 6, 11);

    addDelay(delay);
    timesToFail.set(7);

    runThreads(observer::checkAndNotify, threads);

    Thread.sleep(delay + delayBuf);

    removeDelay();
    observer.checkAndNotify();

    verify(fileListener, times(totalSize + failures))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    init();
    addDelay(delay);
    timesToFail.set(files.length);

    Stream.of(files).forEach(this::changeData);

    runThreads(observer::checkAndNotify, threads);

    Thread.sleep(delay + delayBuf);

    removeDelay();
    observer.checkAndNotify();

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length + failures))
        .onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    init();
    addDelay(delay);

    Stream.of(grandchildFiles).forEach(this::changeData);

    runThreads(observer::checkAndNotify, threads);

    //  Implementation detail these should operate as a No-OP since the file will be "committed"
    // with these changes.
    Stream.of(grandchildFiles).forEach(f -> assertThat(f.setLastModified(0), is(true)));

    fileDelete(grandchildFiles[1]);
    fileDelete(grandchildFiles[2]);
    fileDelete(grandchildFiles[3]);
    fileDelete(grandchildFiles[5]);
    fileDelete(grandchildFiles[8]);

    runThreads(observer::checkAndNotify, threads);

    Thread.sleep(delay + delayBuf);

    //  Implementation detail. Since the modify code is still processing,
    //  deletes SHOULD not happen.
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    runThreads(observer::checkAndNotify, threads);

    Thread.sleep(delay + delayBuf);

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(grandchildFiles.length))
        .onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(5)).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testJsonSerial() throws Exception {
    initNestedDirectory(1, 1, 0, 0);
    observer.initialize();

    AsyncFileEntry ehh = observer.getRootFile();
    Gson gson = new GsonBuilder().serializeNulls().create();
    final String Json = gson.toJson(observer);

    AsyncFileAlterationObserver two = gson.fromJson(Json, AsyncFileAlterationObserver.class);
    two.onLoad();

    two.checkAndNotify();

    assertThat(two.getRootFile().getChildren().get(0).getParent().isPresent(), is(true));
  }

  private void initNestedDirectory(int child, int grand, int topLevel, int gSibling)
      throws Exception {
    childDir = new File(monitoredDirectory, "child001");
    assertThat(childDir.mkdir(), is(true));

    grandchildDir = new File(childDir, "grandchild001");
    assertThat(grandchildDir.mkdir(), is(true));

    childFiles = initFiles(child, childDir, "child-file00");
    grandchildFiles = initFiles(grand, grandchildDir, "grandchild-file00");
    files = initFiles(topLevel, monitoredDirectory, "file00");
    grandsiblingsFiles = initFiles(gSibling, childDir, "grandsiblings-00");

    totalSize = child + grand + topLevel + gSibling;
  }

  private void fileDelete(File f) {
    assertThat(f.delete(), is(true));
  }

  private void changeData(File f) {
    try {
      FileUtils.writeStringToFile(f, changedData, Charset.defaultCharset());
    } catch (IOException ignored) {
      // Clean streams can't have exceptions
    }
  }

  private File[] initFiles(int size, File parent, String suffix) throws IOException {
    File[] files = new File[size];

    for (int i = 0; i < files.length; i++) {
      files[i] = new File(parent, suffix + i);
      FileUtils.writeStringToFile(files[i], dummyData, Charset.defaultCharset());
    }

    return files;
  }

  private void runThreads(Runnable runnable, int numberOfThreads) throws Exception {
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    Thread[] threads = new Thread[numberOfThreads];
    for (int i = 0; i < threads.length; i++) {
      threads[i] =
          new Thread(
              () -> {
                runnable.run();
                latch.countDown();
              });
    }

    for (Thread i : threads) {
      i.start();
    }

    latch.await(1000, TimeUnit.MILLISECONDS);
  }

  private void addDelay(int delay) {
    doTestWrapper = (f -> this.delayFunc(f, delay));
  }

  private void removeDelay() {
    doTestWrapper = Runnable::run;
  }

  private void delayFunc(Runnable cb, int delay) {
    Thread thread =
        new Thread(
            () -> {
              try {
                Thread.sleep(delay);
              } catch (InterruptedException ignored) {
                //  We don't care if this gets interrupted
              }
              cb.run();
            });
    thread.start();
  }
}
