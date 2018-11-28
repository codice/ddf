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
package ddf.catalog.impl.capability;

import ddf.catalog.source.Source;
import ddf.catalog.source.SourceCapabilityProvider;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceCapabilityProviderImpl implements SourceCapabilityProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(SourceCapabilityProviderImpl.class);

  private String capabilityProviderId;

  private List<String> capabilities;

  private String sourceId;

  public SourceCapabilityProviderImpl(String capabilityProviderId) {
    this.capabilityProviderId = capabilityProviderId;
  }

  @Override
  public List<String> getSourceCapabilities(Source source) {
    if (source.getId().equals(sourceId)) {
      return capabilities;
    }

    return Collections.emptyList();
  }

  @Override
  public String getId() {
    return capabilityProviderId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public void setCapabilities(List<String> capabilities) {
    this.capabilities = capabilities;
  }
}
