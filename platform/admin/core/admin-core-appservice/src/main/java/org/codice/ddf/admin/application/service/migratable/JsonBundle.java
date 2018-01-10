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
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.boon.json.annotations.JsonIgnore;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/** Defines a Json object to represent an exported bundle. */
public class JsonBundle implements JsonValidatable {
  public static final String UNINSTALLED_STATE_STRING =
      JsonBundle.getStateString(Bundle.UNINSTALLED);

  @Nullable // only because Boon may not set it as it bypasses our ctor and the final keyword
  private final String name;

  @Nullable // only because Boon may not set it as it bypasses our ctor and the final keyword
  private final Version version;

  @Nullable // only because Boon may not set it as it bypasses our ctor and the final keyword
  private final Long id; // Long used to detect missing Json entries as null

  @Nullable // only because Boon may not set it as it bypasses our ctor and the final keyword
  private final Integer state; // Integer used to detect missing Json entries as null

  @Nullable // only because Boon may not set it as it bypasses our ctor and the final keyword
  private final String location;

  /**
   * Constructs a new <code>JsonBundle</code> based on the given bundle.
   *
   * @param bundle the bundle to create a Json representation for
   * @throws NullPointerException if <code>bundle</code> is <code>null</code> or if the bundle has
   *     no symbolic name
   */
  public JsonBundle(Bundle bundle) {
    this(
        bundle.getSymbolicName(),
        bundle.getVersion(),
        bundle.getBundleId(),
        bundle.getState(),
        bundle.getLocation());
  }

  /**
   * Constructs a new <code>JsonBundle</code> given its separate parts.
   *
   * @param name the bundle name
   * @param version the bundle version
   * @param id the bundle identifier
   * @param state the bundle state
   * @param location the bundle location
   * @throws IllegalArgumentException if any of the arguments are <code>null</code> or if <code>
   *     version</code> is improperly formatted
   */
  @VisibleForTesting
  JsonBundle(String name, String version, long id, int state, String location) {
    Validate.notNull(name, "invalid null name");
    Validate.notNull(version, "invalid null version");
    Validate.notNull(location, "invalid null location");
    this.name = name;
    this.version = new Version(version);
    this.id = id;
    this.state = state;
    this.location = location;
  }

  /**
   * Constructs a new <code>JsonBundle</code> given its separate parts.
   *
   * @param name the bundle name
   * @param version the bundle version
   * @param id the bundle identifier
   * @param state the bundle state
   * @param location the bundle location
   * @throws IllegalArgumentException if any of the arguments are <code>null</code>
   */
  @VisibleForTesting
  JsonBundle(String name, Version version, long id, int state, String location) {
    Validate.notNull(name, "invalid null bundle name");
    Validate.notNull(version, "invalid null bundle version");
    Validate.notNull(location, "invalid null bundle location");
    this.name = name;
    this.version = version;
    this.id = id;
    this.state = state;
    this.location = location;
  }

  /**
   * Gets the symbolic name for this bundle.
   *
   * @return the symbolic name for this bundle
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the name for this bundle (including its version).
   *
   * @return the name for this bundle (including its version)
   */
  public String getFullName() {
    return name + '/' + version;
  }

  public Version getVersion() {
    return version;
  }

  public long getId() {
    return id;
  }

  /**
   * Gets the state for this bundle.
   *
   * @return the state for this bundle
   */
  public int getState() {
    return state;
  }

  /**
   * Gets a simple representation for this bundle state.
   *
   * @return a simple representation for this bundle state
   */
  @JsonIgnore
  public SimpleState getSimpleState() {
    return JsonBundle.getSimpleState(state);
  }

  public String getLocation() {
    return location;
  }

  @Override
  public void validate() {
    Validate.notNull(name, "missing required bundle name");
    Validate.notNull(version, "missing required bundle version");
    Validate.notNull(id, "missing required bundle id");
    Validate.notNull(state, "missing required bundle state");
    Validate.notNull(location, "missing required bundle location");
  }

  @Override
  public int hashCode() {
    return 31 * name.hashCode() + version.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof JsonBundle) {
      final JsonBundle jbundle = (JsonBundle) o;

      return name.equals(jbundle.name)
          && version.equals(jbundle.version)
          && id.equals(jbundle.id)
          && state.equals(jbundle.state)
          && location.equals(jbundle.location);
    }
    return false;
  }

  @Override
  public String toString() {
    return "bundle [" + getFullName() + "]";
  }

  public static String getStateString(Bundle bundle) {
    return JsonBundle.getStateString(bundle.getState());
  }

  public static String getFullName(Bundle bundle) {
    return bundle.getSymbolicName() + '/' + bundle.getVersion();
  }

  public static SimpleState getSimpleState(Bundle bundle) {
    return JsonBundle.getSimpleState(bundle.getState());
  }

  private static String getStateString(int state) {
    return String.format("%s/%x", JsonBundle.getSimpleState(state), state);
  }

  private static SimpleState getSimpleState(int state) {
    if (state == Bundle.UNINSTALLED) {
      return SimpleState.UNINSTALLED;
    } else if ((state == Bundle.STARTING) || (state == Bundle.ACTIVE)) {
      return SimpleState.ACTIVE;
    } // else - INSTALLED OR STOPPING
    return SimpleState.INSTALLED;
  }

  /** Simple representation for bundle states. */
  public enum SimpleState {
    UNINSTALLED,
    INSTALLED,
    ACTIVE
  }
}
