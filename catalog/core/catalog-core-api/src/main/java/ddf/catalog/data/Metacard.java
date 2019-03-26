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
package ddf.catalog.data;

import ddf.catalog.data.types.Associations;
import ddf.catalog.data.types.Core;
import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link Metacard} is a container for Metadata.
 *
 * <p>It is modeled after the OGC SimpleFeature and does two additional things:
 *
 * <ol>
 *   <li/>Represents a resource.
 *   <li/>Contains or can be represented as XML metadata.
 * </ol>
 *
 * This interface does not extend the OGC SimpleFeature interface due to added complexity that was
 * deemed to conflict with the goal of simplicity for this API. <br>
 * The {@link Metacard} can include key/value-style {@link Attribute}s, and includes several
 * standard properties: id, title, document, location, source name, dates(created, modified,
 * effective, expiration), and thumbnail.
 */
public interface Metacard extends Serializable {

  // Names of Queryable Properties
  /**
   * Attribute name for querying all text of all textual properties of a {@link Metacard}. <em>Used
   * in Queries only</em>
   */
  String ANY_TEXT = "anyText";

  /**
   * Attribute name for querying all geometries of all geometric properties of a {@link Metacard}
   * <em>Used in Queries only</em>
   */
  String ANY_GEO = "anyGeo";

  /**
   * Attribute name for querying all dates of all temporal properties of a {@link Metacard}.
   * <em>Used in Queries only</em>
   */
  String ANY_DATE = "anyDate";

  /**
   * Attribute name for the ID of the source where the {@link Metacard} is cataloged.
   *
   * @deprecated - instead reference {@link ddf.catalog.data.types.Core.SOURCE_ID}
   */
  String SOURCE_ID = Core.SOURCE_ID;

  /**
   * Attribute name for querying the metadata content type of a {@link Metacard}.
   *
   * @see MetacardType
   */
  String CONTENT_TYPE = "metadata-content-type";

  /** Attribute name for querying the version of the metadata content type of a {@link Metacard}. */
  String CONTENT_TYPE_VERSION = "metadata-content-type-version";

  /**
   * Attribute name for querying the target namespace of the metadata content type of a {@link
   * Metacard}.
   */
  String TARGET_NAMESPACE = "metadata-target-namespace";

  // Stored Metacard properties
  /**
   * {@link Attribute} name for accessing the ID of the {@link Metacard}. <br>
   * Every {@link ddf.catalog.source.Source} is required to return this attribute.
   *
   * @deprecated - instead reference {@link ddf.catalog.data.types.Core.ID}.
   */
  String ID = Core.ID;

  /**
   * {@link Attribute} name for accessing the tags of the {@link Metacard}.
   *
   * @deprecated - instead reference {@link ddf.catalog.data.types.Core.METACARD_TAGS}.
   */
  String TAGS = Core.METACARD_TAGS;

  /**
   * {@link Attribute} name for accessing the title of the {@link Metacard}.
   *
   * @deprecated - instead reference {@link ddf.catalog.data.types.Core.TITLE}.
   */
  String TITLE = Core.TITLE;

  /**
   * {@link Attribute} name for accessing the XML metadata for this {@link Metacard}.
   *
   * @deprecated - instead reference {@link ddf.catalog.data.types.Core.METADATA}.
   */
  String METADATA = Core.METADATA;

  /**
   * {@link Attribute} name for accessing the location for this {@link Metacard}.
   *
   * @deprecated - instead reference {@link ddf.catalog.data.types.Core.LOCATION}.
   */
  String GEOGRAPHY = Core.LOCATION;

  /**
   * {@link Attribute} name for accessing the date/time this {@link Metacard} was created.
   *
   * @deprecated - This concept has changed. "created" now refers to the resource created date
   *     instead of the metacard created date. This can be referenced with {@link
   *     ddf.catalog.data.types.Core.CREATED}. The metacard created date can now be referenced with
   *     {@link ddf.catalog.data.types.Core.METACARD_CREATED}.
   */
  String CREATED = Core.CREATED;

