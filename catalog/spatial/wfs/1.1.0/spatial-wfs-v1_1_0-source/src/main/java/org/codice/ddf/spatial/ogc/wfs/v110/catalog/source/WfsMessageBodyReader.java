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
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.source;

import ddf.catalog.data.Metacard;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import net.opengis.wfs.v_1_1_0.FeatureTypeType;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.message.api.FeatureTransformationService;
import org.codice.ddf.spatial.ogc.wfs.catalog.message.api.WfsMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WfsMessageBodyReader implements MessageBodyReader<WfsFeatureCollection> {
  private WfsMetadata<FeatureTypeType> wfsMetadata;

  private FeatureTransformationService featureTransformationService;

  private static final Logger LOGGER = LoggerFactory.getLogger(WfsMessageBodyReader.class);

  public WfsMessageBodyReader(
      FeatureTransformationService featureTransformationService,
      WfsMetadata<FeatureTypeType> wfsMetadata) {
    this.featureTransformationService = featureTransformationService;
    this.wfsMetadata = wfsMetadata;
  }

  @Override
  public boolean isReadable(
      Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
    if (!WfsFeatureCollection.class.isAssignableFrom(clazz)) {
      LOGGER.debug("{} class is not readable", clazz);
      return false;
    }

    return true;
  }

  @Override
  public WfsFeatureCollection readFrom(
      Class<WfsFeatureCollection> aClass,
      Type type,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> multivaluedMap,
      InputStream inputStream) {

    List<Metacard> featureMembers = featureTransformationService.apply(inputStream, wfsMetadata);

    WfsFeatureCollection result = new WfsFeatureCollection();
    result.setFeatureMembers(featureMembers);
    return result;
  }
}
