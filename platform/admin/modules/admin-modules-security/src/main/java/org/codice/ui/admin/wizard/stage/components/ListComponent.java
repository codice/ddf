package org.codice.ui.admin.wizard.stage.components;

public class ListComponent extends Component<String>{

    public ListComponent(String id) {
        super(id);
    }

    @Override
    public void validate() {
        //No validation for the List component
    }
}
