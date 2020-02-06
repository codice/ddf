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
import org.codice.ddf.security.Security;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.source.AbstractCswStore;
import org.osgi.framework.BundleContext;

/**
 * A CSW Transactional Federated Source that implements the CatalogStore. This class extends the
 * {@link AbstractCswStore} and support sending transaction requests (CRUD) to the Federated Source.
 */
public class TransactionalCswStoreImpl extends AbstractCswStore {

  public TransactionalCswStoreImpl(
      BundleContext context,
      CswSourceConfiguration cswSourceConfiguration,
      Converter provider,
      ClientFactoryFactory factory,
      EncryptionService encryptionService,
      Security security) {
    super(context, cswSourceConfiguration, provider, factory, encryptionService, security);
  }

  public TransactionalCswStoreImpl(
      EncryptionService encryptionService,
      ClientFactoryFactory clientFactoryFactory,
      Security security) {
    super(encryptionService, clientFactoryFactory, security);
  }

  @Override
  protected Map<String, Consumer<Object>> getAdditionalConsumers() {
    return new HashMap<>();
  }
}
