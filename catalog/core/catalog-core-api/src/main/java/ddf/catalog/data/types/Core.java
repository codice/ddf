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
package ddf.catalog.data.types;

import ddf.catalog.data.Metacard;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface Core {
  /**
   * {@link ddf.catalog.data.Attribute} checksum value for the {@link
   * ddf.catalog.data.Core#RESOURCE_URI}
   */
  String CHECKSUM = "checksum";

  /**
   * {@link ddf.catalog.data.Attribute} algorithm used to calculate the checksum on the {@link
   * ddf.catalog.data.Core#RESOURCE_URI} for local resources
   */
  String CHECKSUM_ALGORITHM = "checksum-algorithm";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the date/time this {@link
   * ddf.catalog.resource.Resource} was created. <br>
   */
  String CREATED = "created";

  /**
   * {@link ddf.catalog.data.Attribute} description associated with the {@link
   * ddf.catalog.data.Metacard}
   */
  String DESCRIPTION = "description";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the date/time the {@link
   * ddf.catalog.data.Metacard} is no longer valid and could be removed. <br>
   */
  String EXPIRATION = "expiration";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the ID of the {@link
   * ddf.catalog.data.Metacard}. <br>
   * Every {@link ddf.catalog.source.Source} is required to return this ddf.catalog.data.Attribute.
   */
  String ID = "id";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the language of the resource of this
   * {@link ddf.catalog.data.Metacard}.
   */
  String LANGUAGE = "language";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the location for this {@link
   * ddf.catalog.data.Metacard}. <br>
   */
  String LOCATION = "location";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the XML metadata for this {@link
   * ddf.catalog.data.Metacard}. <br>
   */
  String METADATA = "metadata";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the date/time this {@link
   * ddf.catalog.resource.Resource} was last modified. <br>
   */
  String MODIFIED = "modified";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the derived resource download URL for the
   * derived products of this {@link ddf.catalog.data.Metacard}. <br>
   * Uses original taxonomy to preserve backwards compatibility.
   */
  String DERIVED_RESOURCE_DOWNLOAD_URL = "resource.derived-download-url";

  /**
   * {@link ddf.catalog.data.Attribute} that provides URIs for derived formats of the {@literal
   * ddf.catalog.data.Core.RESOURCE_URI}
   */
  String DERIVED_RESOURCE_URI = "resource.derived-uri";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the resource download URL for the product
   * this {@link ddf.catalog.data.Metacard} represents. Typically, the framework will write this
   * attribute based on the {@link Core#RESOURCE_URI}, and it should not be set directly. <br>
   */
  String RESOURCE_DOWNLOAD_URL = "resource-download-url";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the size (in bytes) of the product this
   * {@link ddf.catalog.data.Metacard} represents. <br>
   */
  String RESOURCE_SIZE = "resource-size";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the {@link java.net.URI} reference to the
   * product this {@link ddf.catalog.data.Metacard} represents. <br>
   */
  String RESOURCE_URI = "resource-uri";

  /**
   * {@link ddf.catalog.data.Attribute} name for the ID of the source where the {@link
   * ddf.catalog.data.Metacard} is cataloged.
   */
  String SOURCE_ID = "source-id";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the thumbnail image of the product this
   * {@link ddf.catalog.data.Metacard} represents. The thumbnail must be of MIME Type <code>
   * image/jpeg</code> and 128 kilobytes or less. <br>
   */
  String THUMBNAIL = "thumbnail";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the title of the {@link
   * ddf.catalog.data.Metacard}. <br>
   */
  String TITLE = "title";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the generic type of the {@link Metacard}
   * resource. <br>
   */
  String DATATYPE = "datatype";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the creation date of the {@link
   * Metacard}. <br>
   */
  String METACARD_CREATED = "metacard.created";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the modified date of the {@link
   * Metacard}. <br>
   */
  String METACARD_MODIFIED = "metacard.modified";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the owner of the {@link Metacard}. <br>
   */
  String METACARD_OWNER = "metacard.owner";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the tags of the {@link Metacard}. <br>
   * Uses original taxonomy to preserve backwards compatibility.
   */
  String METACARD_TAGS = "metacard-tags";
}
