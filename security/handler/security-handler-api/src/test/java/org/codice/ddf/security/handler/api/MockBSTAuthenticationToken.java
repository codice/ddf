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
package org.codice.ddf.security.handler.api;

import org.opensaml.xml.util.Base64;

public class MockBSTAuthenticationToken extends BSTAuthenticationToken {
    public static final String PRINCIPAL = "principal";

    public static final String CREDS = "creds";

    public static final String REALM = "realm";

    public MockBSTAuthenticationToken(Object p, Object c, String r) {
        super(p, c, r);
    }

//    public static BaseAuthenticationToken parse(String creds) {
//        return new MockBSTAuthenticationToken(PRINCIPAL, CREDS, REALM);
//    }


}
