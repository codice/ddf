package org.codice.ui.admin.wizard.stage.components;

import java.util.HashMap;
import java.util.Map;

public class LdapResultsComponent extends Component<String>{

    public LdapResultsComponent(String id) {
        super(id);
    }

    @Override
    public void validate() {
        //No validation for the LDAP Query Results component
    }
}