  /**
   * {@link Attribute} name for accessing the date/time this {@link Metacard} was last modified.
   *
   * @deprecated - This concept has changed. "modified" now refers to the resource modified date
   *     instead of the metacard modified date. This can be referenced with {@link
   *     ddf.catalog.data.types.Core.MODIFIED}. The metacard modified date can now be referenced
   *     with {@link ddf.catalog.data.types.Core.METACARD_MODIFIED}.
   */
  String MODIFIED = Core.MODIFIED;

  /**
   * {@link Attribute} name for accessing the date/time the {@link Metacard} is no longer valid and
   * could be removed.
   *
   * @deprecated - instead reference {@link ddf.catalog.data.types.Core.EXPIRATION}
   */
  String EXPIRATION = Core.EXPIRATION;

  /**
   * {@link Attribute} name for accessing the date/time the {@link Metacard} was last known to be
   * up-to-date.
   *
   * @deprecated - excluded from catalog taxonomy
   */
  String EFFECTIVE = "effective";

  /**
   * {@link Attribute} name for accessing the point of contact to the product this {@link Metacard}
   * represents.
   *
   * @deprecated - excluded from catalog taxonomy
   */
  String POINT_OF_CONTACT = "point-of-contact";

  /**
   * {@link Attribute} name for accessing the {@link URI} reference to the product this {@link
   * Metacard} represents.
   *
   * @deprecated - instead reference {@link ddf.catalog.data.types.Core.RESOURCE_URI}
   */
  String RESOURCE_URI = Core.RESOURCE_URI;

  /**
   * {@link Attribute} name for accessing the resource download URL for the product this {@link
   * Metacard} represents.
   *
   * @deprecated - instead reference {@link ddf.catalog.data.types.Core.RESOURCE_DOWNLOAD_URL}
   */
  String RESOURCE_DOWNLOAD_URL = Core.RESOURCE_DOWNLOAD_URL;

  /**
   * {@link Attribute} name for accessing the size (in bytes) of the product this {@link Metacard}
   * represents.
   *
   * @deprecated - instead reference {@link ddf.catalog.data.types.Core.RESOURCE_SIZE}
   */
  String RESOURCE_SIZE = Core.RESOURCE_SIZE;

  /**
   * {@link Attribute} name for accessing the thumbnail image of the product this {@link Metacard}
   * represents. The thumbnail must be of MIME Type <code>image/jpeg</code> and 128 kilobytes or
   * less.
   *
   * @deprecated - instead reference {@link ddf.catalog.data.types.Core.THUMBNAIL}
   */
  String THUMBNAIL = Core.THUMBNAIL;

  /**
   * {@link Attribute} name for accessing the security relevant markings on the product that this
   * {@link Metacard} represents.
   *
   * @deprecated Not to be used anymore, replaced with SECURITY attribute.
   */
  String SECURITY_MATCH_ALL = "security-match-all";

  /**
   * {@link Attribute} name for accessing the security relevant markings on the product that this
   * {@link Metacard} represents.
   *
   * @deprecated Not to be used anymore, replaced with SECURITY attribute.
   */
  String SECURITY_MATCH_ONE = "security-match-one";

  /**
   * {@link Attribute} name for accessing the security relevant markings on the product that this
   * {@link Metacard} represents.
   *
   * @since DDF-2.2.0.RC1
   * @deprecated - excluded from catalog taxonomy
   */
  String SECURITY = "security";

  /**
   * {@link Attribute} description associated with the {@link Metacard}
   *
   * @since DDF-2.8.0
   * @deprecated - instead reference {@link ddf.catalog.data.types.Core.DESCRIPTION}
   */
  String DESCRIPTION = Core.DESCRIPTION;

  /** The default tag type for all metacards */
  String DEFAULT_TAG = "resource";

  /**
   * {@link Attribute} algorithm used to calculate the checksum on the {@link Metacard#RESOURCE_URI}
   * for local resources
   *
   * @since DDF-2.9.0
   * @deprecated - instead reference {@link ddf.catalog.data.types.Core.CHECKSUM_ALGORITHM}
   */
  String CHECKSUM_ALGORITHM = Core.CHECKSUM_ALGORITHM;

  /**
   * {@link Attribute} name for related {@link Metacard} ids.
   *
   * @since DDF-2.9.0
   * @deprecated - instead reference {@link ddf.catalog.data.types.Associations.RELATED}
   */
  public static final String RELATED = Associations.RELATED;

