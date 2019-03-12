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
package org.codice.ddf.catalog.sourcepoller;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.google.common.base.Objects;
import ddf.catalog.source.Source;
import ddf.catalog.util.Describable;
import org.apache.commons.lang3.StringUtils;

/**
 * Used to compared {@link Source}s in a {@link SourcePoller}
 *
 * <p>TODO DDF-4288
 *
 * <p>One of the known limitations of this implementation is that modifying a source will cause its
 * {@link SourceKey} to be different. In the {@link Poller} this will mean that there will be a
 * period where {@link Poller#getCachedValue(Object)} will return "unknown" for the new {@link
 * SourceKey} even though there has already been a poll for the {@link SourceKey}.
 */
class SourceKey {

  private final String version;

  private final String id;

  private final String title;

  private final String description;

  private final String organization;

  /**
   * @throws NullPointerException if {@link Describable#getId()} is {@code null}
   * @throws IllegalArgumentException if {@link Describable#getId()} is empty
   */
  SourceKey(final Source source) {
    this.version = source.getVersion();
    this.id = notEmpty(notNull(source).getId());
    this.title = source.getTitle();
    this.description = source.getDescription();
    this.organization = source.getOrganization();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(version, id, title, description, organization);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof SourceKey)) {
      return false;
    }

    final SourceKey sourceKey = (SourceKey) obj;
    return StringUtils.equals(id, sourceKey.id)
        && StringUtils.equals(title, sourceKey.title)
        && StringUtils.equals(version, sourceKey.version)
        && StringUtils.equals(description, sourceKey.description)
        && StringUtils.equals(organization, sourceKey.organization);
  }

  @Override
  public String toString() {
    return String.format(
        "{version='%s', id='%s', title='%s', description='%s', organization='%s'}",
        version, id, title, description, organization);
  }
}
