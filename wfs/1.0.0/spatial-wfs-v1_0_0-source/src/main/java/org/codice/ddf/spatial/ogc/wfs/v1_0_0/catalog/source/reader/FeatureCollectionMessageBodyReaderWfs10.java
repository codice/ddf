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
package org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.source.reader;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.codice.ddf.spatial.ogc.wfs.catalog.source.reader.FeatureCollectionMessageBodyReader;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.converter.impl.FeatureCollectionConverterWfs10;

@Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
@Provider
public class FeatureCollectionMessageBodyReaderWfs10 extends FeatureCollectionMessageBodyReader{
	
	public FeatureCollectionMessageBodyReaderWfs10(){
		super();
		featureCollectionConverter = new FeatureCollectionConverterWfs10();
        featureCollectionConverter.setFeatureConverterMap(featureConverterMap);
        xstream.registerConverter(featureCollectionConverter);
	}
}
