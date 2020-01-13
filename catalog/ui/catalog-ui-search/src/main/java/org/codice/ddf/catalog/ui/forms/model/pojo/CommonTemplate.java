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
import ddf.catalog.data.types.Security;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Provides data model pojo that can be annotated for JSON serialization.
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

  private final Date modified;

  private final String owner;

  protected List<Serializable> accessGroups;

  protected List<Serializable> accessGroupsRead;

  protected List<Serializable> accessIndividuals;

  protected List<Serializable> accessIndividualsRead;

  protected List<Serializable> accessAdministrators;

  public CommonTemplate(Metacard metacard) {
    this.id = safeGet(metacard, Core.ID, String.class);
    this.title = safeGet(metacard, Core.TITLE, String.class);
    this.description = safeGet(metacard, Core.DESCRIPTION, String.class);

    this.created = safeGet(metacard, Core.CREATED, Date.class);
    this.modified = safeGet(metacard, Core.METACARD_MODIFIED, Date.class);
    this.owner = safeGet(metacard, Core.METACARD_OWNER, String.class);
  }

  public CommonTemplate(Metacard metacard, Map<String, List<Serializable>> securityAttributes) {
    this.id = safeGet(metacard, Core.ID, String.class);
    this.title = safeGet(metacard, Core.TITLE, String.class);
    this.description = safeGet(metacard, Core.DESCRIPTION, String.class);

    this.created = safeGet(metacard, Core.CREATED, Date.class);
    this.modified = safeGet(metacard, Core.METACARD_MODIFIED, Date.class);
    this.owner = safeGet(metacard, Core.METACARD_OWNER, String.class);

    this.accessIndividuals =
        securityAttributes.getOrDefault(Security.ACCESS_INDIVIDUALS, new ArrayList<>());
    this.accessIndividualsRead =
        securityAttributes.getOrDefault(Security.ACCESS_INDIVIDUALS_READ, new ArrayList<>());
    this.accessGroups = securityAttributes.getOrDefault(Security.ACCESS_GROUPS, new ArrayList<>());
    this.accessGroupsRead =
        securityAttributes.getOrDefault(Security.ACCESS_GROUPS_READ, new ArrayList<>());
    this.accessAdministrators =
        securityAttributes.getOrDefault(Security.ACCESS_ADMINISTRATORS, new ArrayList<>());
  }

  public CommonTemplate(Map<String, Object> input) {
    this.id = (String) input.get(Core.ID);
    this.title = (String) input.get(Core.TITLE);
    this.description = (String) input.get(Core.DESCRIPTION);

    // Let the framework decide on the date info when creating / updating
    this.created = null;
    this.modified = null;

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

  public Date getModified() {
    return modified;
  }

  public String getOwner() {
    return owner;
  }

  public List<Serializable> getAccessGroups() {
    return accessGroups;
  }

  public List<Serializable> getAccessGroupsRead() {
    return accessGroupsRead;
  }

  public List<Serializable> getAccessIndividuals() {
    return accessIndividuals;
  }

  public List<Serializable> getAccessIndividualsRead() {
    return accessIndividualsRead;
  }

  public List<Serializable> getAccessAdministrators() {
    return accessAdministrators;
  }
}
