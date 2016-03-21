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

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Workspace")
public class Workspace {

    public final String id;

    public final String title;

    public final List<String> metacards;

    public final List<String> queries;

    public Workspace(String id) {
        this(id, null, null, null);
    }

    public Workspace(String id, String title, List<String> metacards, List<String> queries) {
        this.id = id;
        this.title = title;
        this.metacards = metacards;
        this.queries = queries;
    }
}
