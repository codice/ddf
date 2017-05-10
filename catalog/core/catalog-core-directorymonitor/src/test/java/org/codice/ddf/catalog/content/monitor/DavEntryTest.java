/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.content.monitor;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;

@RunWith(JUnit4.class)
public class DavEntryTest {
    private DavEntry entry;

    private DavResource mockResource;

    private Sardine mockSardine;

    @Before
    public void setup() {
        entry = new DavEntry("http://test");
        mockResource = mock(DavResource.class);
        doReturn(false).when(mockResource)
                .isDirectory();
        doReturn(new Date()).when(mockResource)
                .getModified();
        doReturn("E/12345").when(mockResource)
                .getEtag();
        doReturn(42L).when(mockResource)
                .getContentLength();
        entry.refresh(mockResource);
        mockSardine = mock(Sardine.class);
    }

    @Test
    public void testIdempotency() {
        assertThat(entry.refresh(mockResource), is(false));
    }

    @Test
    public void testModified() {
        doReturn(new Date(mockResource.getModified()
                .getTime() + 1)).when(mockResource)
                .getModified();
        assertThat(entry.refresh(mockResource), is(true));
    }

    @Test
    public void testEtag() {
        doReturn("E/67890").when(mockResource)
                .getEtag();
        assertThat(entry.refresh(mockResource), is(true));
    }

    @Test
    public void testContentLength() {
        doReturn(24L).when(mockResource)
                .getContentLength();
        assertThat(entry.refresh(mockResource), is(true));
    }

    @Test
    public void testDirectory() {
        doReturn(true).when(mockResource)
                .isDirectory();
        assertThat(entry.refresh(mockResource), is(true));
    }

    @Test
    public void testChild() {
        assertThat(entry.getChildren(), is(DavEntry.getEmptyEntries()));
        DavEntry child = entry.newChildInstance("foo");
        entry.setChildren(child);
        DavEntry[] children = entry.getChildren();
        assertThat(children.length, is(1));
        assertThat(children[0], is(child));
        assertThat(child.getLocation(), is("http://test/foo"));
        assertThat(child.getParent(), is(entry));
        assertThat(entry.getLevel(), is(0));
        assertThat(child.getLevel(), is(1));

    }

    @Test
    public void testEscaping() {
        DavEntry child = entry.newChildInstance("/more shenanigans.txt");
        assertThat(child.getLocation(), is("http://test/more%20shenanigans.txt"));
    }

    @Test
    public void testLocation() {
        for (String parent : new String[] {"http://test", "http://test/"}) {
            entry.setLocation(parent);
            for (String child : new String[] {"http://test/foo", "/foo", "foo"}) {
                assertThat(DavEntry.getLocation(child, entry), is("http://test/foo"));
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullLocation() {
        new DavEntry(null);
    }

    @Test
    public void testFile() throws IOException {
        DavEntry child = entry.newChildInstance("more shenanigans.txt");
        doReturn(IOUtils.toInputStream("test", "UTF-8")).when(mockSardine)
                .get(child.getLocation());
        File file = child.getFile(mockSardine);
        assertThat(FileUtils.readFileToString(file, "UTF-8"), is("test"));
        assertThat(file.getName(), is("more shenanigans.txt"));
    }

    @Test
    public void testRemoteExists() throws IOException {
        doReturn(Collections.singletonList(mockResource)).when(mockSardine)
                .list(entry.getLocation());
        assertThat(entry.remoteExists(mockSardine), is(true));
    }

    @Test
    public void testRemoteNotExists() throws IOException {
        doThrow(new IOException()).when(mockSardine)
                .list(entry.getLocation());
        entry.remoteExists(mockSardine);
    }
}
