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
package org.codice.ddf.spatial.ogc.csw.catalog.common.source;

import static org.mockito.Mockito.mock;

import com.thoughtworks.xstream.converters.Converter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.codice.ddf.spatial.ogc.csw.catalog.common.Csw;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.osgi.framework.BundleContext;

public class CswSourceStub extends AbstractCswSource {

  public CswSourceStub(
      BundleContext mockContext,
      CswSourceConfiguration cswSourceConfiguration,
      Converter mockProvider,
      JAXRSClientFactoryBean clientFactoryBean) {
    super(mockContext, cswSourceConfiguration, mockProvider);
    super.subscribeClientFactory = mock(JAXRSClientFactoryBean.class);
    super.factory = clientFactoryBean;
  }

  @Override
  protected JAXRSClientFactoryBean initClientFactory(Class clazz) {
    if (Csw.class.equals(clazz)) {
      return super.factory;
    } else {
      return super.subscribeClientFactory;
    }
  }

  @Override
  protected Map<String, Consumer<Object>> getAdditionalConsumers() {
    return new HashMap<>();
  }

  public JAXRSClientFactoryBean getSubscriberClientFactory() {
    return subscribeClientFactory;
  }
}
