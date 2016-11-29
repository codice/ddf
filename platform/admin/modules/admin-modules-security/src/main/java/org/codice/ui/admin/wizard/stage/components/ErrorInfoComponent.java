package org.codice.ui.admin.wizard.stage.components;

// TODO: tbatie - 11/4/16 - Hoping to get rid of this class and only clear out TestComponents instead of Error components
public class ErrorInfoComponent extends Component<String> {

    public ErrorInfoComponent(String id) {
        super(id);
    }

    @Override
    public void validate() {
        //No validation for error info component
    }
}
