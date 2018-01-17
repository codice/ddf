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
package org.codice.ddf.admin.application.service.migratable;

import com.google.common.annotations.VisibleForTesting;
import java.util.Objects;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;

/** Defines a Json object to represent an exported application. */
public class JsonApplication implements JsonValidatable {
  @Nullable // only because Boon may not set if as it bypasses our ctor and the final keyword
  private final String name;

  @Nullable private final String version;

  @Nullable private final String description;

  @Nullable private final String uri;

  @Nullable // only because Boon may not set if as it bypasses our ctor and the final keyword
  private final boolean started;

  JsonApplication(Application app, ApplicationService applicationService) {
    this(
        app.getName(),
        app.getVersion(),
        app.getDescription(),
        app.getURI().toString(),
        applicationService.isApplicationStarted(app));
  }

  JsonApplication(
      String name,
      @Nullable String version,
      @Nullable String description,
      @Nullable String uri,
      boolean started) {
    Validate.notNull(name, "invalid null name");
    this.name = name;
    this.version = version;
    this.description = description;
    this.uri = uri;
    this.started = started;
  }

  @VisibleForTesting
  JsonApplication(String name, boolean started) {
    this(name, null, null, null, started);
  }

  public String getName() {
    return name;
  }

  public boolean isStarted() {
    return started;
  }

  @Override
  public void validate() {
    Validate.notNull(name, "missing required application name");
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof JsonApplication) {
      final JsonApplication japp = (JsonApplication) o;

      return (started == japp.started)
          && name.equals(japp.name)
          && Objects.equals(version, japp.version)
          && Objects.equals(description, japp.description)
          && Objects.equals(uri, japp.uri);
    }
    return false;
  }

  @Override
  public String toString() {
    return "application [" + name + "]";
  }
}
