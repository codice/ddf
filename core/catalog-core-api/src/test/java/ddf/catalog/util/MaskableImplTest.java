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
package ddf.catalog.util;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class MaskableImplTest {

    MaskableImpl mi;

    String data;

    String id1;

    String id2;

    String shortName;

    @Before
    public void setUp() throws Exception {
        id1 = UUID.randomUUID().toString();
        id2 = UUID.randomUUID().toString();
        data = "JUnit:" + MaskableImpl.class.getSimpleName();

    }

    @Test
    public void testMaskableImpl() {
        mi = new MaskableImpl();
        mi.setId(id1);
        mi.setDescription(data);
        mi.setOrganization(data);
        mi.setTitle(data);
        mi.setVersion(data);
        assertTrue(id1.equals(mi.getId()));
        assertTrue(data.equals(mi.getDescription()));
        assertTrue(data.equals(mi.getOrganization()));
        assertTrue(data.equals(mi.getTitle()));
        assertTrue(data.equals(mi.getVersion()));

        mi.maskId(id2);
        assertTrue(id2.equals(mi.getId()));
        mi.setId(id1);
        assertTrue(id2.equals(mi.getId()));
    }
}
