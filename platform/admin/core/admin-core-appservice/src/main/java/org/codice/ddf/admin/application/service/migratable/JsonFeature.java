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
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;

/** Defines a Json object to represent an exported feature. */
public class JsonFeature implements JsonValidatable {
  private static final String FEATURE_OSGI_REQUIREMENT_FORMAT = "feature:%s/[%s,%s]";

  @Nullable // only because Boon may not set it as it bypasses our ctor and the final keyword
  private final String name;

  @Nullable // only because Boon may not set it as it bypasses our ctor and the final keyword
  private final String id;

  @Nullable private final String version;

  @Nullable private final String description;

  @Nullable // only because Boon may not set it as it bypasses our ctor and the final keyword
  private final FeatureState state;

  @Nullable // only because Boon may not set it as it bypasses our ctor and the final keyword
  private final Boolean required; // Boolean used to detect missing Json entries as null

  @Nullable private final String region;

  @Nullable private final String repository;

  @Nullable // only because Boon may not set it as it bypasses our ctor and the final keyword
  private final Integer startLevel; // Integer used to detect missing Json entries as null

  /**
   * Constructs a new <code>JsonFeature</code> based on the given feature.
   *
   * @param feature the feature to create a Json representation for
   * @param featureService service used for retrieving info for features
   * @throws NullPointerException if <code>feature</code> or <code>featureService </code> is <code>
   *     null</code>
   */
  public JsonFeature(Feature feature, FeaturesService featureService) {
    this(
        feature.getName(),
        feature.getId(),
        feature.getVersion(),
        feature.getDescription(),
        featureService.getState(feature.getId()),
        featureService.isRequired(feature),
        null,
        feature.getRepositoryUrl(),
        feature.getStartLevel());
  }

  /**
   * Constructs a new <code>JsonFeature</code> given its separate parts.
   *
   * @param name the feature name
   * @param id the feature identifier
   * @param version the feature optional version
   * @param description the feature optional description
   * @param state the feature state
   * @param required whether the feature is required or not
   * @param region the feature optional region
   * @param startLevel the feature start level
   * @throws IllegalArgumentException if <code>name</code>, <code>id</code>, or <code>state</code>
   *     is <code>null</code>
   */
  @SuppressWarnings(
      "squid:S00107" /* Pojo attributes initialization of final attributes requires 9 parameters */)
  @VisibleForTesting
  JsonFeature(
      String name,
      String id,
      @Nullable String version,
      @Nullable String description,
      FeatureState state,
      boolean required,
      @Nullable String region,
      @Nullable String repository,
      int startLevel) {
    Validate.notNull(name, "invalid null name");
    Validate.notNull(id, " invalid null id");
    Validate.notNull(state, "invalid null state");
    this.name = name;
    this.id = id;
    this.version = version;
    this.description = description;
    this.state = state;
    this.required = required;
    this.region = region;
    this.repository = repository;
    this.startLevel = startLevel;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public String getVersion() {
    return version;
  }

  @Nullable
  public String getDescription() {
    return description;
  }

  public FeatureState getState() {
    return state;
  }

  public boolean isRequired() {
    return required;
  }

  public String getRegion() {
    return (region != null) ? region : FeaturesService.ROOT_REGION;
  }

  @Nullable
  public String getRepository() {
    return repository;
  }

  public int getStartLevel() {
    return startLevel;
  }

  /**
   * Builds a requirement string for this feature.
   *
   * @return the corresponding requirement string
   */
  public String toRequirement() {
    return String.format(JsonFeature.FEATURE_OSGI_REQUIREMENT_FORMAT, name, version, version);
  }

  @Override
  public void validate() {
    Validate.notNull(name, "missing required feature name");
    Validate.notNull(id, "missing required feature id");
    Validate.notNull(state, "missing required feature state");
    Validate.notNull(required, "missing feature required flag");
    Validate.notNull(startLevel, "missing required feature start level");
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof JsonFeature) {
      final JsonFeature jfeature = (JsonFeature) o;

      return (state == jfeature.state)
          && name.equals(jfeature.name)
          && id.equals(jfeature.id)
          && Objects.equals(version, jfeature.version)
          && Objects.equals(description, jfeature.description)
          && required.equals(jfeature.required)
          && Objects.equals(region, jfeature.region)
          && Objects.equals(repository, jfeature.repository)
          && startLevel.equals(jfeature.startLevel);
    }
    return false;
  }

  @Override
  public String toString() {
    return "feature [" + id + "]";
  }

  /**
   * Builds a requirement string for the provided feature.
   *
   * @param feature the feature to get a requirement string for
   * @return the corresponding requirement string
   */
  public static String toRequirement(Feature feature) {
    final String vstr = feature.getVersion();

    return String.format(
        JsonFeature.FEATURE_OSGI_REQUIREMENT_FORMAT, feature.getName(), vstr, vstr);
  }
}
