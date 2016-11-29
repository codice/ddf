package org.codice.ui.admin.wizard.stage.components.ldap;

import org.codice.ui.admin.wizard.stage.components.Component;

public class LdapQueryComponent extends Component<String> {
    public LdapQueryComponent(String id) {
        super(id);
    }

    @Override
    public void validate() {
        if(required && getValue() == null) {
            addError("Ldap query cannot be empty.");
        }
    }
}
