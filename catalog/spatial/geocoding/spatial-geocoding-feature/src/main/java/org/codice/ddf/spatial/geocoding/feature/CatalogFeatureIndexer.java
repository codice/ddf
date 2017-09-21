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
package org.codice.ddf.spatial.geocoding.feature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.security.service.SecurityServiceException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.spatial.geocoding.FeatureExtractionException;
import org.codice.ddf.spatial.geocoding.FeatureExtractor;
import org.codice.ddf.spatial.geocoding.FeatureIndexer;
import org.codice.ddf.spatial.geocoding.FeatureIndexingException;

public class CatalogFeatureIndexer implements FeatureIndexer {

  private Security security = Security.getInstance();

  private CatalogFramework catalogFramework;

  public void setCatalogFramework(CatalogFramework catalogFramework) {
    this.catalogFramework = catalogFramework;
  }

  @Override
  public void updateIndex(
      String resource, FeatureExtractor featureExtractor, boolean create, IndexCallback callback)
      throws FeatureExtractionException, FeatureIndexingException {

    AtomicInteger count = new AtomicInteger(0);

    final FeatureExtractor.ExtractionCallback extractionCallback =
        (feature) -> {
          Metacard metacard = new MetacardImpl();

          String countryCode = (String) feature.getAttribute("ISO_A3");
          metacard.setAttribute(new AttributeImpl(Metacard.TITLE, countryCode));

          WKTWriter writer = new WKTWriter();
          String wkt = writer.write((Geometry) feature.getDefaultGeometry());
          metacard.setAttribute(new AttributeImpl(Metacard.GEOGRAPHY, wkt));

          List<Serializable> tags = new ArrayList<>();
          tags.add("gazetteer");
          metacard.setAttribute(new AttributeImpl(Metacard.TAGS, tags));

          try {
            security.runWithSubjectOrElevate(
                () -> catalogFramework.create(new CreateRequestImpl(Arrays.asList(metacard))));
          } catch (SecurityServiceException | InvocationTargetException e) {
            throw new FeatureIndexingException(e.getMessage());
          }

          callback.indexed(count.incrementAndGet());
        };

    featureExtractor.pushFeaturesToExtractionCallback(resource, extractionCallback);
  }
}
