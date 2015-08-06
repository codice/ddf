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
package org.codice.ddf.ui.admin.api.module;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AdminModuleExt implements AdminModule, Comparable {

    private AdminModule module;

    AdminModuleExt(AdminModule module) {
        this.module = module;
    }

    public static List<AdminModuleExt> wrap(List<AdminModule> adminList) {
        List<AdminModuleExt> list = new ArrayList<>();
        for (AdminModule module : adminList) {
            list.add(new AdminModuleExt(module));
        }
        return list;
    }

    public String getName() {
        return module.getName();
    }

    public String getId() {
        return module.getId();
    }

    public URI getJSLocation() {
        return module.getJSLocation();
    }

    public URI getCSSLocation() {
        return module.getCSSLocation();
    }

    public URI getIframeLocation() {
        return module.getIframeLocation();
    }

    private boolean isValidURI(URI uri) {
        return uri == null || (uri.toString().charAt(0) != '/' && !uri.isAbsolute());
    }

    public boolean isValid() {
        return isValidURI(getJSLocation()) && isValidURI(getCSSLocation()) && isValidURI(
                getIframeLocation());
    }

    public int compareTo(Object o) {
        return getName().compareTo(((AdminModule) o).getName());
    }

    public HashMap<String, Object> toHashMap() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("name", getName());
        map.put("id", getId());
        map.put("jsLocation", getJSLocation());
        map.put("cssLocation", getCSSLocation());
        map.put("iframeLocation", getIframeLocation());
        return map;
    }

}
