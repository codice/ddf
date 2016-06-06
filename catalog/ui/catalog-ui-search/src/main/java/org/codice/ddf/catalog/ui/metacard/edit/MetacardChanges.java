package org.codice.ddf.catalog.ui.metacard.edit;

import java.util.List;

public class MetacardChanges {
    private List<String> ids;

    private List<AttributeChange> attributes;

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    public List<AttributeChange> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AttributeChange> attributes) {
        this.attributes = attributes;
    }
}
