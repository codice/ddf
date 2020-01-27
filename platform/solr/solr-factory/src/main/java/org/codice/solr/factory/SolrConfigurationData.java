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
package org.codice.solr.factory;

import java.io.InputStream;

/** Descriptor for a Solr configuration file */
public interface SolrConfigurationData {

  /**
   * The file name to write the data into the Solr configuration or Zookeeper
   *
   * @return
   */
  String getFileName();

  /**
   * The contents of the Configuration file to be written.
   *
   * @return InputStream - This stream must be closed by the consumer.
   */
  InputStream getConfigurationData();
}