  /**
   * {@link Attribute} name for derived {@link Metacard} ids.
   *
   * @since DDF-2.9.0
   * @deprecated - instead reference {@link ddf.catalog.data.types.Associations.DERIVED}
   */
  public static final String DERIVED = Associations.DERIVED;

  /**
   * {@link Attribute} checksum value for the {@link Metacard#RESOURCE_URI}
   *
   * @since DDF-2.9.0
   * @deprecated - instead reference {@link ddf.catalog.data.types.Core.CHECKSUM}
   */
  String CHECKSUM = Core.CHECKSUM;

  /**
   * {@link Attribute} that provides URIs for derived formats of the {@literal
   * Metacard.RESOURCE_URI}
   *
   * @deprecated - instead reference {@link ddf.catalog.data.types.Core.DERIVED_RESOURCE_URI}
   */
  String DERIVED_RESOURCE_URI = Core.DERIVED_RESOURCE_URI;

  /**
   * {@link Attribute} name for accessing the derived resource download URL for the derived products
   * of this {@link Metacard}. <br>
   *
   * @deprecated - instead reference {@link
   *     ddf.catalog.data.types.Core.DERIVED_RESOURCE_DOWNLOAD_URL}
   */
  String DERIVED_RESOURCE_DOWNLOAD_URL = Core.DERIVED_RESOURCE_DOWNLOAD_URL;

  /**
   * Returns {@link Attribute} for given attribute name.
   *
   * @param name name of attribute
   * @return {@link Attribute} for given name, or <code>null</code> if not available
   */
  Attribute getAttribute(String name);

  /**
   * Sets {@link Attribute} with new attribute.
   *
   * @param attribute new {@link Attribute} to set. If the getValue() call on the passed in
   *     attribute returns <code>null</code>, the attribute will be removed from this {@link
   *     Metacard}.
   */
  void setAttribute(Attribute attribute);

  /**
   * Return the {@link MetacardType} of this {@link Metacard}.
   *
   * @return {@link MetacardType} - the type of this {@link Metacard}, MetacardType is required and
   *     must not be <code>null</code>
   */
  MetacardType getMetacardType();

  // Shortcuts

  /**
   * Returns the ID of this {@link Metacard}. <br>
   * Convenience method for <code>
   * {@link #getAttribute getAttribute}({@link Metacard#ID})
   * </code>
   *
   * @return unique identifier of the Metacard
   * @see Metacard#ID
   */
  String getId();

  /**
   * Returns the tags associated with this metacard.
   *
   * @return Returns the TAGS attribute values if it exists otherwise return null
   */
  default Set<String> getTags() {
    Attribute attribute = getAttribute(TAGS);
    if (attribute == null || attribute.getValue() == null) {
      return new HashSet<>();
    } else {
      return new HashSet<>(
          getAttribute(TAGS)
              .getValues()
              .stream()
              .map(String::valueOf)
              .collect(Collectors.toList()));
    }
  }

  /**
   * Returns the metadata associated with this {@link Metacard}. <br>
   * Convenience method for <code>
   * {@link #getAttribute getAttribute}({@link Metacard#METADATA})
   * </code>
   *
   * @return XML metadata
   * @see Metacard#METADATA
   */
  String getMetadata();

  /**
   * Returns the date/time this {@link Metacard} was created. <br>
   * Convenience method for <code>
   * {@link #getAttribute getAttribute}({@link Metacard#CREATED})
   * </code>
   *
   * @return {@link Date} - when this {@link Metacard} was created.
   * @see Metacard#CREATED
   * @deprecated - Do not use. Concept of created date has changed. Instead access dates using
   *     catalog taxonomy fields.
   */
  Date getCreatedDate();

  /**
   * Returns the date/time this {@link Metacard} was last modifed. <br>
   * Convenience method for <code>
   * {@link #getAttribute getAttribute}({@link Metacard#MODIFIED})
   * </code>
   *
   * @return {@link Date} - when this {@link Metacard} was last modified.
   * @see Metacard#MODIFIED
   * @deprecated - Do not use. Concept of modified date has changed. Instead access dates using
   *     catalog taxonomy fields.
   */
  Date getModifiedDate();

