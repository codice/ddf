package org.codice.ui.admin.wizard.stage.components;

public class ResultComponent extends Component<String>{

    private boolean succeeded;

    public ResultComponent(String id) {
        super(id);
    }

    public ResultComponent succeeded(boolean succeeded) {
        this.succeeded = succeeded;
        return this;
    }

    @Override
    public void validate() {
        //No validation for the StatusPageComponent
    }
}
