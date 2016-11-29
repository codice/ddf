package org.codice.ui.admin.wizard.stage.components;

public class ButtonActionComponent extends Component<String> {

    private String url;

    private Method method = Method.GET;

    public ButtonActionComponent() {
        super(null);
    }

    public ButtonActionComponent(String id) {
        super(id);
    }

    @Override
    public void validate() {
        //No validation for button action
    }

    public ButtonActionComponent setUrl(String url) {
        this.url = url;
        return this;
    }

    public ButtonActionComponent setMethod(Method method) {
        this.method = method;
        return this;
    }

    public enum Method {GET, POST}

}
