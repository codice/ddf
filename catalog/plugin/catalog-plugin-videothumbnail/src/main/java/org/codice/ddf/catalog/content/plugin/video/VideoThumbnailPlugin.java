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

import ddf.catalog.Constants;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.content.plugin.PostCreateStoragePlugin;
import ddf.catalog.content.plugin.PostUpdateStoragePlugin;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.video.thumbnail.VideoThumbnail;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoThumbnailPlugin implements PostCreateStoragePlugin, PostUpdateStoragePlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(VideoThumbnailPlugin.class);

  private final VideoThumbnail videoThumbnail;

  public VideoThumbnailPlugin(final VideoThumbnail videoThumbnail) {
    this.videoThumbnail = videoThumbnail;
  }

  @Override
  public CreateStorageResponse process(final CreateStorageResponse input) {
    // TODO: How to handle application/octet-stream?
    processContentItems(input.getCreatedContentItems(), input.getProperties());
    return input;
  }

  @Override
  public UpdateStorageResponse process(final UpdateStorageResponse input) {
    // TODO: How to handle application/octet-stream?
    processContentItems(input.getUpdatedContentItems(), input.getProperties());
    return input;
  }

  private void processContentItems(
      final List<ContentItem> contentItems, final Map<String, Serializable> properties) {
    Map<String, Map<String, Path>> tmpContentPaths =
        (Map<String, Map<String, Path>>) properties.get(Constants.CONTENT_PATHS);

    for (ContentItem contentItem : contentItems) {
      Map<String, Path> contentPaths = videoContentPaths(tmpContentPaths, contentItem);

      if (contentPaths != null) {
        // create a thumbnail for the unqualified content item
        Path tmpPath = contentPaths.get(null);
        if (tmpPath != null) {
          createThumbnail(contentItem, tmpPath);
        }
      }
    }
  }

  private Map<String, Path> videoContentPaths(
      Map<String, Map<String, Path>> tmpContentPaths, ContentItem contentItem) {
    if (!isVideo(contentItem)) {
      return null;
    }

    Map<String, Path> contentPaths = tmpContentPaths.get(contentItem.getId());
    if (contentPaths == null || contentPaths.isEmpty()) {
      LOGGER.debug(
          "No path for ContentItem (id={}). Unable to create thumbnail for metacard (id={})",
          contentItem.getId(),
          contentItem.getMetacard().getId());
      return null;
    }
    return contentPaths;
  }

  private boolean isVideo(final ContentItem contentItem) {
    return videoThumbnail.isVideo(contentItem.getMimeType());
  }

  private void createThumbnail(final ContentItem contentItem, final Path contentPath) {

    LOGGER.trace("About to create video thumbnail");

    try {

      Optional<byte[]> optionalThumbnailBytes =
          videoThumbnail.videoThumbnail(
              contentPath.toAbsolutePath().toFile(), contentItem.getMimeType());

      optionalThumbnailBytes.ifPresent(
          bytes -> {
            addThumbnailAttribute(contentItem, bytes);
            LOGGER.debug(
                "Successfully created video thumbnail for ContentItem (id={})",
                contentItem.getId());
          });

    } catch (IOException e) {
      LOGGER.warn("Error creating thumbnail for ContentItem (id={}).", contentItem.getId(), e);
    } catch (InterruptedException e) {
      LOGGER.warn("Error creating thumbnail for ContentItem (id={}).", contentItem.getId(), e);
      Thread.currentThread().interrupt();
    }
  }

  private void addThumbnailAttribute(final ContentItem contentItem, final byte[] thumbnailBytes) {
    contentItem.getMetacard().setAttribute(new AttributeImpl(Metacard.THUMBNAIL, thumbnailBytes));
  }
}
