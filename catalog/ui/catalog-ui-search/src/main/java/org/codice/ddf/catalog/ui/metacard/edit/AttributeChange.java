package org.codice.ddf.catalog.ui.metacard.edit;

import java.util.List;

public class AttributeChange {
    private String attribute;

    private List<String> values;

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