  /**
   * Returns the date/time this {@link Metacard} is no longer valid and could be removed. <br>
   * Convenience method for <code>
   * {@link #getAttribute getAttribute}({@link Metacard#EXPIRATION})
   * </code>
   *
   * @return {@link Date} - when this {@link Metacard} expires and should be removed from any
   *     stores.
   * @see Metacard#EXPIRATION
   */
  Date getExpirationDate();

  /**
   * Returns the date/time this {@link Metacard} was last known to be valid. <br>
   * Convenience method for <code>
   * {@link #getAttribute getAttribute}({@link Metacard#EFFECTIVE})
   * </code>
   *
   * @return {@link Date} - when the information represented by this {@link Metacard} was last known
   *     to be valid.
   * @see Metacard#EFFECTIVE
   */
  Date getEffectiveDate();

  /**
   * Returns the WKT representation of the geometry. <br>
   * Convenience method for <code>
   * {@link #getAttribute getAttribute}({@link Metacard#GEOGRAPHY})
   * </code>
   *
   * @return {@link String} - WKT-defined geospatial object, returns null if not applicable
   * @see Metacard#GEOGRAPHY
   */
  String getLocation();

  /** Returns the source ID of the source the metacard is located. <br> */
  String getSourceId();

  /**
   * Sets the source ID of the source the metacard is located. <br>
   *
   * @param sourceId unique name of source location of metacard
   */
  void setSourceId(String sourceId);

  /**
   * Returns the title of this {@link Metacard}. <br>
   * Convenience method for <code>
   * {@link #getAttribute getAttribute}({@link Metacard#TITLE})
   * </code>
   *
   * @return Title of the {@link Metacard}
   * @see Metacard#TITLE
   */
  String getTitle();

  /**
   * Get the value of this {@link Metacard}s Resource URI and in the form of a {@link URI} Object.
   * <br>
   * Convenience method for <code>
   * new URI({@link #getAttribute getAttribute}({@link Metacard#RESOURCE_URI}));
   * </code>
   *
   * @return {@link URI} - a {@link URI} representation of the {@link Metacard}'s {@link
   *     Metacard#RESOURCE_URI Resource URI} which itself is stored as a {@link
   *     ddf.catalog.data.AttributeType.AttributeFormat#STRING String
   *     ddf.catalog.data.AttributeType.AttributeFormat}
   */
  URI getResourceURI();

  /**
   * This is the size of the resource which may or may not contain a unit.
   *
   * @return {@link String} - {@link String} representation of the size
   */
  String getResourceSize();

  /**
   * Returns the thumbnail associated with this {@link Metacard}. <br>
   * Convenience method for <code>
   * {@link #getAttribute getAttribute}({@link Metacard#THUMBNAIL})
   * </code>
   *
   * @return thumbnail for the {@link Metacard}
   * @see Metacard#THUMBNAIL
   */
  byte[] getThumbnail();

  /**
   * Returns the name of the content type of the {@link Metacard}. <br>
   * Convenience method for <code>
   * {@link #getAttribute getAttribute}({@link Metacard#CONTENT_TYPE})
   * </code>
   *
   * @return name of content type of the {@link Metacard}
   * @see Metacard#CONTENT_TYPE
   */
  String getContentTypeName();

  /**
   * Returns the version of the content type of the {@link Metacard}. <br>
   * Convenience method for <code>
   * {@link #getAttribute getAttribute}({@link Metacard#CONTENT_TYPE_VERSION})
   * </code>
   *
   * @return version of content type of the {@link Metacard}
   * @see Metacard#CONTENT_TYPE_VERSION
   */
  String getContentTypeVersion();

  /**
   * Some types of metadata use different content types. If utilized, returns the {@link URI} of the
   * content type. <br>
   * Convenience method for <code>
   * {@link #getAttribute getAttribute}({@link Metacard#TARGET_NAMESPACE})
   * </code>
   *
   * @return {@link URI} - the sub-type, null if unused
   * @see Metacard#TARGET_NAMESPACE
   */
  URI getContentTypeNamespace();
}
