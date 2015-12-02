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
package org.codice.ddf.security.servlet.logout;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import ddf.security.http.impl.HttpSessionFactory;

/**
 * Created by tbatie on 12/1/15.
 */
public class TestLogoutService {

    @Test
    public void testLogout() {
        HttpSessionFactory sessionFactory = mock(HttpSessionFactory.class);
        when(sessionFactory.getOrCreateSession(any(HttpServletRequest.class))).thenReturn(null);
        LogoutService logoutService = new LogoutService();
        logoutService.setHttpSessionFactory(sessionFactory);
    }


}
