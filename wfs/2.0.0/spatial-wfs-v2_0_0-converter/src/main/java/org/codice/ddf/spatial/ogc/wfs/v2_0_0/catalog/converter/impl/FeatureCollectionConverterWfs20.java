/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/

package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.converter.impl;

import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.FeatureCollectionConverter;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.WfsConstants;

/**
 * This class works in conjunction with XStream to convert a {@link WfsFeatureCollection} v2.0 to XML
 * according to the GML 2.1.2 spec. It will also convert respective XML into a
 * {@link WfsFeatureCollection}.
 * 
 * @author kcwire
 * 
 */

public class FeatureCollectionConverterWfs20 extends FeatureCollectionConverter {
    
    public FeatureCollectionConverterWfs20() {
        super();
        prefixToUriMapping.put(WfsConstants.WFS_NAMESPACE_PREFIX, WfsConstants.WFS_2_0_NAMESPACE);
        prefixToUriMapping.put(WfsConstants.GML_PREFIX, WfsConstants.GML_3_2_NAMESPACE);
    }
}
