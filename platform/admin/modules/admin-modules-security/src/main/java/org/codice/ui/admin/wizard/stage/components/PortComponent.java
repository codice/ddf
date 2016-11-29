package org.codice.ui.admin.wizard.stage.components;

// TODO: tbatie - 10/31/16 - There were problems making this an integer since gson treats the number in a json as a double instead of integer
public class PortComponent extends Component<Double> {

    public PortComponent(String id) {
        super(id);
    }

    public PortComponent value(int port) {
        value((double) port);
        return this;
    }

    @Override
    public void validate() {

        if (required && getValue() == null || getValue() < 0) {
            addError("Invalid port number. Port number must be greater than 0.");
        }
    }
}
