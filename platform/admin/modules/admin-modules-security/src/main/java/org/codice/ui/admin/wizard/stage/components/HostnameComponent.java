package org.codice.ui.admin.wizard.stage.components;

import org.apache.cxf.common.util.StringUtils;

public class HostnameComponent extends Component<String> {
    public HostnameComponent(String id) {
        super(id);
    }

    @Override
    public void validate() {
        if(required && StringUtils.isEmpty(getValue())) {
            addError("Invalid hostname. Hostname cannot be empty or contain specific special characters.");
        }
    }
}
