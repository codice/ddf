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
package org.codice.ddf.spatial.ogc.wfs.catalog.message;

import ddf.catalog.data.Metacard;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.codice.ddf.spatial.ogc.wfs.catalog.message.api.FeatureTransformer;
import org.codice.ddf.spatial.ogc.wfs.catalog.message.api.WfsMetadata;

public final class WfsTransformerProcessor {
  private List<FeatureTransformer> transformerServiceList;

  public WfsTransformerProcessor(List<FeatureTransformer> transformerServiceList) {
    this.transformerServiceList = transformerServiceList;
  }

  public Optional<Metacard> apply(String featureMemeber, WfsMetadata metadata) {

    for (FeatureTransformer featureTransformer : transformerServiceList) {
      InputStream featureMemberInputStream =
          new BufferedInputStream(new ByteArrayInputStream(featureMemeber.getBytes()));

      Optional<Metacard> metacardOptional =
          featureTransformer.apply(featureMemberInputStream, metadata);

      if (metacardOptional.isPresent()) {
        return metacardOptional;
      }
    }

    return Optional.empty();
  }
}
