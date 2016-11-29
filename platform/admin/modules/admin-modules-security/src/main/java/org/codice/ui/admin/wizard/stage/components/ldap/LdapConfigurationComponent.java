package org.codice.ui.admin.wizard.stage.components.ldap;

import java.util.HashMap;
import java.util.Map;

import org.codice.ui.admin.ldap.config.LdapConfiguration;
import org.codice.ui.admin.wizard.config.Configuration;
import org.codice.ui.admin.wizard.stage.components.Component;

public class LdapConfigurationComponent extends Component{

    private LdapConfiguration config;

    public LdapConfigurationComponent(String id) {
        super(id);
    }

    @Override
    public void validate() {
        // No validation for an already created Configuration
    }

    public LdapConfigurationComponent configuration(LdapConfiguration ldapConfig) {
        config = ldapConfig;
        return this;
    }
}
