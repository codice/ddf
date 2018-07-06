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
package org.codice.ddf.config.model;

import java.util.Optional;
import java.util.stream.Stream;
import org.codice.ddf.config.ConfigGroup;
import org.codice.ddf.config.ConfigType;

@ConfigType
public interface MimeTypeConfig extends ConfigGroup {
  @Override
  public default Class<MimeTypeConfig> getType() {
    return MimeTypeConfig.class;
  }

  /**
   * Gets the name of this mime type configuration.
   *
   * @return the name of this mime type config
   */
  public String getName();

  /**
   * Gets the priority of this mime type configuration. The higher the number the higher the
   * priority, meaning the corresponding mime type resolver is invoked earlier.
   *
   * @return the priority of this mime type config
   */
  public int getPriority();

  /**
   * Gets the file extension for the specific mime type. For example, if the mime type is image/nitf
   * a file extension of .nitf would be returned.
   *
   * @param mimeType the mime type to get a corresponding file extension for
   * @return the corresponding file extension, including the period in the extension or empty if
   *     none configured for the given mime type
   */
  public Optional<String> getExtensionFor(String mimeType);

  /**
   * Gets the mime type for the specified file extension.
   *
   * @param extension the file extension to get a corresponding mime type
   * @return the corresponding mime type or empty if none configured for the given file extension
   */
  public Optional<String> getMimeTypeFor(String extension);

  /**
   * Gets all mime type mappings defined as part of this configuration.
   *
   * @return a stream of all mime type mappings configured as part of this config
   */
  public Stream<Mapping> mappings();

  public interface Mapping {
    public String getExtension();

    public String getMimeType();
  }
}
