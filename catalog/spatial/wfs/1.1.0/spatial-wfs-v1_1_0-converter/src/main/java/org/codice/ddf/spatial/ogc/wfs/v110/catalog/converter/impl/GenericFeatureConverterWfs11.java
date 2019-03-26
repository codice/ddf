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

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class works in conjunction with XStream to convert a {@link Metacard} to XML according to
 * the GML 3.1.1 spec. It will also convert respective XML into a Metacard.
 */
public class GenericFeatureConverterWfs11 extends AbstractFeatureConverterWfs11 {

  private static final String ID = "id";

  private static final Logger LOGGER = LoggerFactory.getLogger(GenericFeatureConverterWfs11.class);

  public GenericFeatureConverterWfs11(String srs) {
    super(srs);
  }

  public GenericFeatureConverterWfs11(MetacardMapper metacardMapper) {
    super(metacardMapper);
  }

  /**
   * Method to determine if this converter knows how to convert the specified Class.
   *
   * @param clazz the class to check
   */
  @Override
  public boolean canConvert(Class clazz) {
    return Metacard.class.isAssignableFrom(clazz);
  }

  @Override
  public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
    throw new UnsupportedOperationException();
  }

  /**
   * This method will unmarshal an XML instance of a "gml:member" to a {@link Metacard}.
   *
   * @param hreader the stream reader responsible for reading this xml doc
   * @param context a reference back to the Xstream unmarshalling context. Allows you to call
   *     "convertAnother" which will lookup other registered converters.
   */
  @Override
  public Object unmarshal(HierarchicalStreamReader hreader, UnmarshallingContext context) {

    if ("featureMember".equals(hreader.getNodeName())) {
      hreader.moveDown();
    }
    LOGGER.trace("Entering: {} : unmarshal", this.getClass().getName());
    // Workaround for Xstream which seems to be having issues involving attributes with namespaces,
    // in that it cannot fetch the attributes value directly by name.
    String id = null;
    int count = hreader.getAttributeCount();
    for (int i = 0; i < count; i++) {
      if (hreader.getAttributeName(i) != null && hreader.getAttributeName(i).equals(ID)) {
        id = hreader.getAttribute(i);
      }
    }

    MetacardImpl mc;
    if (metacardType != null) {
      mc = (MetacardImpl) createMetacardFromFeature(hreader, metacardType);
    } else {
      throw new IllegalArgumentException(
          "No MetacardType registered on the FeatureConverter.  Unable to to convert features to metacards.");
    }
    if (StringUtils.isNotBlank(id)) {
      mc.setId(id);
    } else {
      LOGGER.debug("Feature id is blank.  Unable to set metacard id.");
    }

    mc.setSourceId(sourceId);

    // set some default values that we can't get from a generic
    // featureCollection if they are not already set
    Date genericDate = new Date();
    if (mc.getEffectiveDate() == null) {
      mc.setEffectiveDate(genericDate);
    }
    if (mc.getAttribute(Core.CREATED).getValue() == null) {
      mc.setCreatedDate(genericDate);
    }
    if (mc.getAttribute(Core.MODIFIED).getValue() == null) {
      mc.setModifiedDate(genericDate);
    }
    if (StringUtils.isBlank(mc.getTitle())) {
      mc.setTitle(id);
    }

    mc.setContentTypeName(metacardType.getName());
    try {
      mc.setTargetNamespace(new URI(WfsConstants.NAMESPACE_URN_ROOT + metacardType.getName()));
    } catch (URISyntaxException e) {
      LOGGER.debug(
          "Unable to set Target Namespace on metacard: {}, Exception {}",
          WfsConstants.NAMESPACE_URN_ROOT + metacardType.getName(),
          e);
    }

    return mc;
  }

  public void setSourceId(final String sourceId) {
    this.sourceId = sourceId;
  }
}
