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
package org.codice.ddf.catalog.ui.query.monitor.impl;

import org.junit.Ignore;
import org.junit.Test;

public class TestSecurityServiceImpl {

    /**
     * Just make sure we don't get an exception
     */
    @Test
    @Ignore("We actually do get an exception from Security, an NPE with the keystore.")
    public void testGetSystemSubject() {
        new SecurityServiceImpl().getSystemSubject();
    }

}
