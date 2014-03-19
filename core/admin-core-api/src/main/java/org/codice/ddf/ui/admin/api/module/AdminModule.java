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
package org.codice.ddf.ui.admin.api.module;

import java.net.URI;

/**
 * Defines a module to be plugged into the admin ui
 */
public interface AdminModule {

    /**
     * Returns the name of the module. This refers to the module name that will be attached to the Marionette object
     * as well as the text of the module's tab. This must not contain spaces.
     * @return String
     */
    public String getName();

    /**
     * Returns the id that will be injected into the DOM. If using Marionette, one can call App.<this id> within the
     * module to return the region where the module should be rendered.
     * @return String
     */
    public String getId();

    /**
     * Absolute path to the module JS file. This file can require in additional requirejs modules as needed.
     * @return URI
     */
    public URI getJSLocation();

    /**
     * Absolute path to any CSS that accompanies this module. The CSS will be included globally.
     * @return URI
     */
    public URI getCSSLocation();
}
