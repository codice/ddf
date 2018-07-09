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
package org.codice.ddf.config.model.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.config.model.MimeTypeConfig;

public class MimeTypeConfigImpl extends AbstractConfigGroup implements MimeTypeConfig {
  private String name;
  private int priority;
  private final MultiValuedMap<String, String> mimesToExtensions = new HashSetValuedHashMap<>();
  private final Map<String, String> extensionsToMimes = new HashMap<>();

  public MimeTypeConfigImpl() {}

  public MimeTypeConfigImpl(
      String id, String name, int priority, Map<String, String> customMimeTypes, String version) {
    super(id, version);
    this.name = name;
    this.priority = priority;
    setCustomMimeTypes(customMimeTypes);
  }

  @SuppressWarnings(
      "squid:ForLoopCounterChangedCheck" /* Purposely iterating 2 entries at a time */)
  public MimeTypeConfigImpl(
      String id, String name, int priority, String version, String... extsToMimes) {
    super(id, version);
    this.name = name;
    this.priority = priority;
    for (int i = 0; i < extsToMimes.length; i++) {
      final String ext = StringUtils.prependIfMissing(extsToMimes[i], ".");
      final String mime = extsToMimes[++i];

      extensionsToMimes.putIfAbsent(ext, mime);
      mimesToExtensions.put(mime, ext);
    }
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  @Override
  public Optional<String> getExtensionFor(String mimeType) {
    return mimesToExtensions.get(mimeType).stream().findFirst();
  }

  @Override
  public Optional<String> getMimeTypeFor(String extension) {
    return Optional.ofNullable(extensionsToMimes.get(StringUtils.prependIfMissing(extension, ".")));
  }

  @Override
  public Stream<Mapping> mappings() {
    return extensionsToMimes.entrySet().stream().map(MappingImpl::new);
  }

  public void setCustomMimeTypes(Map<String, String> customMimeTypes) {
    extensionsToMimes.clear();
    mimesToExtensions.clear();
    extensionsToMimes.putAll(customMimeTypes);
    extensionsToMimes.forEach((ext, mime) -> mimesToExtensions.put(mime, ext));
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(name, priority, extensionsToMimes);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof MimeTypeConfigImpl) {
      final MimeTypeConfigImpl cfg = (MimeTypeConfigImpl) obj;

      return super.equals(obj)
          && (priority == cfg.priority)
          && Objects.equals(name, cfg.name)
          && Objects.equals(extensionsToMimes, cfg.extensionsToMimes);
    }
    return false;
  }

  private static class MappingImpl implements MimeTypeConfig.Mapping {
    private final String extension;

    private final String mimeType;

    MappingImpl(Map.Entry<String, String> entry) {
      this.extension = entry.getKey();
      this.mimeType = entry.getValue();
    }

    @Override
    public String getExtension() {
      return extension;
    }

    @Override
    public String getMimeType() {
      return mimeType;
    }

    @Override
    public int hashCode() {
      return 31 * super.hashCode() + Objects.hash(extension, mimeType);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (obj instanceof MappingImpl) {
        final MappingImpl mapping = (MappingImpl) obj;

        return Objects.equals(extension, mapping.extension)
            && Objects.equals(mimeType, mapping.mimeType)
            && super.equals(obj);
      }
      return false;
    }

    @Override
    public String toString() {
      return extension + " -> " + mimeType;
    }
  }
}
