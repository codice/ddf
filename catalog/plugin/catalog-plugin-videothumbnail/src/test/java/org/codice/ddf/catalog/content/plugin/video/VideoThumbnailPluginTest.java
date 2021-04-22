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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.Constants;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.video.thumbnail.VideoThumbnail;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

public class VideoThumbnailPluginTest {

  private static final byte[] GIF = new byte[] {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};

  private static final byte[] PNG =
      new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

  private static final MimeType VIDEO_MP4;

  private static final MimeType IMAGE_JPEG;

  private VideoThumbnailPlugin videoThumbnailPlugin;

  private HashMap<String, Map> tmpContentPaths;

  private VideoThumbnail videoThumbnail;

  static {
    try {
      VIDEO_MP4 = new MimeType("video/mp4");
      IMAGE_JPEG = new MimeType("image/jpeg");
    } catch (MimeTypeParseException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @Before
  public void setUp() {
    videoThumbnail = mock(VideoThumbnail.class);

    videoThumbnailPlugin = new VideoThumbnailPlugin(videoThumbnail);

    tmpContentPaths = new HashMap<>();
  }

  /**
   * Tests processing a mix of {@link ContentItem}s where some get thumbnails. Also tests that
   * processing an {@link UpdateStorageResponse} works for different edge cases.
   */
  @Test
  public void testProcessMixedContentItems() throws Exception {
    // given
    Pair<File, Path> mediumContent = mockContent(307707);
    Pair<File, Path> longContent = mockContent(926666);
    Pair<File, Path> corruptedContent = mockContent(92160);
    Pair<File, Path> shortContent = mockContent(137408);

    final ContentItem mediumVideoMockContentItem =
        createMockVideoContentItemFromResource(mediumContent.getRight());
    final ContentItem longVideoMockContentItem =
        createMockVideoContentItemFromResource(longContent.getRight());
    final ContentItem corruptedVideoMockContentItem =
        createMockVideoContentItemFromResource(corruptedContent.getRight());
    final ContentItem shortVideoMockContentItem =
        createMockVideoContentItemFromResource(shortContent.getRight());
    final ContentItem notVideoMockContentItem =
        createMockContentItemOfMimeType(IMAGE_JPEG.toString());

    when(videoThumbnail.isVideo(withVideoMime())).thenReturn(true);

    when(videoThumbnail.videoThumbnail(eq(mediumContent.getLeft()), withVideoMime()))
        .thenReturn(Optional.of(GIF));
    when(videoThumbnail.videoThumbnail(eq(longContent.getLeft()), withVideoMime()))
        .thenReturn(Optional.of(GIF));
    when(videoThumbnail.videoThumbnail(eq(corruptedContent.getLeft()), withVideoMime()))
        .thenThrow(IOException.class);
    when(videoThumbnail.videoThumbnail(eq(shortContent.getLeft()), withVideoMime()))
        .thenReturn(Optional.of(PNG));

    final UpdateStorageResponse updateStorageResponse =
        createMockUpdateStorageResponse(
            mediumVideoMockContentItem,
            longVideoMockContentItem,
            corruptedVideoMockContentItem,
            shortVideoMockContentItem,
            notVideoMockContentItem);

    // when
    final UpdateStorageResponse processedUpdateResponse =
        videoThumbnailPlugin.process(updateStorageResponse);

    // then
    final List<ContentItem> processedContentItems =
        processedUpdateResponse.getUpdatedContentItems();
    assertThat(
        "There should be exactly 5 returned content items", processedContentItems, hasSize(5));

    verifyThumbnailIsGif(mediumVideoMockContentItem, processedContentItems.get(0));
    verifyThumbnailIsGif(longVideoMockContentItem, processedContentItems.get(1));
    verifyThumbnailIsNotSet(corruptedVideoMockContentItem, processedContentItems.get(2));
    verifyThumbnailIsPng(shortVideoMockContentItem, processedContentItems.get(3));
    verifyThumbnailIsNotSet(notVideoMockContentItem, processedContentItems.get(4));
  }

  private Pair<File, Path> mockContent(long size) {
    File file = mock(File.class);
    Path path = mock(Path.class);
    when(path.toAbsolutePath()).thenReturn(path);
    when(path.toFile()).thenReturn(file);
    when(file.length()).thenReturn(size);
    return Pair.of(file, path);
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

  private ContentItem createMockVideoContentItemFromResource(Path path) throws Exception {

    final ContentItem mockContentItem = createMockVideoContentItem();

    HashMap<String, Path> contentItemPaths = new HashMap<>();
    contentItemPaths.put(null, path);
    tmpContentPaths.put(mockContentItem.getId(), contentItemPaths);

    return mockContentItem;
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
    return processedContentItem.getMetacard().getThumbnail();
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

  private static MimeType withVideoMime() {
    return argThat(new VideoMimeTypeMatcher());
  }

  private static class VideoMimeTypeMatcher implements ArgumentMatcher<MimeType> {
    public boolean matches(MimeType mimeType) {
      return VIDEO_MP4.match(mimeType);
    }
  }
}
