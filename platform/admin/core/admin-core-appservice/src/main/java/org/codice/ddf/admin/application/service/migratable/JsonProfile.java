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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.lang.Validate;

/** Defines a Json object to represent an exported profile. */
public class JsonProfile implements JsonValidatable {
  private final List<JsonApplication> apps;
  private final List<JsonFeature> features;
  private final List<JsonBundle> bundles;

  /**
   * Constructs a default Json profile.
   *
   * <p><i>Note:</i> This constructor is primarly defined for Json deserialization such that we end
   * up initializing the lists with empty collections. This will be helpful, for example, in case
   * where no features were serialized in which case Boon would not be setting this attribute.
   */
  public JsonProfile() {
    this.apps = new ArrayList<>();
    this.features = new ArrayList<>();
    this.bundles = new ArrayList<>();
  }

  /**
   * Constructs a new Json profile given the provided set of Json applications, features, and
   * bundles.
   *
   * @param apps the Json applications for this profile
   * @param features the Json features for this profile
   * @param bundles the Json bundles for this profile
   * @throws IllegalArgumentException if <code>apps</code>, <code>features</code>, or <code>bundles
   *     </code> is <code>null</code>
   */
  public JsonProfile(
      List<JsonApplication> apps, List<JsonFeature> features, List<JsonBundle> bundles) {
    Validate.notNull(apps, "invalid null applications");
    Validate.notNull(features, "invalid null features");
    Validate.notNull(bundles, "invalid null bundles");
    this.apps = apps;
    this.features = features;
    this.bundles = bundles;
  }

  @VisibleForTesting
  JsonProfile(JsonApplication japp, JsonFeature jfeature, JsonBundle jbundle) {
    this.apps = Collections.singletonList(japp);
    this.features = Collections.singletonList(jfeature);
    this.bundles = Collections.singletonList(jbundle);
  }

  /**
   * Retrieves all exported applications.
   *
   * @return a stream of all exported applications
   */
  public Stream<JsonApplication> applications() {
    return apps.stream();
  }

  /**
   * Retrieves all exported features.
   *
   * @return a stream of all exported features
   */
  public Stream<JsonFeature> features() {
    return features.stream();
  }

  /**
   * Retrieves all exported bundles
   *
   * @return a stream of all exported bundles
   */
  public Stream<JsonBundle> bundles() {
    // sort bundles based on bundle id to be sure we process them in that order
    return bundles.stream().sorted(Comparator.comparing(JsonBundle::getId));
  }

  @Override
  public void validate() {
    Validate.notNull(apps, "missing required apps");
    Validate.notNull(features, "missing required features");
    Validate.notNull(bundles, "missing required bundles");
    Stream.of(apps.stream(), features.stream(), bundles.stream())
        .flatMap(Function.identity())
        .map(JsonValidatable.class::cast)
        .forEach(JsonValidatable::validate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apps, features, bundles);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof JsonProfile) {
      final JsonProfile jprofile = (JsonProfile) o;

      return apps.equals(jprofile.apps)
          && features.equals(jprofile.features)
          && bundles.equals(jprofile.bundles);
    }
    return false;
  }

  @Override
  public String toString() {
    return Stream.of(apps.stream(), features.stream(), bundles.stream())
        .flatMap(Function.identity())
        .map(Object::toString)
        .collect(java.util.stream.Collectors.joining(", ", "profile [", "]"));
  }
}
