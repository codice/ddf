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

public class DurableFileAlterationListenerTest {

  //  @Rule public TemporaryFolder folder = new TemporaryFolder();
  //
  //  AbstractDurableFileConsumer consumer;
  //
  //  ScheduledExecutorService executor;
  //
  //  DurableFileAlterationListener listener;
  //
  //  File testFile;
  //
  //  @Before
  //  public void setup() throws Exception {
  //    consumer = mock(AbstractDurableFileConsumer.class);
  //    executor = mock(ScheduledExecutorService.class);
  //    listener = new DurableFileAlterationListener(consumer, executor);
  //
  //    testFile = folder.newFile("testFile.txt");
  //  }
  //
  //  @Test
  //  public void testFileCreate() throws Exception {
  //    listener.onFileCreate(testFile);
  //    runCheck(1, false);
  //    verify(consumer).createExchangeHelper(testFile, StandardWatchEventKinds.ENTRY_CREATE);
  //  }
  //
  //  @Test
  //  public void testFileCreateSlowFileTransfer() throws Exception {
  //    listener.onFileCreate(testFile);
  //    runCheck(3, true);
  //    verify(consumer).createExchangeHelper(testFile, StandardWatchEventKinds.ENTRY_CREATE);
  //  }
  //
  //  @Test
  //  public void testFileUpdate() throws Exception {
  //    listener.onFileChange(testFile);
  //    runCheck(1, false);
  //    verify(consumer).createExchangeHelper(testFile, StandardWatchEventKinds.ENTRY_MODIFY);
  //  }
  //
  //  @Test
  //  public void testFileUpdateSlowFileTransfer() throws Exception {
  //    listener.onFileChange(testFile);
  //    runCheck(3, true);
  //    verify(consumer).createExchangeHelper(testFile, StandardWatchEventKinds.ENTRY_MODIFY);
  //  }
  //
  //  @Test
  //  public void testCustomPeriod() {
  //    System.setProperty(DurableFileAlterationListener.CDM_FILE_CHECK_PERIOD_PROPERTY, "8");
  //    executor = mock(ScheduledExecutorService.class);
  //    listener = new DurableFileAlterationListener(consumer, executor);
  //    verify(executor).scheduleAtFixedRate(any(), anyLong(), eq(8L), any());
  //  }
  //
  //  @Test
  //  public void testBadCustomPeriod() {
  //    System.setProperty(DurableFileAlterationListener.CDM_FILE_CHECK_PERIOD_PROPERTY,
  // "notanumber");
  //    executor = mock(ScheduledExecutorService.class);
  //    listener = new DurableFileAlterationListener(consumer, executor);
  //    verify(executor)
  //        .scheduleAtFixedRate(
  //            any(), anyLong(), eq(DurableFileAlterationListener.DEFAULT_PERIOD), any());
  //  }
  //
  //  @Test
  //  public void testNegativeCustomPeriod() {
  //    System.setProperty(DurableFileAlterationListener.CDM_FILE_CHECK_PERIOD_PROPERTY, "-1");
  //    executor = mock(ScheduledExecutorService.class);
  //    listener = new DurableFileAlterationListener(consumer, executor);
  //    verify(executor)
  //        .scheduleAtFixedRate(
  //            any(), anyLong(), eq(DurableFileAlterationListener.DEFAULT_PERIOD), any());
  //  }
  //
  //  private void runCheck(int times, boolean sizeChange) throws Exception {
  //    try (BufferedWriter writer = new BufferedWriter(new FileWriter(testFile))) {
  //      for (int i = 0; i < times; i++) {
  //        if (sizeChange) {
  //          writer.write("Test Data\n");
  //          writer.flush();
  //        }
  //        listener.checkFiles();
  //        verify(consumer, never()).createExchangeHelper(any(), any());
  //      }
  //      listener.checkFiles();
  //    }
  //  }
}
