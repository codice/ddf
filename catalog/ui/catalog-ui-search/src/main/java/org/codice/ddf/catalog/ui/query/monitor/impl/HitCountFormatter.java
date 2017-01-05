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

import java.util.regex.Pattern;

import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;

/**
 * Replaces all tags {@code %[hitCount]} with the hit count.
 */
public class HitCountFormatter extends BaseMetacardFormatter {

    private static final String HIT_COUNT_TAG = "%[hitCount]";

    @Override
    protected String doFormat(String template, WorkspaceMetacardImpl workspaceMetacard,
            Long hitCount) {
        return template.replaceAll(Pattern.quote(HIT_COUNT_TAG), hitCount.toString());
    }

    @Override
    public String toString() {
        return "HitCountFormatter{}";
    }
}
