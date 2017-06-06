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
package org.codice.ddf.admin.core.impl.module;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.codice.ddf.ui.admin.api.module.AdminModule;

/**
 * ValidationDecorator - a specific decorator for the {@link AdminModule} that adds support
 * for validation.
 */
public class ValidationDecorator extends Decorator {

    ValidationDecorator(AdminModule module) {
        super(module);
    }

    /**
     * Wraps a list of {@link List <AdminModule>}.
     * @param adminList
     * @return
     */
    public static List<ValidationDecorator> wrap(List<AdminModule> adminList) {
        List<ValidationDecorator> list = new ArrayList<>();
        for (AdminModule module : adminList) {
            list.add(new ValidationDecorator(module));
        }
        return list;
    }

    private boolean isValidURI(URI uri) {
        return uri == null || (uri.toString()
                .charAt(0) != '/' && !uri.isAbsolute());
    }

    /**
     * Determine if an {@link AdminModule} is valid.
     * NOTE: a valid module cannot contain absolute URIs.
     * @return
     */
    public boolean isValid() {
        return isValidURI(getJSLocation()) && isValidURI(getCSSLocation()) && isValidURI(
                getIframeLocation());
    }

}
