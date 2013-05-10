/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.service.kml;

import java.io.Serializable;
import java.util.Map;

import ddf.catalog.data.Metacard;


public interface KMLEntryTransformer {

	/**
	 * Meant to return the main snippet of the KML representation of the metadata
	 * record without styling information included and without KML and Document
	 * element tags. Styling information can be included but is not recommended. Styling should be passed separately 
	 * in the {@link #getKMLStyle() } method.
	 * <p>
	 * <pre>
	 * {@code 
	 * <KML> 		---> not included
	 * <Document> 	---> not included
	 * <Placemark>  ---> What is returned from this method
	 * ...			---> What is returned from this method
	 * </Placemark  ---> What is returned from this method
	 * </Document>	---> not included
	 * </KML>		---> not included
	 * }
	 * </pre>
	 * </p>
	 * @param entry
	 *            CatalogEntry that is used as the object to be transformed.
	 * @param arguments
	 *            An optional map to be used by the implementers for any extra
	 *            parameters that make sense for a specific metadata record
	 *            content type. Can be null.
	 * @return String of partial KML representation of the given metadata record
	 *         (CatalogEntry)
	 */
	public String getKMLContent(Metacard entry, Map<String, Serializable> arguments) ;

	/**
	 * Returns snippets of the styling information that is to be used in
	 * conjunction with the results that are returned in getKMLContent.
	 * <br/>
	 * Example:
	 * 
	 * <pre>
	 * {@code
	 * <Style id="icon">
	 *  <IconStyle>
	 * 	 <scale>0.0</scale>
	 *  </IconStyle>
	 * </Style>
	 * }
	 * 
	 * </pre>
	 * 
	 * @return String of KML Styling
	 */
	public String getKMLStyle() ;
	
}
