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
package org.codice.ddf.catalog.ui.forms.model.pojo;

import static org.codice.ddf.catalog.ui.util.AccessUtil.safeGet;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import java.util.Date;
import java.util.Map;

/**
 * Provides data model pojo that can be annotated and sent to Boon for JSON serialization.
 *
 * <p>{@link CommonTemplate} is the base used for transporting template data completely separate
 * from any filter structures that may be needed. These properties apply to both query templates and
 * result templates.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class CommonTemplate {
  private final String id;

  private final String title;

  private final String description;

  private final Date created;

  private final String owner;

  public CommonTemplate(Metacard metacard) {
    this.id = safeGet(metacard, Core.ID, String.class);
    this.title = safeGet(metacard, Core.TITLE, String.class);
    this.description = safeGet(metacard, Core.DESCRIPTION, String.class);

    this.created = safeGet(metacard, Core.CREATED, Date.class);
    this.owner = safeGet(metacard, Core.METACARD_OWNER, String.class);
  }

  public CommonTemplate(Map<String, Object> input) {
    this.id = (String) input.get(Core.ID);
    this.title = (String) input.get(Core.TITLE);
    this.description = (String) input.get(Core.DESCRIPTION);

    Long t = (Long) input.get(Core.CREATED);
    this.created = (t == null) ? new Date() : new Date(t);
    this.owner = (String) input.get(Core.METACARD_OWNER);
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public Date getCreated() {
    return created;
  }

  public String getOwner() {
    return owner;
  }
}
