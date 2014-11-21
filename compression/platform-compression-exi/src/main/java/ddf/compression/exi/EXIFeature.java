/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.compression.exi;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controls EXI compression of CXF-based messages.
 * Attaching this feature to a cxf endpoint (jaxws:endpoint or jaxrs:server) will allow the server to send responses
 * back in an exi-encoded format.
 */
@NoJSR250Annotations
public class EXIFeature extends AbstractFeature {

    private static final Logger LOGGER = LoggerFactory.getLogger(EXIFeature.class);

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        EXIOutInterceptor outInterceptor = new EXIOutInterceptor();
        provider.getOutInterceptors().add(outInterceptor);
        LOGGER.debug("Added EXIOutInterceptor to provider.");
    }
}
