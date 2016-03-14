/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.ogc.csw.catalog.source;

import static org.mockito.Mockito.mock;

import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSubscribe;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswTransformProvider;
import org.osgi.framework.BundleContext;

import ddf.security.Subject;

public class CswSourceStub extends CswSource {

    Subject subject = mock(Subject.class);

    public CswSourceStub(BundleContext mockContext, CswSourceConfiguration cswSourceConfiguration,
            CswTransformProvider mockProvider, SecureCxfClientFactory mockFactory) {
        super(mockContext, cswSourceConfiguration, mockProvider, mockFactory);
        super.subscribeClientFactory = mock(SecureCxfClientFactory.class);
    }

    @Override
    protected void initSubscribeClientFactory() {

    }

    @Override
    protected Subject getSystemSubject() {
        return subject;
    }

    public Subject getSubject() {
        return subject;
    }

    public SecureCxfClientFactory<CswSubscribe> getSubscriberClientFactory() {
        return subscribeClientFactory;
    }
}
