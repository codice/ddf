package org.codice.ui.admin.wizard.stage.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListItemComponent extends Component<String>{

    public Map<String, String> attributes;

    public ListItemComponent(String id) {
        super(id);
    }

    @Override
    public void validate() {
        //No validation for the List Item component
    }

    public ListItemComponent attributes(Map<String, String> attributes) {
        this.attributes = attributes;
        return this;
    }
}
