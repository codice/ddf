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
package ddf.content.plugin.video;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsMapContaining.hasKey;
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
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.exec.ExecuteException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import ddf.catalog.data.Metacard;
import ddf.content.data.ContentItem;
import ddf.content.operation.CreateRequest;
import ddf.content.operation.CreateResponse;
import ddf.content.operation.Response;
import ddf.content.operation.UpdateRequest;
import ddf.content.operation.UpdateResponse;
import ddf.content.plugin.ContentPlugin;
import ddf.content.plugin.PluginExecutionException;

public class TestVideoThumbnailPlugin {
    private String binaryPath;

    private BundleContext mockBundleContext;

    private VideoThumbnailPlugin videoThumbnailPlugin;

    private ContentItem mockContentItem;

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

        doReturn(new MimeType("video/mp4")).when(mockContentItem)
                .getMimeType();

        doReturn(new File(getClass().getClassLoader()
                .getResource(resource)
                .toURI())).when(mockContentItem)
                .getFile();
    }

    @Test
    public void testMediumCreatedItemGifThumbnail() throws Exception {
        // This file is short enough that the plugin won't try to grab thumbnails from different
        // portions of the video but is long enough that the resulting thumbnail will be a GIF.
        final byte[] thumbnail = getCreatedItemThumbnail("medium.mp4");
        assertThat(thumbnail, notNullValue());
        verifyThumbnailIsGif(thumbnail);
    }

    @Test
    public void testShortCreatedItemStaticImageThumbnail() throws Exception {
        // This file is short enough that FFmpeg will only generate one thumbnail for it even if we
        // request more than one.
        final byte[] thumbnail = getCreatedItemThumbnail("short.mp4");
        assertThat(thumbnail, notNullValue());
        verifyThumbnailIsPng(thumbnail);
    }

    @Test
    public void testLongCreatedItemGifThumbnail() throws Exception {
        // This file is long enough that the plugin will try to grab thumbnails from different
        // portions of the video.
        final byte[] thumbnail = getCreatedItemThumbnail("long.mp4");
        assertThat(thumbnail, notNullValue());
        verifyThumbnailIsGif(thumbnail);
    }

    private byte[] getCreatedItemThumbnail(final String videoFile) throws Exception {
        setUpMockContentItem(videoFile);

        final CreateResponse mockCreateResponse = mock(CreateResponse.class);

        doReturn(mockContentItem).when(mockCreateResponse)
                .getCreatedContentItem();

        final CreateRequest mockCreateRequest = mock(CreateRequest.class);

        doReturn(mockCreateRequest).when(mockCreateResponse)
                .getRequest();

        final CreateResponse processedCreateResponse = videoThumbnailPlugin.process(
                mockCreateResponse);

        return (byte[]) getAttributeMapFromResponse(processedCreateResponse).get(Metacard.THUMBNAIL);
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
        setUpMockContentItem("medium.mp4");

        final UpdateResponse mockUpdateResponse = mock(UpdateResponse.class);

        doReturn(mockContentItem).when(mockUpdateResponse)
                .getUpdatedContentItem();

        final UpdateRequest mockUpdateRequest = mock(UpdateRequest.class);

        doReturn(mockUpdateRequest).when(mockUpdateResponse)
                .getRequest();

        final UpdateResponse processedUpdateResponse = videoThumbnailPlugin.process(
                mockUpdateResponse);

        final byte[] thumbnail = (byte[]) getAttributeMapFromResponse(processedUpdateResponse).get(
                Metacard.THUMBNAIL);
        assertThat(thumbnail, notNullValue());
        verifyThumbnailIsGif(thumbnail);
    }

    @Test
    public void testCreatedItemNotVideoFile() throws Exception {
        mockContentItem = mock(ContentItem.class);

        doReturn(new MimeType("image/jpeg")).when(mockContentItem)
                .getMimeType();

        final CreateResponse mockCreateResponse = mock(CreateResponse.class);

        doReturn(mockContentItem).when(mockCreateResponse)
                .getCreatedContentItem();

        final CreateResponse processedCreateResponse = videoThumbnailPlugin.process(
                mockCreateResponse);

        assertThat(getAttributeMapFromResponse(processedCreateResponse),
                not(hasKey(Metacard.THUMBNAIL)));
    }

    @Test
    public void testUpdatedItemNotVideoFile() throws Exception {
        mockContentItem = mock(ContentItem.class);

        doReturn(new MimeType("application/pdf")).when(mockContentItem)
                .getMimeType();

        final UpdateResponse mockUpdateResponse = mock(UpdateResponse.class);

        doReturn(mockContentItem).when(mockUpdateResponse)
                .getUpdatedContentItem();

        final UpdateResponse processedUpdateResponse = videoThumbnailPlugin.process(
                mockUpdateResponse);

        assertThat(getAttributeMapFromResponse(processedUpdateResponse),
                not(hasKey(Metacard.THUMBNAIL)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Serializable> getAttributeMapFromResponse(final Response response) {
        return (Map<String, Serializable>) response.getPropertyValue(ContentPlugin.STORAGE_PLUGIN_METACARD_ATTRIBUTES);
    }

    @Test
    public void testCorruptedVideo() {
        try {
            getCreatedItemThumbnail("corrupted.mp4");
            fail("The video thumbnail plugin should have thrown an exception.");
        } catch (Exception e) {
            assertThat(e, instanceOf(PluginExecutionException.class));
            assertThat(e.getCause(), instanceOf(ExecuteException.class));
        }
    }
}
