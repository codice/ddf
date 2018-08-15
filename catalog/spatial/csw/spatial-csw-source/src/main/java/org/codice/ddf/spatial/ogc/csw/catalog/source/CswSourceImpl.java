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
package org.codice.ddf.spatial.ogc.csw.catalog.source;

import com.thoughtworks.xstream.converters.Converter;
import ddf.security.encryption.EncryptionService;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.source.AbstractCswSource;
import org.osgi.framework.BundleContext;

public class CswSourceImpl extends AbstractCswSource {
  public CswSourceImpl(
      BundleContext context,
      CswSourceConfiguration cswSourceConfiguration,
      Converter provider,
      ClientFactoryFactory clientFactoryFactory,
      EncryptionService encryptionService) {
    super(context, cswSourceConfiguration, provider, clientFactoryFactory, encryptionService);
  }

  public CswSourceImpl(
      EncryptionService encryptionService, ClientFactoryFactory clientFactoryFactory) {
    super(encryptionService, clientFactoryFactory);
  }

  @Override
  protected Map<String, Consumer<Object>> getAdditionalConsumers() {
    return new HashMap<>();
  }
}
