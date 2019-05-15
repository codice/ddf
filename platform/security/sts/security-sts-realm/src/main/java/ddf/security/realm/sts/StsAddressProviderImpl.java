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
package ddf.security.realm.sts;

import ddf.security.sts.client.configuration.STSClientConfiguration;
import ddf.security.sts.client.configuration.StsAddressProvider;
import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StsAddressProviderImpl implements StsAddressProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(StsAddressProviderImpl.class);

  private final STSClientConfiguration internalSts;

  public StsAddressProviderImpl(STSClientConfiguration internalSts) {
    this.internalSts = internalSts;
  }

  @Override
  public String getWsdlAddress() {
    return internalSts.getAddress();
  }

  @Override
  public String getProtocol() {
    try {
      URI uri = new URI(getWsdlAddress());
      return uri.getScheme();
    } catch (URISyntaxException e) {
      LOGGER.info("Unable to parse STS url", e);
      return "";
    }
  }

  @Override
  public String getHost() {
    try {
      URI uri = new URI(getWsdlAddress());
      return uri.getHost();
    } catch (URISyntaxException e) {
      LOGGER.info("Unable to parse STS url", e);
      return "";
    }
  }

  @Override
  public String getPort() {
    try {
      URI uri = new URI(getWsdlAddress());
      return Integer.toString(uri.getPort());
    } catch (URISyntaxException e) {
      LOGGER.info("Unable to parse STS url", e);
      return "";
    }
  }
}
