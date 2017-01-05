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

import static org.apache.commons.lang3.Validate.noNullElements;
import static org.apache.commons.lang3.Validate.notNull;

import java.util.List;

import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.query.monitor.api.MetacardFormatter;

/**
 * Iterate through a list of {@link MetacardFormatter} objects as a daisy chain.
 */
public class ListMetacardFormatter extends BaseMetacardFormatter {

    private final List<MetacardFormatter> metacardFormatterList;

    /**
     * @param metacardFormatterList must be non-null, elements must be non-null
     */
    public ListMetacardFormatter(List<MetacardFormatter> metacardFormatterList) {
        notNull(metacardFormatterList, "metacardFormatterList must be non-null");
        noNullElements(metacardFormatterList);
        this.metacardFormatterList = metacardFormatterList;
    }

    @Override
    protected String doFormat(String template, WorkspaceMetacardImpl workspaceMetacard,
            Long hitCount) {

        String tmp = template;
        for (MetacardFormatter metacardFormatter : metacardFormatterList) {
            tmp = metacardFormatter.format(tmp, workspaceMetacard, hitCount);
        }

        return tmp;
    }

    @Override
    public String toString() {
        return "ListMetacardFormatter{" +
                "metacardFormatterList=" + metacardFormatterList +
                '}';
    }
}
