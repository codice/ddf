/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.util.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class MaskerTest {

    Masker masker;

    MaskableImpl maskee;

    String unmaskedId;

    String data;

    @Before
    public void setUp() throws Exception {
        unmaskedId = MaskerTest.class.getName();
        data = "JUnit:" + MaskerTest.class.getSimpleName();
        masker = new Masker();
        maskee = new MaskableImpl();
        maskee.setId(unmaskedId);
        maskee.setDescription(data);
        maskee.setOrganization(data);
        maskee.setTitle(data);
    }

    @Test
    public void testMasker() {
        assertNotNull(masker);

    }

    @Test
    public void testSetIdAfterBinding() {
        masker.bind(maskee);
        assertTrue(unmaskedId.equals(maskee.getId()));
        masker.setId("testMask");
        assertFalse(unmaskedId.equals(maskee.getId()));

    }

    @Test
    public void testSetIdBeforeBinding() {
        assertTrue(unmaskedId.equals(maskee.getId()));
        masker.setId("testMask");
        masker.bind(maskee);
        assertFalse(unmaskedId.equals(maskee.getId()));
    }

    @Test
    public void testBind() {
        masker.bind(maskee);
        assertTrue(masker.maskees.contains(maskee));
    }

    @Test
    public void testUnbind() {
        masker.bind(maskee);
        masker.unbind(maskee);
        assertFalse(masker.maskees.contains(maskee));

    }

}
