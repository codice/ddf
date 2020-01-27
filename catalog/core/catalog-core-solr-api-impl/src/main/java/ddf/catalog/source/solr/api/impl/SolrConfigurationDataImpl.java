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

import java.io.InputStream;
import org.codice.solr.factory.SolrConfigurationData;

public class SolrConfigurationDataImpl implements SolrConfigurationData {

  private String fileName;

  private InputStream data;

  public SolrConfigurationDataImpl(String fileName, InputStream data) {
    setFileName(fileName);
    setData(data);
  }

  @Override
  public String getFileName() {
    return fileName;
  }

  @Override
  public InputStream getConfigurationData() {
    return data;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public void setData(InputStream data) {
    this.data = data;
  }
}
