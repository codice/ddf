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

import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.util.impl.MaskableImpl;

public abstract class AbstractWfsSource extends MaskableImpl
    implements FederatedSource, ConnectedSource, ConfiguredService {

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
}
