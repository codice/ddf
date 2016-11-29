package org.codice.ui.admin.wizard.stage.components;

import java.util.List;
import java.util.Map;

public abstract class RadioButtonsComponent<T> extends Component<T> {

    // TODO: 11/3/16 Stop hacking this together
    //label that will be retuned and the value that will be returned if selected
    private Map<String, T> options;

    private Map.Entry<String,T> defaultOption;

    public RadioButtonsComponent(String id) {
        super(id);
    }

    public RadioButtonsComponent defaultOption(Map.Entry<String,T> defaultOption) {
        // TODO: 11/3/16 Make sure this default option is inside the options list
        this.defaultOption = defaultOption;
        return this;
    }

    public RadioButtonsComponent setOptions(Map<String, T> options){
        // TODO: 11/3/16 Make sure values are unique
        this.options = options;
        return this;
    }

    @Override
    public void validate() {
        // TODO: 11/3/16 Validate this stuff
        if(required && getValue() == null) {
            addError("Invalid entry");
        }
    }
}
