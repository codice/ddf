package org.codice.ui.admin.wizard.stage.components;

import org.apache.cxf.common.util.StringUtils;

public class StringComponent extends Component<String> {

    public StringComponent(String id) {
        super(id);
    }

    @Override
    public void validate() {
        if(required && StringUtils.isEmpty(getValue())) {
            addError("Invalid entry. Field cannot be empty.");
        }
    }
}
