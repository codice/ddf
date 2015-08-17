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
import java.util.Map;

/**
 * AdminModuleWrapper - wraps the {@link AdminModule} interface and adds useful methods such as
 * validation, comparison, and conversion to maps.
 * NOTE: a valid admin module cannot have any absolute urls.
 */
public class AdminModuleWrapper implements AdminModule, Comparable {

    private AdminModule module;

    AdminModuleWrapper(AdminModule module) {
        this.module = module;
    }

    /**
     * Wraps a list of {@link List<AdminModule>}.
     * @param adminList
     * @return
     */
    public static List<AdminModuleWrapper> wrap(List<AdminModule> adminList) {
        List<AdminModuleWrapper> list = new ArrayList<>();
        for (AdminModule module : adminList) {
            list.add(new AdminModuleWrapper(module));
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

    /**
     * Determine if an {@link AdminModule} is valid.
     * NOTE: a valid module cannot contain absolute URIs.
     * @return
     */
    public boolean isValid() {
        return isValidURI(getJSLocation()) && isValidURI(getCSSLocation()) && isValidURI(
                getIframeLocation());
    }

    /**
     * Define an ordering for {@link AdminModule}. The are alphabetically sorted by module name.
     * @param o
     * @return
     */
    public int compareTo(Object o) {
        return getName().compareTo(((AdminModule) o).getName());
    }

    private String uriToString(URI uri) {
        if (uri == null) {
            return "";
        }
        return uri.toString();
    }

    /**
     * Serialize a {@link AdminModule} as a {@link Map}.
     * NOTE: any null URIs get returned as empty strings.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", getName());
        map.put("id", getId());
        map.put("jsLocation", uriToString(getJSLocation()));
        map.put("cssLocation", uriToString(getCSSLocation()));
        map.put("iframeLocation", uriToString(getIframeLocation()));
        return map;
    }

}
