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
package org.codice.ddf.ui.searchui.standard.endpoints;

public class StandardSearchException extends Exception {
    public StandardSearchException(String s) {
        super(s);
    }

    public StandardSearchException(Exception e) {
        super(e);
    }

    public StandardSearchException(String s, Exception e) {
        super(s, e);
    }

    public StandardSearchException(String s, Exception e, boolean x, boolean y) {
        super(s, e, x, y);
    }
}
