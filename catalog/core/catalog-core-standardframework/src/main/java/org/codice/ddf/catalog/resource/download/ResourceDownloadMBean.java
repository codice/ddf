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
package org.codice.ddf.catalog.resource.download;

import javax.management.MBeanException;
import org.codice.ddf.catalog.resource.download.impl.ResourceDownload;

/** MBean interface describing the operations used to manage metacard resource downloads. */
public interface ResourceDownloadMBean {

  public static final String OBJECT_NAME =
      ResourceDownload.class.getName() + ":service=resource-download-service";

  public static final Class<ResourceDownloadMBean> MBEAN_CLASS = ResourceDownloadMBean.class;

  /**
   * Starts an asynchronous download of a specific metacard resource to the local site.
   *
   * @param sourceId ID of the federated source where the resource should be downloaded from
   * @param metacardId ID of the metacard that contains the resource to download
   * @throws MBeanException
   */
  void copyToLocalSite(String sourceId, String metacardId) throws MBeanException;
}
