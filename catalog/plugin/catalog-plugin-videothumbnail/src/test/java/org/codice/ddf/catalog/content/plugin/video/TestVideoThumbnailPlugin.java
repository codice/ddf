/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.content.plugin.video;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.exec.ExecuteException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import ddf.catalog.Constants;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.plugin.PluginExecutionException;

public class TestVideoThumbnailPlugin {
    private String binaryPath;

    private BundleContext mockBundleContext;

    private VideoThumbnailPlugin videoThumbnailPlugin;

    private ContentItem mockContentItem;

    private static final String ID = UUID.randomUUID()
            .toString();

    private HashMap<String, Serializable> properties;

    @Before
    public void setUp() throws IOException, MimeTypeParseException, URISyntaxException {
        System.setProperty("ddf.home", SystemUtils.USER_DIR);

        binaryPath = FilenameUtils.concat(System.getProperty("ddf.home"), "bin_third_party");

        setUpMockBundleContext();

        videoThumbnailPlugin = new VideoThumbnailPlugin(mockBundleContext);
    }

    @After
    public void tearDown() {
        videoThumbnailPlugin.destroy();

        final File binaryFolder = new File(binaryPath);
        if (binaryFolder.exists() && !FileUtils.deleteQuietly(binaryFolder)) {
            binaryFolder.deleteOnExit();
        }
    }

    private void setUpMockBundleContext() {
        mockBundleContext = mock(BundleContext.class);

        final Bundle mockBundle = mock(Bundle.class);
        doReturn(mockBundle).when(mockBundleContext)
                .getBundle();

        String ffmpegResourcePath;
        URL ffmpegBinaryUrl;

        if (SystemUtils.IS_OS_LINUX) {
            ffmpegResourcePath = "linux/ffmpeg";
        } else if (SystemUtils.IS_OS_MAC) {
            ffmpegResourcePath = "osx/ffmpeg";
        } else if (SystemUtils.IS_OS_WINDOWS) {
            ffmpegResourcePath = "windows/ffmpeg.exe";
        } else if (SystemUtils.IS_OS_SOLARIS) {
            ffmpegResourcePath = "solaris/ffmpeg";
        } else {
            fail("Platform is not Linux, Mac, or Windows. No FFmpeg binaries are provided for this platform.");
            return;
        }

        ffmpegBinaryUrl = getClass().getClassLoader()
                .getResource(ffmpegResourcePath);

        doReturn(ffmpegBinaryUrl).when(mockBundle)
                .getEntry(ffmpegResourcePath);
    }

    private void setUpMockContentItem(final String resource)
            throws IOException, MimeTypeParseException, URISyntaxException {
        mockContentItem = mock(ContentItem.class);

        Metacard mockMetacard = new MetacardImpl();

        doReturn(mockMetacard).when(mockContentItem)
                .getMetacard();

        doReturn(ID).when(mockContentItem)
                .getId();

        doReturn(new MimeType("video/mp4")).when(mockContentItem)
                .getMimeType();

        HashMap<String, Path> contentPaths = new HashMap<>();
        Path tmpPath = Paths.get(getClass().getResource(resource)
                .toURI());
        contentPaths.put(ID, tmpPath);
        properties = new HashMap<>();
        properties.put(Constants.CONTENT_PATHS, contentPaths);
    }

    @Test
    public void testMediumCreatedItemGifThumbnail() throws Exception {
        // This file is short enough that the plugin won't try to grab thumbnails from different
        // portions of the video but is long enough that the resulting thumbnail will be a GIF.
        final byte[] thumbnail = getCreatedItemThumbnail("/medium.mp4");
        assertThat(thumbnail, notNullValue());
        verifyThumbnailIsGif(thumbnail);
    }

    @Test
    public void testShortCreatedItemStaticImageThumbnail() throws Exception {
        // This file is short enough that FFmpeg will only generate one thumbnail for it even if we
        // request more than one.
        final byte[] thumbnail = getCreatedItemThumbnail("/short.mp4");
        assertThat(thumbnail, notNullValue());
        verifyThumbnailIsPng(thumbnail);
    }

    @Test
    public void testLongCreatedItemGifThumbnail() throws Exception {
        // This file is long enough that the plugin will try to grab thumbnails from different
        // portions of the video.
        final byte[] thumbnail = getCreatedItemThumbnail("/long.mp4");
        assertThat(thumbnail, notNullValue());
        verifyThumbnailIsGif(thumbnail);
    }

