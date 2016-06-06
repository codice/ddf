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
