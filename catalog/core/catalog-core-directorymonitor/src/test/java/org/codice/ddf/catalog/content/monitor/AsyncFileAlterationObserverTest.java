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
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
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

  private Consumer<Runnable> doTestWrapper = Runnable::run;

  private final AtomicInteger timesToFail = new AtomicInteger(0);

  private int failures = 0;

  private Semaphore artificialDelay;

  private CountDownLatch delayLatch;

  private static int timeout = 3000; //  3 seconds

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

  private ObjectPersistentStore store;

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private String json;

  private Object setJson(InvocationOnMock invocationOnMock) {
    Gson gson = new Gson();
    json = gson.toJson(invocationOnMock.getArguments()[1], AsyncFileEntry.class);
    return null;
  }

  private Object loadJson(InvocationOnMock invocationOnMock) {
    Gson gson = new Gson();
    return gson.fromJson(json, AsyncFileEntry.class);
  }

  @Before
  public void setup() throws IOException {

    store = Mockito.mock(ObjectPersistentStore.class);

    doAnswer(this::setJson).when(store).store(any(), any());

    doAnswer(this::loadJson).when(store).load(any(), any());

    fileListener = Mockito.mock(AsyncFileAlterationListener.class);
    monitoredDirectory = temporaryFolder.newFolder("inbox");
    observer = new AsyncFileAlterationObserver(monitoredDirectory, store);
    observer.setListener(fileListener);

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
    observer = new AsyncFileAlterationObserver(null, null);
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

    threads[4] =
        new Thread(
            () -> {
              observer.removeListener();
              latch.countDown();
            });

    for (Thread i : threads) {
      i.start();
    }

    latch.await(timeout, TimeUnit.MILLISECONDS);

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
    int toFail = 2;
    timesToFail.set(toFail);

    observer.checkAndNotify();
    observer.checkAndNotify();

    observer.checkAndNotify();
    observer.checkAndNotify();

    verify(fileListener, times(files.length + failures))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
    assertThat(failures, is(toFail));
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

    int toFail = 2;
    timesToFail.set(toFail);
    Stream.of(files).forEach(this::changeData);

    for (int i = 0; i < 4; i++) {
      observer.checkAndNotify();
    }

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length + failures))
        .onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
    assertThat(failures, is(toFail));
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
  public void testSerializationAfterCreate() throws Exception {
    initNestedDirectory(5, 7, 11, 2);

    observer.checkAndNotify();

    verify(store, times(1)).store(any(), any());
  }

  @Test
  public void testFileDeleteWithError() throws Exception {

    File[] files = initFiles(1, monitoredDirectory, "file00");
    observer.checkAndNotify();
    init();

    Stream.of(files).forEach(this::fileDelete);
    int toFail = 3;
    timesToFail.set(toFail);
    for (int i = 0; i < 4; i++) {
      observer.checkAndNotify();
    }

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length + failures))
        .onFileDelete(any(File.class), any(Synchronization.class));
    assertThat(failures, is(toFail));
  }

  @Test
  public void testThreadSafety() throws Exception {

    File[] files = initFiles(1, monitoredDirectory, "file00");

    delayLatch = new CountDownLatch(2);

    Thread one =
        new Thread(
            () -> {
              assertThat(observer.checkAndNotify(), is(true));
              delayLatch.countDown();
            });
    Thread two =
        new Thread(
            () -> {
              assertThat(observer.checkAndNotify(), is(false));
              delayLatch.countDown();
            });

    one.start();
    two.start();

    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    verify(fileListener, times(files.length))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testModifyAndDeleteWithDelays() throws Exception {

    File[] files = initFiles(1, monitoredDirectory, "file00");
    observer.checkAndNotify();
    init();

    Stream.of(files).forEach(this::changeData);

    initSemaphore(1);

    observer.checkAndNotify();

    Stream.of(files).forEach(this::fileDelete);
    //  Implementation Detail
    //  Because the last operation hasn't finished yet the file should not be deleted in the state.
    //  It will wait until the file has finished it's prior update to remove it.

    observer.checkAndNotify();
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    artificialDelay.release(files.length);
    delayLatch.await(timeout, TimeUnit.MILLISECONDS);
    initSemaphore(1);

    verify(fileListener, times(files.length))
        .onFileChange(any(File.class), any(Synchronization.class));

    observer.checkAndNotify();

    artificialDelay.release(files.length);
    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length))
        .onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length))
        .onFileDelete(any(File.class), any(Synchronization.class));

    assertThat(artificialDelay.getQueueLength(), is(0));
  }

  @Test
  public void testCreateAndDeleteWithDelays() throws Exception {

    File[] files = initFiles(1, monitoredDirectory, "file00");

    initSemaphore(files.length);

    observer.checkAndNotify();

    Stream.of(files).forEach(this::fileDelete);
    //  Because the last operation hasn't finished yet the file should not be deleted in the state.
    //  It will wait until the file has finished it's prior update to remove it.

    observer.checkAndNotify();
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    artificialDelay.release(files.length * 2);
    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    verify(fileListener, times(files.length))
        .onFileCreate(any(File.class), any(Synchronization.class));

    observer.checkAndNotify();

    verify(fileListener, times(files.length))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length))
        .onFileDelete(any(File.class), any(Synchronization.class));

    assertThat(artificialDelay.getQueueLength(), is(0));
  }

  @Test
  public void testNestedCreateAndDeleteWithDelays() throws Exception {

    File childDirectory = new File(monitoredDirectory, "child001");
    assertThat(childDirectory.mkdir(), is(true));

    File[] files = initFiles(1, childDirectory, "childFile00");

    initSemaphore(files.length);

    observer.checkAndNotify();

    Stream.of(files).forEach(this::fileDelete);
    fileDelete(childDirectory);

    observer.checkAndNotify();
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    artificialDelay.release(files.length * 2);
    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    verify(fileListener, times(files.length))
        .onFileCreate(any(File.class), any(Synchronization.class));

    observer.checkAndNotify();

    verify(fileListener, times(files.length))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length))
        .onFileDelete(any(File.class), any(Synchronization.class));

    assertThat(artificialDelay.getQueueLength(), is(0));
  }

  @Test
  public void testCreatesWithDelay() throws Exception {
    File[] files = new File[10];

    initSemaphore(files.length / 2);

    for (int i = 0; i < files.length / 2; i++) {
      files[i] = new File(monitoredDirectory, "file00" + i);
      FileUtils.writeStringToFile(files[i], dummyData, Charset.defaultCharset());
    }

    assertThat(observer.checkAndNotify(), is(true));

    for (int i = files.length / 2; i < files.length; i++) {
      files[i] = new File(monitoredDirectory, "file00" + i);
      FileUtils.writeStringToFile(files[i], dummyData, Charset.defaultCharset());
    }

    artificialDelay.release(files.length / 2);
    delayLatch.await(timeout, TimeUnit.MILLISECONDS);
    delayLatch = new CountDownLatch(files.length / 2);

    verify(fileListener, times(files.length / 2))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    assertThat(observer.checkAndNotify(), is(true));
    artificialDelay.release(files.length / 2);
    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    verify(fileListener, times(files.length))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    assertThat(observer.getRootFile().getChildren().size(), is(files.length));
    assertThat(artificialDelay.getQueueLength(), is(0));
  }

  @Test
  public void testModifyWithDelay() throws Exception {

    File[] files = initFiles(10, monitoredDirectory, "file00");

    observer.checkAndNotify();
    init();

    initSemaphore(files.length);

    //  New implementation will wait until the process completes
    delayLatch = new CountDownLatch(1);

    Stream.of(files)
        .forEach(
            f -> {
              changeData(f);
              observer.checkAndNotify();
            });

    artificialDelay.release(files.length);

    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(1)).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    delayLatch = new CountDownLatch(files.length - 1);

    observer.checkAndNotify();

    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length))
        .onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    assertThat(artificialDelay.getQueueLength(), is(0));
  }

  @Test
  public void testDeleteWithDelay() throws Exception {
    File[] files = initFiles(10, monitoredDirectory, "file00");

    observer.checkAndNotify();
    init();

    initSemaphore(files.length);

    //  New implementation will wait until the process completes
    delayLatch = new CountDownLatch(1);

    Stream.of(files)
        .forEach(
            f -> {
              fileDelete(f);
              observer.checkAndNotify();
            });

    artificialDelay.release(files.length);

    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(1)).onFileDelete(any(File.class), any(Synchronization.class));

    delayLatch = new CountDownLatch(files.length - 1);

    observer.checkAndNotify();

    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length))
        .onFileDelete(any(File.class), any(Synchronization.class));

    assertThat(artificialDelay.getQueueLength(), is(0));
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
    assertThat(failures, is(grandchildFiles.length + files.length));
  }

  @Test
  public void testNestedDirectoryInit() throws Exception {
    //  Create the observer after the nested directory. Nothing should be added.
    initNestedDirectory(5, 3, 4, 2);

    observer = new AsyncFileAlterationObserver(monitoredDirectory, store);
    observer.setListener(fileListener);
    observer.initialize();
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

    initNestedDirectory(4, 3, 6, 0);

    observer.checkAndNotify();

    init();

    initSemaphore(5 + 3 + grandchildFiles.length);

    delayLatch = new CountDownLatch(2);

    FileUtils.writeStringToFile(childFiles[1], changedData, Charset.defaultCharset());
    FileUtils.writeStringToFile(childFiles[3], changedData, Charset.defaultCharset());

    observer.checkAndNotify();

    FileUtils.writeStringToFile(files[1], changedData, Charset.defaultCharset());
    FileUtils.writeStringToFile(files[3], changedData, Charset.defaultCharset());
    FileUtils.writeStringToFile(grandchildFiles[1], changedData, Charset.defaultCharset());

    observer.checkAndNotify();

    artificialDelay.release(5);

    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    delayLatch = new CountDownLatch(3);

    observer.checkAndNotify();

    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(5)).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    init();

    delayLatch = new CountDownLatch(grandchildFiles.length);

    Stream.of(grandchildFiles).forEach(this::fileDelete);
    fileDelete(grandchildDir);

    observer.checkAndNotify();

    FileUtils.writeStringToFile(files[1], dummyData, Charset.defaultCharset());
    FileUtils.writeStringToFile(childFiles[3], dummyData, Charset.defaultCharset());
    FileUtils.writeStringToFile(
        new File(monitoredDirectory, "aBrandNewFile"), dummyData, Charset.defaultCharset());

    observer.checkAndNotify();

    artificialDelay.release(3 + grandchildFiles.length);

    delayLatch.await(timeout, TimeUnit.MILLISECONDS);
    delayLatch = new CountDownLatch(3);

    observer.checkAndNotify();

    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    verify(fileListener, times(1)).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(2)).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(grandchildFiles.length))
        .onFileDelete(any(File.class), any(Synchronization.class));

    assertThat(artificialDelay.getQueueLength(), is(0));
  }

  @Test
  public void testMovingDirectoryWithDelays() throws Exception {
    initNestedDirectory(7, 6, 9, 0);

    observer.checkAndNotify();

    init();
    initSemaphore(grandchildFiles.length * 2);

    File siblingDir = new File(monitoredDirectory, "child002");

    assertThat(grandchildDir.renameTo(siblingDir), is(true));

    assertThat(observer.checkAndNotify(), is(true));

    artificialDelay.release(totalSize);

    assertThat(observer.checkAndNotify(), is(false));

    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    verify(fileListener, times(grandchildFiles.length))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(grandchildFiles.length))
        .onFileDelete(any(File.class), any(Synchronization.class));

    assertThat(artificialDelay.getQueueLength(), is(0));
  }

  @Test
  public void testMovingDirectory() throws Exception {

    initNestedDirectory(5, 8, 9, 0);

    observer.checkAndNotify();

    init();

    File siblingDir = new File(monitoredDirectory, "child002");

    observer.checkAndNotify();

    assertThat(grandchildDir.renameTo(siblingDir), is(true));

    observer.checkAndNotify();

    verify(fileListener, times(grandchildFiles.length))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(grandchildFiles.length))
        .onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testMovingDirectoryWithErrorsAndDelays() throws Exception {
    initNestedDirectory(7, 6, 9, 0);

    observer.checkAndNotify();

    init();
    int toFail = 10;
    initSemaphore(toFail + grandchildFiles.length * 2);

    File siblingDir = new File(monitoredDirectory, "child002");

    observer.checkAndNotify();

    timesToFail.set(toFail);

    assertThat(grandchildDir.renameTo(siblingDir), is(true));

    artificialDelay.release(totalSize + toFail);

    observer.checkAndNotify();

    observer.checkAndNotify();

    //  These are combined because we don't know what it will fail on. Thus we can only
    //  count the combined total.

    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    ArgumentCaptor<File> propertyKeyCaptor = ArgumentCaptor.forClass(File.class);
    Mockito.verify(fileListener, atLeast(0)).onFileCreate(propertyKeyCaptor.capture(), any());
    Mockito.verify(fileListener, atLeast(0)).onFileDelete(propertyKeyCaptor.capture(), any());

    assertThat(propertyKeyCaptor.getAllValues().size(), is(grandchildFiles.length * 2 + failures));

    assertThat(failures, is(toFail));

    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));

    assertThat(artificialDelay.getQueueLength(), is(0));
  }

  @Test
  public void testDeleteNestedDirectory() throws Exception {

    initNestedDirectory(16, 12, 6, 11);

    observer.checkAndNotify();
    init();

    Stream.of(grandchildFiles).forEach(this::fileDelete);
    Stream.of(grandsiblingsFiles).forEach(this::fileDelete);
    Stream.of(childFiles).forEach(this::fileDelete);
    Stream.of(files).forEach(this::fileDelete);

    fileDelete(grandchildDir);
    fileDelete(childDir);

    observer.checkAndNotify();

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(totalSize))
        .onFileDelete(any(File.class), any(Synchronization.class));
  }

  @Test
  public void testCreateWithErrors() throws Exception {

    File[] files = initFiles(9, monitoredDirectory, "file00");

    timesToFail.set(files.length);
    observer.checkAndNotify();

    verify(fileListener, times(files.length))
        .onFileCreate(any(File.class), any(Synchronization.class));

    //  Just in case there was a straggler who was unable to successfully finish
    observer.checkAndNotify();

    verify(fileListener, times(files.length + failures))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
    assertThat(failures, is(files.length));
  }

  @Test
  public void testDeleteWithErrors() throws Exception {

    File[] files = initFiles(9, monitoredDirectory, "file00");

    observer.checkAndNotify();

    init();

    timesToFail.set(files.length);

    Stream.of(files).forEach(this::fileDelete);

    observer.checkAndNotify();

    verify(fileListener, times(files.length))
        .onFileDelete(any(File.class), any(Synchronization.class));

    //  Just in case there was a straggler who was unable to successfully finish
    observer.checkAndNotify();

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length + failures))
        .onFileDelete(any(File.class), any(Synchronization.class));
    assertThat(failures, is(files.length));
  }

  @Test
  public void testModifyNestedDirectory() throws Exception {

    initNestedDirectory(16, 12, 6, 11);

    observer.checkAndNotify();
    init();

    Stream.of(grandchildFiles).forEach(this::changeData);
    Stream.of(grandsiblingsFiles).forEach(this::changeData);
    Stream.of(childFiles).forEach(this::changeData);
    Stream.of(files).forEach(this::changeData);

    observer.checkAndNotify();

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
    assertThat(failures, is(files.length));
  }

  //  Test creations, changes, and deletes with a delay, 5 threads, and intermittent errors
  @Test
  public void contentMonitorTest() throws Exception {
    int threads = 5;

    initNestedDirectory(16, 12, 6, 11);

    int toFail = 7;
    initSemaphore(0);
    timesToFail.set(toFail);

    delayLatch = new CountDownLatch(totalSize);

    artificialDelay.release(totalSize);
    observer.checkAndNotify();
    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    delayLatch = new CountDownLatch(toFail);

    artificialDelay.release(toFail);
    observer.checkAndNotify();

    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    assertThat(failures, is(toFail));
    verify(fileListener, times(totalSize + failures))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    init();
    initSemaphore(files.length);
    timesToFail.set(files.length);

    artificialDelay.release(5);

    Stream.of(files).forEach(this::changeData);

    observer.checkAndNotify();

    artificialDelay.release((files.length * 2) - 5);
    delayLatch.await(timeout, TimeUnit.MILLISECONDS);
    delayLatch = new CountDownLatch(files.length);

    observer.checkAndNotify();
    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    assertThat(failures, is(files.length));
    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(files.length + failures))
        .onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));
    assertThat(failures, is(files.length));

    init();
    initSemaphore(grandchildFiles.length);

    Stream.of(grandchildFiles).forEach(this::changeData);

    observer.checkAndNotify();

    //  Implementation detail these should operate as a No-OP since the file will be "committed"
    // with these changes.
    Stream.of(grandchildFiles).forEach(f -> assertThat(f.setLastModified(0), is(true)));

    fileDelete(grandchildFiles[1]);
    fileDelete(grandchildFiles[2]);
    fileDelete(grandchildFiles[3]);
    fileDelete(grandchildFiles[5]);
    fileDelete(grandchildFiles[8]);

    observer.checkAndNotify();

    artificialDelay.release(grandchildFiles.length);

    //  Implementation detail. Since the modify code is still processing,
    //  deletes SHOULD not happen.
    verify(fileListener, never()).onFileDelete(any(File.class), any(Synchronization.class));

    delayLatch.await(timeout, TimeUnit.MILLISECONDS);
    observer.checkAndNotify();
    delayLatch = new CountDownLatch(5);

    artificialDelay.release(5);

    observer.checkAndNotify();

    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    verify(fileListener, never()).onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(grandchildFiles.length))
        .onFileChange(any(File.class), any(Synchronization.class));
    verify(fileListener, times(5)).onFileDelete(any(File.class), any(Synchronization.class));

    assertThat(artificialDelay.getQueueLength(), is(0));
  }

  //  Implementation heavy test
  @Test
  public void testCreateDirectoryDeleteDirAndRecreate() throws Exception {
    File childDir = new File(monitoredDirectory, "child001");
    assertThat(childDir.mkdir(), is(true));

    File[] childFiles = initFiles(4, childDir, "child-file00");

    observer.checkAndNotify();

    //  Set it up so 2 files are being processed and 2 files are available.
    initSemaphore(2);
    artificialDelay.release(2);
    int failNo = 2;
    timesToFail.set(failNo);

    Stream.of(childFiles).forEach(this::fileDelete);
    assertThat(childDir.delete(), is(true));
    observer.checkAndNotify();

    delayLatch.await(timeout, TimeUnit.MILLISECONDS);
    //  2 files will be waiting on being deleted an 2 files will be failed.
    //  This will give us a state where we're in the middle of processing
    verify(fileListener, times(childFiles.length))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, times(childFiles.length))
        .onFileDelete(any(File.class), any(Synchronization.class));
    assertThat(failures, is(failNo));

    delayLatch = new CountDownLatch(2);

    childFiles = initFiles(4, childDir, "child-file00");

    //  When we initialize these files, we're half way through checking, The files that are blocked
    //  from the first pass will still be blocked on the delete, the files that have not yet
    // realized
    //  that the files are getting deleted will not notice anything as
    //  they are back. Depending they may send update requests
    observer.checkAndNotify();

    verify(fileListener, times(childFiles.length))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, atMost(2)).onFileChange(any(), any());
    verify(fileListener, times(childFiles.length))
        .onFileDelete(any(File.class), any(Synchronization.class));
    assertThat(failures, is(failNo));

    artificialDelay.release(4);
    delayLatch.await(timeout, TimeUnit.MILLISECONDS);

    removeDelay();

    //  when we remove the delay and unblock, the two files waiting on deletes will delete and the
    // next pass will
    //  create the new files again.
    observer.checkAndNotify();

    verify(fileListener, times(childFiles.length + 2))
        .onFileCreate(any(File.class), any(Synchronization.class));
    verify(fileListener, atMost(2)).onFileChange(any(), any());
    verify(fileListener, times(failNo + 2))
        .onFileDelete(any(File.class), any(Synchronization.class));
    assertThat(failures, is(failNo));

    assertThat(artificialDelay.getQueueLength(), is(0));
  }

  @Test
  public void testJsonSerial() throws Exception {

    initNestedDirectory(1, 1, 0, 0);
    observer.initialize();

    AsyncFileAlterationObserver two = AsyncFileAlterationObserver.load(new File("File01"), store);

    two.checkAndNotify();

    assertThat(two.getRootFile().getChildren().get(0).getParent().isPresent(), is(true));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testloadNull() {
    AsyncFileAlterationObserver.load(new File("File"), null);
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

  private void initSemaphore(int latchNo) {
    doTestWrapper = this::delayFunc;
    artificialDelay = new Semaphore(0);
    delayLatch = new CountDownLatch(latchNo);
  }

  private void removeDelay() {
    doTestWrapper = Runnable::run;
  }

  private void delayFunc(Runnable cb) {
    Thread thread =
        new Thread(
            () -> {
              try {
                artificialDelay.acquire();
              } catch (InterruptedException ignored) {
                //  We don't care if this gets interrupted
              }
              cb.run();
              delayLatch.countDown();
            });
    thread.start();
  }
}
