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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DavAlterationObserverTest {
  private DavEntry parent;

  private DavResource mockParent;

  private DavResource mockChild1;

  private DavResource mockChild3;

  private Sardine mockSardine;

  private DavAlterationObserver observer;

  private EntryAlterationListener mockListener;

  @Before
  public void setup() throws IOException {
    parent = new DavEntry("http://test");
    DavEntry child1 = parent.newChildInstance("child1");
    DavEntry child2 = parent.newChildInstance("child2");

    mockSardine = mock(Sardine.class);
    mockParent = mock(DavResource.class);
    mockChild1 = mock(DavResource.class);
    mockChild3 = mock(DavResource.class);

    doReturn("/test").when(mockParent).getName();
    doReturn(true).when(mockParent).isDirectory();
    doReturn(new Date()).when(mockParent).getModified();
    doReturn(0L).when(mockParent).getContentLength();
    doReturn("E/0001").when(mockParent).getEtag();

    doReturn("/child1").when(mockChild1).getName();
    doReturn(false).when(mockChild1).isDirectory();
    doReturn(new Date()).when(mockChild1).getModified();
    doReturn(42L).when(mockChild1).getContentLength();
    doReturn("E/0002").when(mockChild1).getEtag();

    doReturn("/child3").when(mockChild3).getName();
    doReturn(false).when(mockChild3).isDirectory();
    doReturn(new Date()).when(mockChild3).getModified();
    doReturn(43L).when(mockChild3).getContentLength();
    doReturn("E/0003").when(mockChild3).getEtag();

    doReturn(Arrays.asList(mockParent, mockChild1, mockChild3))
        .when(mockSardine)
        .list(parent.getLocation());
    doReturn(Collections.singletonList(mockChild1)).when(mockSardine).list(child1.getLocation());
    doReturn(Collections.singletonList(mockChild3)).when(mockSardine).list(child2.getLocation());

    observer = new DavAlterationObserver(parent);
    mockListener = mock(EntryAlterationListener.class);
    observer.initialize(mockSardine);
    observer.addListener(mockListener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullRoot() {
    new DavAlterationObserver(null);
  }

  @Test
  public void testInit() {
    observer.checkAndNotify(mockSardine);
    verifyNoMoreInteractions(mockListener);
  }

  @Test
  public void testInsertedCreate() throws IOException {
    DavResource mockChild2 = mock(DavResource.class);
    DavEntry child2 = parent.newChildInstance("/child2");
    doReturn("/child2").when(mockChild2).getName();
    doReturn(false).when(mockChild2).isDirectory();
    doReturn(new Date()).when(mockChild2).getModified();
    doReturn(17L).when(mockChild2).getContentLength();
    doReturn("E/0004").when(mockChild2).getEtag();
    doReturn(Arrays.asList(mockParent, mockChild1, mockChild2, mockChild3))
        .when(mockSardine)
        .list(parent.getLocation());
    doReturn(Collections.singletonList(mockChild2)).when(mockSardine).list(child2.getLocation());

    observer.checkAndNotify(mockSardine);
    verify(mockListener, only()).onFileCreate(any());
  }

  @Test
  public void testTrailingCreate() throws IOException {
    DavResource mockChild4 = mock(DavResource.class);
    DavEntry child4 = parent.newChildInstance("/child4");
    doReturn("/child4").when(mockChild4).getName();
    doReturn(false).when(mockChild4).isDirectory();
    doReturn(new Date()).when(mockChild4).getModified();
    doReturn(17L).when(mockChild4).getContentLength();
    doReturn("E/0005").when(mockChild4).getEtag();
    doReturn(Arrays.asList(mockParent, mockChild1, mockChild4, mockChild3))
        .when(mockSardine)
        .list(parent.getLocation());
    doReturn(Collections.singletonList(mockChild4)).when(mockSardine).list(child4.getLocation());

    observer.checkAndNotify(mockSardine);
    verify(mockListener, only()).onFileCreate(any());
  }

  @Test
  public void testLeadingCreate() throws IOException {
    DavResource mockChild0 = mock(DavResource.class);
    DavEntry child0 = parent.newChildInstance("/child0");
    doReturn("/child0").when(mockChild0).getName();
    doReturn(false).when(mockChild0).isDirectory();
    doReturn(new Date()).when(mockChild0).getModified();
    doReturn(12345L).when(mockChild0).getContentLength();
    doReturn("E/0011").when(mockChild0).getEtag();
    doReturn(Arrays.asList(mockParent, mockChild1, mockChild0, mockChild3))
        .when(mockSardine)
        .list(parent.getLocation());
    doReturn(Collections.singletonList(mockChild0)).when(mockSardine).list(child0.getLocation());

    observer.checkAndNotify(mockSardine);
    verify(mockListener, only()).onFileCreate(any());
  }

  @Test
  public void testModify() {
    doReturn("E/00006").when(mockChild1).getEtag();
    observer.checkAndNotify(mockSardine);
    verify(mockListener, only()).onFileChange(any());
  }

  @Test
  public void testRename() {
    doReturn("/child7").when(mockChild1).getName();
    observer.checkAndNotify(mockSardine);
    verify(mockListener, times(1)).onFileCreate(any());
    verify(mockListener, times(1)).onFileDelete(any());
    verifyNoMoreInteractions(mockListener);
  }

  @Test
  public void testDelete() throws IOException {
    doReturn(Arrays.asList(mockParent, mockChild3)).when(mockSardine).list(parent.getLocation());
    observer.checkAndNotify(mockSardine);
    verify(mockListener, only()).onFileDelete(any());

    doThrow(new IOException()).when(mockSardine).list(parent.getLocation());
    observer.checkAndNotify(mockSardine);
    verify(mockListener, times(2)).onFileDelete(any());
    verifyNoMoreInteractions(mockListener);
  }

  @Test
  public void testDirectory() throws IOException {
    DavEntry dir = parent.newChildInstance("dir");
    DavResource mockDir = mock(DavResource.class);
    doReturn("/dir").when(mockDir).getName();
    doReturn(true).when(mockDir).isDirectory();
    doReturn(new Date()).when(mockDir).getModified();
    doReturn(0L).when(mockDir).getContentLength();
    doReturn("E/0008").when(mockDir).getEtag();

    doReturn(Arrays.asList(mockParent, mockChild1, mockChild3, mockDir))
        .when(mockSardine)
        .list(parent.getLocation());
    observer.checkAndNotify(mockSardine);
    verify(mockListener, times(1)).onDirectoryCreate(any());

    doReturn("E/0009").when(mockDir).getEtag();
    observer.checkAndNotify(mockSardine);
    verify(mockListener, times(1)).onDirectoryChange(any());

    doReturn(Arrays.asList(mockParent, mockChild1, mockChild3))
        .when(mockSardine)
        .list(parent.getLocation());
    observer.checkAndNotify(mockSardine);
    verify(mockListener, times(1)).onDirectoryDelete(any());
  }
}