    private byte[] getCreatedItemThumbnail(final String videoFile) throws Exception {
        setUpMockContentItem(videoFile);

        final CreateStorageResponse mockCreateResponse = mock(CreateStorageResponse.class);

        doReturn(Collections.singletonList(mockContentItem)).when(mockCreateResponse)
                .getCreatedContentItems();

        final CreateStorageRequest mockCreateRequest = mock(CreateStorageRequest.class);

        doReturn(mockCreateRequest).when(mockCreateResponse)
                .getRequest();

        doReturn(properties).when(mockCreateResponse)
                .getProperties();

        final CreateStorageResponse processedCreateResponse = videoThumbnailPlugin.process(
                mockCreateResponse);

        return (byte[]) processedCreateResponse.getCreatedContentItems()
                .get(0)
                .getMetacard()
                .getAttribute(Metacard.THUMBNAIL)
                .getValue();
    }

    private void verifyThumbnailIsGif(final byte[] thumbnail) {
        // Check the GIF header bytes.
        assertThat(thumbnail[0], is((byte) 0x47));
        assertThat(thumbnail[1], is((byte) 0x49));
        assertThat(thumbnail[2], is((byte) 0x46));
        assertThat(thumbnail[3], is((byte) 0x38));
        assertThat(thumbnail[4], is((byte) 0x39));
        assertThat(thumbnail[5], is((byte) 0x61));
    }

    private void verifyThumbnailIsPng(final byte[] thumbnail) {
        // Check the PNG header bytes.
        assertThat(thumbnail[0], is((byte) 0x89));
        assertThat(thumbnail[1], is((byte) 0x50));
        assertThat(thumbnail[2], is((byte) 0x4E));
        assertThat(thumbnail[3], is((byte) 0x47));
        assertThat(thumbnail[4], is((byte) 0x0D));
        assertThat(thumbnail[5], is((byte) 0x0A));
        assertThat(thumbnail[6], is((byte) 0x1A));
        assertThat(thumbnail[7], is((byte) 0x0A));
    }

    @Test
    public void testUpdatedItemGifThumbnail() throws Exception {
        setUpMockContentItem("/medium.mp4");

        final UpdateStorageResponse mockUpdateResponse = mock(UpdateStorageResponse.class);

        doReturn(Collections.singletonList(mockContentItem)).when(mockUpdateResponse)
                .getUpdatedContentItems();

        final UpdateStorageRequest mockUpdateRequest = mock(UpdateStorageRequest.class);

        doReturn(mockUpdateRequest).when(mockUpdateResponse)
                .getRequest();

        doReturn(properties).when(mockUpdateResponse)
                .getProperties();

        final UpdateStorageResponse processedUpdateResponse = videoThumbnailPlugin.process(
                mockUpdateResponse);

        final byte[] thumbnail = (byte[]) processedUpdateResponse.getUpdatedContentItems()
                .get(0)
                .getMetacard()
                .getAttribute(Metacard.THUMBNAIL)
                .getValue();
        assertThat(thumbnail, notNullValue());
        verifyThumbnailIsGif(thumbnail);
    }

    @Test
    public void testCreatedItemNotVideoFile() throws Exception {
        mockContentItem = mock(ContentItem.class);

        doReturn(new MimeType("image/jpeg")).when(mockContentItem)
                .getMimeType();

        Metacard mockMetacard = new MetacardImpl();

        doReturn(mockMetacard).when(mockContentItem)
                .getMetacard();

        final CreateStorageResponse mockCreateResponse = mock(CreateStorageResponse.class);

        doReturn(Collections.singletonList(mockContentItem)).when(mockCreateResponse)
                .getCreatedContentItems();

        final CreateStorageResponse processedCreateResponse = videoThumbnailPlugin.process(
                mockCreateResponse);

        assertThat(processedCreateResponse.getCreatedContentItems()
                .get(0)
                .getMetacard()
                .getAttribute(Metacard.THUMBNAIL), CoreMatchers.is(nullValue()));
    }

    @Test
    public void testUpdatedItemNotVideoFile() throws Exception {
        mockContentItem = mock(ContentItem.class);

        doReturn(new MimeType("application/pdf")).when(mockContentItem)
                .getMimeType();

        Metacard mockMetacard = new MetacardImpl();

        doReturn(mockMetacard).when(mockContentItem)
                .getMetacard();

        final UpdateStorageResponse mockUpdateResponse = mock(UpdateStorageResponse.class);

        doReturn(Collections.singletonList(mockContentItem)).when(mockUpdateResponse)
                .getUpdatedContentItems();

        final UpdateStorageResponse processedUpdateResponse = videoThumbnailPlugin.process(
                mockUpdateResponse);

        assertThat(processedUpdateResponse.getUpdatedContentItems()
                .get(0)
                .getMetacard()
                .getAttribute(Metacard.THUMBNAIL), CoreMatchers.is(nullValue()));
    }

    @Test
    public void testCorruptedVideo() {
        try {
            getCreatedItemThumbnail("/corrupted.mp4");
            fail("The video thumbnail plugin should have thrown an exception.");
        } catch (Exception e) {
            assertThat(e, instanceOf(PluginExecutionException.class));
            assertThat(e.getCause(), instanceOf(ExecuteException.class));
        }
    }
}
