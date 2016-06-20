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
package org.codice.ddf.catalog.ui.metacard.workspace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.impl.MetacardImpl;
import ddf.security.permission.CollectionPermission;

public class SharingMetacardImplTest {

    private SharingMetacardImpl sharing;

    @Before
    public void setUp() {
        sharing = new SharingMetacardImpl();
    }

    @Test
    public void testSharingAttribute() {
        assertThat(sharing.setSharingAttribute("email")
                .getSharingAttribute(), is("email"));
    }

    @Test
    public void testAction() {
        assertThat(sharing.setAction(CollectionPermission.READ_ACTION)
                .getAction(), is(CollectionPermission.READ_ACTION));
    }

    @Test
    public void testValue() {
        assertThat(sharing.setValue("guest@localhost")
                .getValue(), is("guest@localhost"));
    }

    @Test
    public void testIsSharingMetacard() {
        assertThat(SharingMetacardImpl.isSharingMetacard(null), is(false));
        assertThat(SharingMetacardImpl.isSharingMetacard(new MetacardImpl()), is(false));
        assertThat(SharingMetacardImpl.isSharingMetacard(sharing), is(true));
    }

}