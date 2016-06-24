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

import static org.apache.commons.lang3.Validate.notNull;

import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.query.monitor.api.MetacardFormatter;

/**
 * Base class that ensures arguments to {@link #format(String, WorkspaceMetacardImpl, Long)} are
 * checked.
 */
public abstract class BaseMetacardFormatter implements MetacardFormatter {

    @Override
    public final String format(String template, WorkspaceMetacardImpl workspaceMetacard,
            Long hitCount) {
        notNull(template, "template must be non-null");
        notNull(workspaceMetacard, "workspaceMetacard must be non-null");
        notNull(hitCount, "hitCount must be non-null");
        return doFormat(template, workspaceMetacard, hitCount);
    }

    /**
     * The arguments are guaranteed to be non-null.
     *
     * @param template          non-null
     * @param workspaceMetacard non-null
     * @param hitCount          non-null
     * @return formatted string
     */
    protected abstract String doFormat(String template, WorkspaceMetacardImpl workspaceMetacard,
            Long hitCount);

}
