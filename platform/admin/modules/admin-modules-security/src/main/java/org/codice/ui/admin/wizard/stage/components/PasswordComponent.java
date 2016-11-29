package org.codice.ui.admin.wizard.stage.components;

import org.apache.cxf.common.util.StringUtils;

public class PasswordComponent extends Component<String> {

    public PasswordComponent(String id) {
        super(id);
    }

    @Override
    public void validate() {
        if(required && StringUtils.isEmpty(getValue())) {
            addError("Password field cannot be empty.");
        }
    }
}
