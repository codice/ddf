package org.codice.ui.admin.wizard.stage.components;

public class StringEnumComponent extends Component<String> {

    public StringEnumComponent(String id) {
        super(id);
    }

    @Override
    public void validate() {
        if(required && getValue() == null ) {
            addError("The provided entry is invalid.");
        }
    }
}
