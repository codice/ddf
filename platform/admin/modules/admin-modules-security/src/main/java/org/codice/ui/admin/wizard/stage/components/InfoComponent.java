package org.codice.ui.admin.wizard.stage.components;

public class InfoComponent extends Component<String>{

    public InfoComponent(String id) {
        super(id);
    }

    @Override
    public void validate() {
        //No validation for the info component
    }
}
