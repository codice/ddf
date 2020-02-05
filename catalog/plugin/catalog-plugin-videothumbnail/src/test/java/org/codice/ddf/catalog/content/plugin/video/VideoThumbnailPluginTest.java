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
package org.codice.ddf.catalog.content.plugin.video;

import static org.codice.ddf.catalog.content.plugin.video.VideoThumbnailPlugin.DEFAULT_MAX_FILE_SIZE_MB;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import ddf.catalog.Constants;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.data.impl.MetacardImpl;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class VideoThumbnailPluginTest {

  private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

  private String binaryPath;

  private VideoThumbnailPlugin videoThumbnailPlugin;

  private HashMap<String, Map> tmpContentPaths;

  @BeforeClass
  public static void setUpClass() {
    Assume.assumeFalse("Skip unit tests on Windows. See DDF-3503.", SystemUtils.IS_OS_WINDOWS);
  }

  @Before
  public void setUp() throws IOException, MimeTypeParseException, URISyntaxException {
    System.setProperty("ddf.home", SystemUtils.USER_DIR);

    binaryPath = FilenameUtils.concat(System.getProperty("ddf.home"), "bin_third_party");

    videoThumbnailPlugin = new VideoThumbnailPlugin(createMockBundleContext());
    tmpContentPaths = new HashMap<>();
  }

  @After
  public void tearDown() {
    videoThumbnailPlugin.destroy();

    final File binaryFolder = new File(binaryPath);
    if (binaryFolder.exists() && !FileUtils.deleteQuietly(binaryFolder)) {
      binaryFolder.deleteOnExit();
    }
  }

  /**
   * Tests processing short.mp4. This file is short enough that FFmpeg will only generate one
   * thumbnail for it even if we request more than one.
   */
  @Test
  public void testProcessShortVideo() throws Exception {
    // given
    final ContentItem mockContentItem = createMockVideoContentItemFromResource("/short.mp4");

    // when
    final CreateStorageResponse processedCreateResponse =
        videoThumbnailPlugin.process(createMockCreateStorageResponse(mockContentItem));

    // then
    final List<ContentItem> processedContentItems =
        processedCreateResponse.getCreatedContentItems();
    assertThat(
        "There should be exactly 1 returned content item", processedContentItems, hasSize(1));
    verifyThumbnailIsPng(mockContentItem, processedContentItems.get(0));
  }

  /**
   * Tests processing medium.mp4. This file is short enough that the plugin won't try to grab
   * thumbnails from different portions of the video but is long enough that the resulting thumbnail
   * will be a GIF.
   */
  @Test
  public void testProcessMediumVideo() throws Exception {
    // given
    final ContentItem mockContentItem = createMockVideoContentItemFromResource("/medium.mp4");

    // when
    final CreateStorageResponse processedCreateResponse =
        videoThumbnailPlugin.process(createMockCreateStorageResponse(mockContentItem));

    // then
    final List<ContentItem> processedContentItems =
        processedCreateResponse.getCreatedContentItems();
    assertThat(
        "There should be exactly 1 returned content item", processedContentItems, hasSize(1));
    verifyThumbnailIsGif(mockContentItem, processedContentItems.get(0));
  }

  /**
   * Tests processing long.mp4. This file is long enough that the plugin will try to grab thumbnails
   * from different portions of the video.
   */
  @Test
  public void testProcessLongVideo() throws Exception {
    // given
    final ContentItem mockContentItem = createMockVideoContentItemFromResource("/long.mp4");

    // when
    final CreateStorageResponse processedCreateResponse =
        videoThumbnailPlugin.process(createMockCreateStorageResponse(mockContentItem));

    // then
    final List<ContentItem> processedContentItems =
        processedCreateResponse.getCreatedContentItems();
    ContentItem processedItem = processedContentItems.get(0);
    assertThat(
        "There should be exactly 1 returned content item", processedContentItems, hasSize(1));
    verifyThumbnailIsGif(mockContentItem, processedContentItems.get(0));
  }

  @Test
  public void testProcessNotVideoFile() throws Exception {
    // given
    final ContentItem mockContentItem = createMockContentItemOfMimeType("image/jpeg");

    // when
    final CreateStorageResponse processedCreateResponse =
        videoThumbnailPlugin.process(createMockCreateStorageResponse(mockContentItem));

    // then
    final List<ContentItem> processedContentItems =
        processedCreateResponse.getCreatedContentItems();
    assertThat(
        "There should be exactly 1 returned content item", processedContentItems, hasSize(1));
    verifyThumbnailIsNotSet(mockContentItem, processedContentItems.get(0));
  }

  @Test
  public void testProcessCorruptedVideo() throws Exception {
    // given
    final ContentItem mockContentItem = createMockVideoContentItemFromResource("/corrupted.mp4");

    // when
    final CreateStorageResponse processedCreateResponse =
        videoThumbnailPlugin.process(createMockCreateStorageResponse(mockContentItem));

    // then
    final List<ContentItem> processedContentItems =
        processedCreateResponse.getCreatedContentItems();
    assertThat(
        "There should be exactly 1 returned content item", processedContentItems, hasSize(1));
    verifyThumbnailIsNotSet(mockContentItem, processedContentItems.get(0));
  }

  @Test
  public void testProcessVideoLargerThanDefaultMaxFileSize() throws Exception {
    // given
    final ContentItem mockContentItem =
        createMockVideoContentItemWithSizeBytes(DEFAULT_MAX_FILE_SIZE_MB * BYTES_PER_MEGABYTE + 1);

    // when
    final CreateStorageResponse processedCreateResponse =
        videoThumbnailPlugin.process(createMockCreateStorageResponse(mockContentItem));

    // then
    final List<ContentItem> processedContentItems =
        processedCreateResponse.getCreatedContentItems();
    assertThat(
        "There should be exactly 1 returned content item", processedContentItems, hasSize(1));
    verifyThumbnailIsNotSet(mockContentItem, processedContentItems.get(0));
  }

  @Test
  public void testProcessVideoLargerThanConfiguredMaxFileSize() throws Exception {
    // given
    final int maxFileSizeMB = 5;
    videoThumbnailPlugin.setMaxFileSizeMB(maxFileSizeMB);
    final ContentItem mockContentItem =
        createMockVideoContentItemWithSizeBytes(maxFileSizeMB * BYTES_PER_MEGABYTE + 1);

    // when
    final CreateStorageResponse processedCreateResponse =
        videoThumbnailPlugin.process(createMockCreateStorageResponse(mockContentItem));

    // then
    final List<ContentItem> processedContentItems =
        processedCreateResponse.getCreatedContentItems();
    assertThat(
        "There should be exactly 1 returned content item", processedContentItems, hasSize(1));
    verifyThumbnailIsNotSet(mockContentItem, processedContentItems.get(0));
  }

  @Test
  public void testProcessVideoWhenMaxFileSizeIs0() throws Exception {
    // given
    videoThumbnailPlugin.setMaxFileSizeMB(0);

    final ContentItem mockContentItem = createMockVideoContentItemFromResource("/short.mp4");

    // when
    final CreateStorageResponse processedCreateResponse =
        videoThumbnailPlugin.process(createMockCreateStorageResponse(mockContentItem));

    // then
    final List<ContentItem> processedContentItems =
        processedCreateResponse.getCreatedContentItems();
    assertThat(
        "There should be exactly 1 returned content item", processedContentItems, hasSize(1));
    verifyThumbnailIsNotSet(mockContentItem, processedContentItems.get(0));
  }

  @Test
  public void testProcessVideoSmallerThanConfiguredMaxFileSize() throws Exception {
    // given
    videoThumbnailPlugin.setMaxFileSizeMB(1);

    final ContentItem mockContentItem = createMockVideoContentItemFromResource("/long.mp4");

    // when
    final CreateStorageResponse processedCreateResponse =
        videoThumbnailPlugin.process(createMockCreateStorageResponse(mockContentItem));

    // then
    final List<ContentItem> processedContentItems =
        processedCreateResponse.getCreatedContentItems();
    assertThat(
        "There should be exactly 1 returned content item", processedContentItems, hasSize(1));
    verifyThumbnailIsGif(mockContentItem, processedContentItems.get(0));
  }

  @Test
  public void testProcessVideoWhenErrorRetrievingContentItemSize() throws Exception {
    // given
    final ContentItem mockContentItem = createMockVideoContentItem();
    doThrow(new IOException()).when(mockContentItem).getSize();

    // when
    final CreateStorageResponse processedCreateResponse =
        videoThumbnailPlugin.process(createMockCreateStorageResponse(mockContentItem));

    // then
    final List<ContentItem> processedContentItems =
        processedCreateResponse.getCreatedContentItems();
    assertThat(
        "There should be exactly 1 returned content item", processedContentItems, hasSize(1));
    verifyThumbnailIsNotSet(mockContentItem, processedContentItems.get(0));
  }

  /**
   * Tests processing a mix of {@link ContentItem}s where some get thumbnails. Also tests that
   * processing an {@link UpdateStorageResponse} works for different edge cases.
   */
  @Test
  public void testProcessMixedContentItems() throws Exception {
    // given
    final ContentItem mediumVideoMockContentItem =
        createMockVideoContentItemFromResource("/medium.mp4");
    final ContentItem throwsIoExceptionVideoMockContentItem = createMockVideoContentItem();
    doThrow(new IOException()).when(throwsIoExceptionVideoMockContentItem).getSize();
    final ContentItem longVideoMockContentItem =
        createMockVideoContentItemFromResource("/long.mp4");
    final ContentItem corruptedVideoMockContentItem =
        createMockVideoContentItemFromResource("/corrupted.mp4");
    final ContentItem videoLargerThanDefaultMaxFileSizeMockContentItem =
        createMockVideoContentItemWithSizeBytes(DEFAULT_MAX_FILE_SIZE_MB * BYTES_PER_MEGABYTE + 1);
    final ContentItem shortVideoMockContentItem =
        createMockVideoContentItemFromResource("/short.mp4");
    final ContentItem notVideoMockContentItem = createMockContentItemOfMimeType("image/jpeg");

    final UpdateStorageResponse updateStorageResponse =
        createMockUpdateStorageResponse(
            mediumVideoMockContentItem,
            throwsIoExceptionVideoMockContentItem,
            longVideoMockContentItem,
            corruptedVideoMockContentItem,
            videoLargerThanDefaultMaxFileSizeMockContentItem,
            shortVideoMockContentItem,
            notVideoMockContentItem);

    // when
    final UpdateStorageResponse processedUpdateResponse =
        videoThumbnailPlugin.process(updateStorageResponse);

    // then
    final List<ContentItem> processedContentItems =
        processedUpdateResponse.getUpdatedContentItems();
    assertThat(
        "There should be exactly 7 returned content items", processedContentItems, hasSize(7));

    verifyThumbnailIsGif(mediumVideoMockContentItem, processedContentItems.get(0));
    verifyThumbnailIsNotSet(throwsIoExceptionVideoMockContentItem, processedContentItems.get(1));
    verifyThumbnailIsGif(longVideoMockContentItem, processedContentItems.get(2));
    verifyThumbnailIsNotSet(corruptedVideoMockContentItem, processedContentItems.get(3));
    verifyThumbnailIsNotSet(
        videoLargerThanDefaultMaxFileSizeMockContentItem, processedContentItems.get(4));
    verifyThumbnailIsPng(shortVideoMockContentItem, processedContentItems.get(5));
    verifyThumbnailIsNotSet(notVideoMockContentItem, processedContentItems.get(6));
  }

  /** create mock methods */
  private BundleContext createMockBundleContext() {
    final BundleContext mockBundleContext = mock(BundleContext.class);

    final Bundle mockBundle = mock(Bundle.class);
    doReturn(mockBundle).when(mockBundleContext).getBundle();

    String ffmpegResourcePath;
    URL ffmpegBinaryUrl;

    if (SystemUtils.IS_OS_LINUX) {
      ffmpegResourcePath = "linux/ffmpeg-4.0";
    } else if (SystemUtils.IS_OS_MAC) {
      ffmpegResourcePath = "osx/ffmpeg-4.2.2";
      //      Skip unit tests on Windows. See DDF-3503.
      //    } else if (SystemUtils.IS_OS_WINDOWS) {
      //      ffmpegResourcePath = "windows/ffmpeg.exe";
    } else {
      fail(
          "Platform is not Linux, Mac, or Windows. No FFmpeg binaries are provided for this platform.");
      return null;
    }

    ffmpegBinaryUrl = getClass().getClassLoader().getResource(ffmpegResourcePath);

    doReturn(ffmpegBinaryUrl).when(mockBundle).getEntry(ffmpegResourcePath);

    return mockBundleContext;
  }

  private ContentItem createMockContentItemOfMimeType(String mimeType)
      throws MimeTypeParseException {
    final ContentItem mockContentItem = mock(ContentItem.class);
    doReturn(new MimeType(mimeType)).when(mockContentItem).getMimeType();
    doReturn(new MetacardImpl()).when(mockContentItem).getMetacard();
    doReturn(UUID.randomUUID().toString()).when(mockContentItem).getId();
    return mockContentItem;
  }

  private ContentItem createMockVideoContentItem() throws Exception {
    return createMockContentItemOfMimeType("video/mp4");
  }

  private ContentItem createMockVideoContentItemWithSizeBytes(long sizeBytes) throws Exception {
    final ContentItem mockContentItem = createMockVideoContentItem();
    doReturn(sizeBytes).when(mockContentItem).getSize();
    return mockContentItem;
  }

  private ContentItem createMockVideoContentItemFromResource(final String resource)
      throws Exception {
    final Path tmpPath = Paths.get(getClass().getResource(resource).toURI());

    final ContentItem mockContentItem =
        createMockVideoContentItemWithSizeBytes(Files.size(tmpPath));

    HashMap<String, Path> contentItemPaths = new HashMap<>();
    contentItemPaths.put(null, tmpPath);
    tmpContentPaths.put(mockContentItem.getId(), contentItemPaths);

    return mockContentItem;
  }

  private CreateStorageResponse createMockCreateStorageResponse(ContentItem contentItem) {
    final CreateStorageResponse mockCreateResponse = mock(CreateStorageResponse.class);
    doReturn(Collections.singletonList(contentItem))
        .when(mockCreateResponse)
        .getCreatedContentItems();
    final CreateStorageRequest mockCreateRequest = mock(CreateStorageRequest.class);
    doReturn(mockCreateRequest).when(mockCreateResponse).getRequest();

    final Map<String, Serializable> properties = new HashMap<>();
    properties.put(Constants.CONTENT_PATHS, tmpContentPaths);
    doReturn(properties).when(mockCreateResponse).getProperties();
    return mockCreateResponse;
  }

  private UpdateStorageResponse createMockUpdateStorageResponse(ContentItem... contentItems) {
    final UpdateStorageResponse mockUpdateResponse = mock(UpdateStorageResponse.class);
    doReturn(Arrays.asList(contentItems)).when(mockUpdateResponse).getUpdatedContentItems();
    final UpdateStorageRequest mockUpdateRequest = mock(UpdateStorageRequest.class);
    doReturn(mockUpdateRequest).when(mockUpdateResponse).getRequest();

    final Map<String, Serializable> properties = new HashMap<>();
    properties.put(Constants.CONTENT_PATHS, tmpContentPaths);
    doReturn(properties).when(mockUpdateResponse).getProperties();
    return mockUpdateResponse;
  }

  /** verify methods */
  private byte[] getThumbnail(
      ContentItem unprocessedContentItem, ContentItem processedContentItem) {
    assertThat(
        "The returned content item should be the same as the original content item",
        unprocessedContentItem,
        is(processedContentItem));
    final byte[] thumbnail = processedContentItem.getMetacard().getThumbnail();
    return thumbnail;
  }

  private void verifyThumbnailIsGif(
      ContentItem unprocessedContentItem, ContentItem processedContentItem) {
    final byte[] thumbnail = getThumbnail(unprocessedContentItem, processedContentItem);
    assertThat("The thumbnail should not be null", thumbnail, notNullValue());

    assertThat(
        "The thumbnail should have the right GIF header bytes", thumbnail[0], is((byte) 0x47));
    assertThat(
        "The thumbnail should have the right GIF header bytes", thumbnail[1], is((byte) 0x49));
    assertThat(
        "The thumbnail should have the right GIF header bytes", thumbnail[2], is((byte) 0x46));
    assertThat(
        "The thumbnail should have the right GIF header bytes", thumbnail[3], is((byte) 0x38));
    assertThat(
        "The thumbnail should have the right GIF header bytes", thumbnail[4], is((byte) 0x39));
    assertThat(
        "The thumbnail should have the right GIF header bytes", thumbnail[5], is((byte) 0x61));
  }

  private void verifyThumbnailIsPng(
      ContentItem unprocessedContentItem, ContentItem processedContentItem) {
    final byte[] thumbnail = getThumbnail(unprocessedContentItem, processedContentItem);
    assertThat("The thumbnail should not be null", thumbnail, notNullValue());

    assertThat(
        "The thumbnail should have the right PNG header bytes", thumbnail[0], is((byte) 0x89));
    assertThat(
        "The thumbnail should have the right PNG header bytes", thumbnail[1], is((byte) 0x50));
    assertThat(
        "The thumbnail should have the right PNG header bytes", thumbnail[2], is((byte) 0x4E));
    assertThat(
        "The thumbnail should have the right PNG header bytes", thumbnail[3], is((byte) 0x47));
    assertThat(
        "The thumbnail should have the right PNG header bytes", thumbnail[4], is((byte) 0x0D));
    assertThat(
        "The thumbnail should have the right PNG header bytes", thumbnail[5], is((byte) 0x0A));
    assertThat(
        "The thumbnail should have the right PNG header bytes", thumbnail[6], is((byte) 0x1A));
    assertThat(
        "The thumbnail should have the right PNG header bytes", thumbnail[7], is((byte) 0x0A));
  }

  private void verifyThumbnailIsNotSet(
      ContentItem unprocessedContentItem, ContentItem processedContentItem) {
    final byte[] thumbnail = getThumbnail(unprocessedContentItem, processedContentItem);
    assertThat("The thumbnail should be null", thumbnail, nullValue());
  }
}
