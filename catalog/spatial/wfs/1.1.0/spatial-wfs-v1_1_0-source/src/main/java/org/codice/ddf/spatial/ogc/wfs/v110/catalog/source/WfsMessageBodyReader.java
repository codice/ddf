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

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Supplier;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import net.opengis.wfs.v_1_1_0.FeatureTypeType;
import org.codice.ddf.spatial.ogc.wfs.catalog.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.featuretransformer.FeatureTransformationService;
import org.codice.ddf.spatial.ogc.wfs.featuretransformer.WfsMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WfsMessageBodyReader implements MessageBodyReader<WfsFeatureCollection> {
  private Supplier<WfsMetadata<FeatureTypeType>> wfsMetadataSupplier;

  private FeatureTransformationService featureTransformationService;

  private static final Logger LOGGER = LoggerFactory.getLogger(WfsMessageBodyReader.class);

  public WfsMessageBodyReader(
      FeatureTransformationService featureTransformationService,
      Supplier<WfsMetadata<FeatureTypeType>> wfsMetadataSupplier) {
    this.featureTransformationService = featureTransformationService;
    this.wfsMetadataSupplier = wfsMetadataSupplier;
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
    return featureTransformationService.apply(inputStream, wfsMetadataSupplier.get());
  }
}
