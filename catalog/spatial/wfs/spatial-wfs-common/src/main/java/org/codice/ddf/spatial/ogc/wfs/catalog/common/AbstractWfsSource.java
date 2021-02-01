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
package org.codice.ddf.spatial.ogc.wfs.catalog.common;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.util.impl.MaskableImpl;
import java.util.List;
import java.util.function.Predicate;
import javax.xml.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWfsSource extends MaskableImpl
    implements FederatedSource, ConnectedSource, ConfiguredService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWfsSource.class);

  protected static final String CERT_ALIAS_KEY = "certAlias";

  protected static final String KEYSTORE_PATH_KEY = "keystorePath";

  protected static final String SSL_PROTOCOL_KEY = "sslProtocol";

  private static final String DEFAULT_SSL_PROTOCOL = "TLSv1.2";

  protected String certAlias;

  protected String keystorePath;

  protected String sslProtocol = DEFAULT_SSL_PROTOCOL;

  public String getCertAlias() {
    return certAlias;
  }

  public void setCertAlias(String certAlias) {
    this.certAlias = certAlias;
  }

  public String getKeystorePath() {
    return keystorePath;
  }

  public void setKeystorePath(String keystorePath) {
    this.keystorePath = keystorePath;
  }

  public String getSslProtocol() {
    return sslProtocol;
  }

  public void setSslProtocol(String sslProtocol) {
    this.sslProtocol = sslProtocol;
  }

  /**
   * If a MetacardMapper cannot be found or there is no mapping for the incomingPropertyName, return
   * null. This will cause a query to be constructed without an AbstractSortingClause.
   */
  protected String mapSortByPropertyName(
      QName featureType, String incomingPropertyName, List<MetacardMapper> metacardMapperList) {
    if (featureType == null || incomingPropertyName == null || metacardMapperList == null) {
      return null;
    }
    if (LOGGER.isDebugEnabled()) {
      metacardMapperList.forEach(
          m -> {
            LOGGER.debug(
                "Sorting: Mapper: featureType {}, mapped property for {} : {}",
                m.getFeatureType(),
                incomingPropertyName,
                m.getFeatureProperty(incomingPropertyName));
          });
      LOGGER.debug(
          "Mapping sort property: featureType {}, incomingPropertyName {}",
          featureType,
          incomingPropertyName);
    }
    MetacardMapper metacardToFeaturePropertyMapper =
        lookupMetacardAttributeToFeaturePropertyMapper(featureType, metacardMapperList);
    String mappedPropertyName = null;

    if (metacardToFeaturePropertyMapper != null) {

      if (StringUtils.equals(Result.TEMPORAL, incomingPropertyName)
          || StringUtils.equals(Metacard.EFFECTIVE, incomingPropertyName)) {
        mappedPropertyName =
            StringUtils.defaultIfBlank(
                metacardToFeaturePropertyMapper.getSortByTemporalFeatureProperty(), null);
      } else if (StringUtils.equals(Result.RELEVANCE, incomingPropertyName)) {
        mappedPropertyName =
            StringUtils.defaultIfBlank(
                metacardToFeaturePropertyMapper.getSortByRelevanceFeatureProperty(), null);
      } else if (StringUtils.equals(Result.DISTANCE, incomingPropertyName)) {
        mappedPropertyName =
            StringUtils.defaultIfBlank(
                metacardToFeaturePropertyMapper.getSortByDistanceFeatureProperty(), null);
      } else {
        mappedPropertyName =
            metacardToFeaturePropertyMapper.getFeatureProperty(incomingPropertyName);
      }
    }

    LOGGER.debug("mapped sort property from {} to {}", incomingPropertyName, mappedPropertyName);
    return mappedPropertyName;
  }

  protected MetacardMapper lookupMetacardAttributeToFeaturePropertyMapper(
      QName featureType, List<MetacardMapper> metacardMapperList) {

    final Predicate<MetacardMapper> matchesFeatureType =
        mapper -> mapper.getFeatureType().equals(featureType.toString());
    return metacardMapperList.stream()
        .filter(matchesFeatureType)
        .findAny()
        .orElseGet(
            () -> {
              LOGGER.debug("Could not find a MetacardMapper for featureType {}.", featureType);
              return null;
            });
  }
}
