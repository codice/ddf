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
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.converter.impl;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import java.util.HashMap;
import java.util.Map;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class works in conjunction with XStream to convert a {@link WfsFeatureCollection} to XML
 * according to the GML 3.1.1 spec. It will also convert respective XML into a {@link
 * WfsFeatureCollection}.
 */
public class FeatureCollectionConverterWfs11 implements Converter {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FeatureCollectionConverterWfs11.class);

  private static final String FEATURE_MEMBER_ELEMENT = "featureMember";

  private static final String FEATURE_MEMBERS_ELEMENT = "featureMembers";

  private Map<String, FeatureConverter> featureConverterMap = new HashMap<>();

  @Override
  public boolean canConvert(Class clazz) {
    if (!WfsFeatureCollection.class.isAssignableFrom(clazz)) {
      LOGGER.debug("Cannot convert: {}", clazz.getName());
      return false;
    }
    return true;
  }

  @Override
  public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    WfsFeatureCollection featureCollection = new WfsFeatureCollection();
    while (reader.hasMoreChildren()) {
      reader.moveDown();
      String nodeName = reader.getNodeName();

      if (FEATURE_MEMBERS_ELEMENT.equals(nodeName)) {
        unmarshalFeatureMembers(reader, context, featureCollection);
      }

      if (FEATURE_MEMBER_ELEMENT.equals(nodeName)) {
        unmarshalFeatureMember(reader, context, featureCollection);
      }
      reader.moveUp();
    }
    return featureCollection;
  }

  private void unmarshalFeatureMember(
      HierarchicalStreamReader reader,
      UnmarshallingContext context,
      WfsFeatureCollection featureCollection) {
    reader.moveDown();
    featureCollection
        .getFeatureMembers()
        .add(
            (Metacard)
                context.convertAnother(
                    null, MetacardImpl.class, featureConverterMap.get(reader.getNodeName())));
    reader.moveUp();
  }

  private void unmarshalFeatureMembers(
      HierarchicalStreamReader reader,
      UnmarshallingContext context,
      WfsFeatureCollection featureCollection) {
    while (reader.hasMoreChildren()) {
      unmarshalFeatureMember(reader, context, featureCollection);
    }
  }

  public void setFeatureConverterMap(Map<String, FeatureConverter> featureConverterMap) {
    this.featureConverterMap = featureConverterMap;
  }
}
