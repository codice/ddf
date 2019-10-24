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
package org.codice.ddf.spatial.ogc.wfs.featuretransformer.impl;

import ddf.catalog.data.Metacard;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.wfs.featuretransformer.FeatureTransformer;
import org.codice.ddf.spatial.ogc.wfs.featuretransformer.WfsMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WfsTransformerProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(WfsTransformerProcessor.class);
  private List<FeatureTransformer> transformerServiceList;

  public WfsTransformerProcessor(List<FeatureTransformer> transformerServiceList) {
    this.transformerServiceList = transformerServiceList;
  }

  public void setActiveFeatureMemberNodeName(WfsMetadata metadata, String featureMemberNodeName) {
    metadata.setActiveFeatureMemberNodeName(featureMemberNodeName);
  }

  public Optional<Metacard> apply(String featureMember, WfsMetadata metadata) {

    if (StringUtils.isEmpty(featureMember)) {
      return Optional.empty();
    }

    for (FeatureTransformer featureTransformer : transformerServiceList) {
      try (InputStream featureMemberInputStream =
          new BufferedInputStream(new ByteArrayInputStream(featureMember.getBytes()))) {
        Optional<Metacard> metacardOptional =
            featureTransformer.apply(featureMemberInputStream, metadata);

        if (metacardOptional.isPresent()) {
          return metacardOptional;
        }
      } catch (IOException e) {
        LOGGER.debug(
            "Error transforming feature member:{}, with feature transformer: {}",
            featureMember,
            featureTransformer);
      }
    }

    return Optional.empty();
  }
}
