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
package rorg.codice.ddf.spatial.ogc.wfs.catalog.converte;

import com.thoughtworks.xstream.converters.Converter;
import ddf.catalog.data.MetacardType;

/**
 * Extension of the XStream <a href=
 * "http://xstream.codehaus.org/javadoc/com/thoughtworks/xstream/converters/Converter.html"
 * >Converter</a> interface
 *
 * <p>The FeatureConverter adds:
 * <li>ability to set SourceID which is required when converting features to {@link
 *     ddf.catalog.data.Metacard}s
 * <li>ability to set the {@link MetacardType} <br>
 */
public interface FeatureConverter extends Converter {

  /**
   * set the source ID on the converter to be used when converting features into metacards
   *
   * @param id
   */
  public void setSourceId(String id);

  /**
   * Get this converter's MetacardType
   *
   * @return
   */
  public MetacardType getMetacardType();

  /**
   * Set the MetacardType on the converter
   *
   * @param metacardType
   */
  public void setMetacardType(MetacardType metacardType);

  /**
   * Set the URL on the on converter
   *
   * @param wfsUrl
   */
  public void setWfsUrl(String wfsUrl);

  /**
   * Set the coordinate order, LAT_LON or LON_LAT, on the converter.
   *
   * @param coordinateOrder
   */
  public void setCoordinateOrder(String coordinateOrder);
}
