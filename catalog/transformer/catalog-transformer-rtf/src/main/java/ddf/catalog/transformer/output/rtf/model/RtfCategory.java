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
package ddf.catalog.transformer.output.rtf.model;

import ddf.catalog.data.Metacard;
import java.util.List;
import java.util.Map;

/**
 * Represents a grouping of (configured/desired) attributes that should be captured from a {@link
 * Metacard}.
 */
public interface RtfCategory {
  String getTitle();

  void setTitle(String title);

  List<String> getAttributes();

  void setAttributes(List<String> attributes);

  /**
   * This method creates a {@link Map} of {@link String} to {@link
   * ddf.catalog.transformer.output.rtf.model.ExportCategory.ExportValue} from supplied {@link
   * Metacard}. It formats {@link ddf.catalog.data.Attribute} that correspond to the list set.
   *
   * @param metacard {@link Metacard} from which the desired attributes will be retrieved
   * @return {@link Map} of {@link String}-{@link
   *     ddf.catalog.transformer.output.rtf.model.ExportCategory.ExportValue} pairs. With the key
   *     corresponding to the title of the current {@link RtfCategory}
   */
  Map<String, ExportCategory.ExportValue> toExportMap(Metacard metacard);
}
