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
package ddf.catalog.source.solr.api.impl;

import java.util.List;
import org.codice.solr.factory.SolrCollectionConfiguration;
import org.codice.solr.factory.SolrConfigurationData;

public class SolrCollectionConfigurationImpl implements SolrCollectionConfiguration {
  private String configurationName;

  private int defaultNumShards;

  private List<SolrConfigurationData> solrConfigurationData;

  public SolrCollectionConfigurationImpl(
      String configurationName,
      int defaultNumShards,
      List<SolrConfigurationData> configurationData) {
    setConfigurationName(configurationName);
    setDefaultNumShards(defaultNumShards);
    setSolrConfigurationData(configurationData);
  }

  @Override
  public String getConfigurationName() {
    return configurationName;
  }

  @Override
  public int getDefaultNumShards() {
    return defaultNumShards;
  }

  @Override
  public List<SolrConfigurationData> getSolrConfigurationData() {
    return solrConfigurationData;
  }

  public void setConfigurationName(String configurationName) {
    this.configurationName = configurationName;
  }

  public void setDefaultNumShards(int defaultNumShards) {
    this.defaultNumShards = defaultNumShards;
  }

  public void setSolrConfigurationData(List<SolrConfigurationData> solrConfigurationData) {
    this.solrConfigurationData = solrConfigurationData;
  }
}
