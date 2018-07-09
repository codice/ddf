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

import java.util.Map;
import java.util.Objects;
import org.codice.ddf.config.model.SchemaMimeTypeConfig;

public class SchemaMimeTypeConfigImpl extends MimeTypeConfigImpl implements SchemaMimeTypeConfig {
  private String schema;

  public SchemaMimeTypeConfigImpl() {}

  public SchemaMimeTypeConfigImpl(
      String id,
      String name,
      int priority,
      String schema,
      Map<String, String> customMimeTypes,
      String version) {
    super(id, name, priority, customMimeTypes, version);
    this.schema = schema;
  }

  public SchemaMimeTypeConfigImpl(
      String id, String name, int priority, String schema, String version, String... extsToMimes) {
    super(id, name, priority, version, extsToMimes);
    this.schema = schema;
  }

  @Override
  public String getSchema() {
    return null;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hashCode(schema);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof SchemaMimeTypeConfigImpl) {
      final SchemaMimeTypeConfigImpl cfg = (SchemaMimeTypeConfigImpl) obj;

      return Objects.equals(schema, cfg.schema) && super.equals(obj);
    }
    return false;
  }
}
