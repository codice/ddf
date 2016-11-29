package org.codice.ui.admin.wizard.stage.components;

public class BaseComponent extends Component<String> {

    public BaseComponent(String id) {
        super(id);
    }

    @Override
    public void validate() {
        //No validation for base component
    }
}
