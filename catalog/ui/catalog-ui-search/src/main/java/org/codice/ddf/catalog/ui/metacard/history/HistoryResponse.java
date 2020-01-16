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
package org.codice.ddf.catalog.ui.metacard.history;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class HistoryResponse {
  private Instant versioned;

  private Map<String, Object> attributes;

  private String versionId, editedBy;

  public HistoryResponse(String versionId, String editedBy, Instant versioned) {
    this.versionId = versionId;
    this.editedBy = editedBy;
    this.versioned = versioned;
  }

  public HistoryResponse(
      String versionId, String editedBy, Date versioned, Map<String, Object> attributes) {
    this.versionId = versionId;
    this.editedBy = editedBy;
    this.versioned = versioned.toInstant();
    this.attributes = attributes;
  }

  public HistoryResponse(String historyId, String editedBy, Date versioned) {
    this(historyId, editedBy, versioned.toInstant());
  }

  public Instant getVersioned() {
    return versioned;
  }

  public void setVersioned(Instant versioned) {
    this.versioned = versioned;
  }

  public String getVersionId() {
    return versionId;
  }

  public void setVersionId(String id) {
    this.versionId = id;
  }

  public String getEditedBy() {
    return editedBy;
  }

  public void setEditedBy(String editedBy) {
    this.editedBy = editedBy;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }
}
