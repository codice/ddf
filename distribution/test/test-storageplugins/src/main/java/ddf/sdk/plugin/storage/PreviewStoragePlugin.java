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
package ddf.sdk.plugin.storage;

import com.google.common.io.ByteSource;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.plugin.PreCreateStoragePlugin;
import ddf.catalog.content.plugin.PreUpdateStoragePlugin;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.plugin.PluginExecutionException;
import java.util.List;
import java.util.stream.Collectors;

/** Plugin showing how to create derived {@link ContentItem}s. */
public class PreviewStoragePlugin implements PreCreateStoragePlugin, PreUpdateStoragePlugin {

  @Override
  public CreateStorageRequest process(CreateStorageRequest input) throws PluginExecutionException {
    if (input == null) {
      return input;
    }
    input.getContentItems().addAll(createPreviewItems(input.getContentItems()));
    return input;
  }

  @Override
  public UpdateStorageRequest process(UpdateStorageRequest input) throws PluginExecutionException {
    if (input == null) {
      return input;
    }
    input.getContentItems().addAll(createPreviewItems(input.getContentItems()));
    return input;
  }

  private List<ContentItem> createPreviewItems(List<ContentItem> items) {
    return items.stream()
        .filter(item -> item.getMetacard().getThumbnail() != null)
        .map(ContentItem::getMetacard)
        .map(this::createPreviewItem)
        .collect(Collectors.toList());
  }

  private ContentItem createPreviewItem(Metacard metacard) {
    ContentItem preview =
        new ContentItemImpl(
            metacard.getId(),
            "preview",
            ByteSource.wrap(metacard.getThumbnail()),
            "image/jpg",
            metacard.getTitle(),
            metacard.getThumbnail().length,
            metacard);
    metacard.setAttribute(new AttributeImpl(Metacard.DERIVED_RESOURCE_URI, preview.getUri()));
    return preview;
  }
}
