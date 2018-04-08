/*
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.spatial.ogc.wfs.transformer.xstream;

import ddf.catalog.data.MetacardType;
import java.util.Optional;
import javax.xml.namespace.QName;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.FeatureTypeType;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverterFactory;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.GenericFeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;

public class XStreamWfs10FeatureTransformer extends XStreamWfsFeatureTransformer<FeatureTypeType> {

  @Override
  protected Optional<MetacardType> lookupFeatureMetacardType(
      String sourceId, FeatureTypeType featureType) {
    return metacardTypeRegistry.lookupMetacardTypeBySimpleName(
        sourceId, featureType.getName().getLocalPart());
  }

  @Override
  protected boolean canConvertFeature(
      FeatureConverterFactory featureConverterFactory, FeatureTypeType featureType) {
    return StringUtils.equalsIgnoreCase(
        featureType.getName().getLocalPart(), featureConverterFactory.getFeatureType());
  }

  @Override
  protected FeatureConverter getGenericFeatureConverter(MetacardMapper metacardMapper) {
    return new GenericFeatureConverter(metacardMapper);
  }

  @Override
  protected FeatureConverter getGenericFeatureConverter(FeatureTypeType featureType) {
    return new GenericFeatureConverter(featureType.getSRS());
  }

  @Override
  protected QName getFeatureTypeName(FeatureTypeType featureType) {
    return featureType.getName();
  }
}
