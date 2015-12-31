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
 **/
package org.codice.ddf.spatial.ogc.wfs.catalog.endpoint;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import ogc.schema.opengis.filter.v_1_0_0.FilterType;

public final class FilterTypeContextFactory {

    private static JAXBContext jaxbContext;

    private FilterTypeContextFactory() {
    }

    public static synchronized JAXBContext getInstance() throws JAXBException {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(FilterType.class);
        }
        return jaxbContext;
    }
}
