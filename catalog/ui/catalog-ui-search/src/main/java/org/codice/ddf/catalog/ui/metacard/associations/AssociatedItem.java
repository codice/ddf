package org.codice.ddf.catalog.ui.metacard.associations;

import ddf.catalog.data.Metacard;

public class AssociatedItem {
    private String type;

    private String title;

    private String id;

    public AssociatedItem(String associationType, Metacard associated) {
        this.type = associationType;
        this.title = associated.getTitle();
        this.id = associated.getId();
    }

    public AssociatedItem(String associationType, String title, String id) {
        this.type = associationType;
        this.title = title;
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
