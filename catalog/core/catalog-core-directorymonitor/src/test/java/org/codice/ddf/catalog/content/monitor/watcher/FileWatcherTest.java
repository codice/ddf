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
package org.codice.ddf.catalog.content.monitor.watcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FileWatcherTest {

  @Test(expected = IllegalArgumentException.class)
  public void testNullWatchedFileThrowsException() {
    new FileWatcher(null, mock(Consumer.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullFileCallbackThrowsException() {
    new FileWatcher(mock(File.class), null);
  }

  @Test
  public void testFileStabilizes() {
    File mockFile = mock(File.class);
    when(mockFile.length()).thenReturn(10L).thenReturn(20L);
    Consumer<File> fileCallback = mock(Consumer.class);

    FileWatcher watcher = new FileWatcher(mockFile, fileCallback);

    assertThat(watcher.check(), is(false));
    assertThat(watcher.check(), is(false));
    assertThat(watcher.check(), is(true));
    verify(fileCallback, times(1)).accept(mockFile);
  }
}
