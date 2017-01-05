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
package org.codice.ddf.catalog.ui.metacard.history;

import java.time.Instant;
import java.util.Date;

public class HistoryResponse {
    private Instant versioned;

    private String id;

    private String editedBy;

    public HistoryResponse(String historyId, String editedBy, Instant versioned) {
        this.id = historyId;
        this.editedBy = editedBy;
        this.versioned = versioned;
    }

    public HistoryResponse(String historyId, String editedBy, Date versioned) {
        this(historyId, editedBy, versioned.toInstant());
    }

    public Instant getVersioned() {
        return versioned;
    }

    public void setVersioned(Instant versioned) {
        this.versioned = versioned;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEditedBy() {
        return editedBy;
    }

    public void setEditedBy(String editedBy) {
        this.editedBy = editedBy;
    }

}
