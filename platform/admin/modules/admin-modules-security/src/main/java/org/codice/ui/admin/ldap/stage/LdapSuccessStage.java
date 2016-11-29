package org.codice.ui.admin.ldap.stage;

import static org.codice.ui.admin.wizard.stage.components.ButtonActionComponent.Method.GET;
import static org.codice.ui.admin.wizard.stage.components.Component.builder;

import java.util.List;
import java.util.Map;

import org.codice.ui.admin.ldap.config.LdapConfiguration;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.stage.Stage;
import org.codice.ui.admin.wizard.stage.components.BaseComponent;
import org.codice.ui.admin.wizard.stage.components.ButtonActionComponent;
import org.codice.ui.admin.wizard.stage.components.Component;
import org.codice.ui.admin.wizard.stage.components.InfoComponent;

public class LdapSuccessStage extends Stage<LdapConfiguration> {

    public static final String LDAP_SUCCESS_STAGE = "LdapSuccessStage";

    public LdapSuccessStage() {
        super(null, null, null);
        canGoBack = false;
    }

    public LdapSuccessStage(String wizardUrl, Map<String, String> state,
            LdapConfiguration sourceConfiguration) {
        super(wizardUrl, state, sourceConfiguration);
        canGoBack = false;
    }

    @Override
    public String getStageId() {
        return LDAP_SUCCESS_STAGE;
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state,
            LdapConfiguration configuration) {
        return new LdapSuccessStage(wizardUrl, state, configuration);
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state) {
        return new LdapSuccessStage(wizardUrl, state, null);
    }

    @Override
    public Stage<LdapConfiguration> probe(Stage<LdapConfiguration> stage, String probeId,
            List<ConfigurationHandler> configurationHandlers) {
        return stage;
    }

    @Override
    public Stage<LdapConfiguration> preconfigureStage(Stage<LdapConfiguration> stageToConfigure,
            List<ConfigurationHandler> configurationHandlers) {
        return stageToConfigure;
    }

    @Override
    public Stage<LdapConfiguration> testStage(Stage<LdapConfiguration> stageToTest, String stageId,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {
        return stageToTest;
    }

    @Override
    public Stage<LdapConfiguration> commitStage(Stage<LdapConfiguration> stageToPersist,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {
        return stageToPersist;
    }

    @Override
    public Component getDefaultRootComponent() {
        String homeUrl = getWizardUrl() == null ? "" : getWizardUrl().substring(
                getWizardUrl().lastIndexOf('/') + 1);

        return builder(BaseComponent.class).subComponents(builder(InfoComponent.class).label(
                "LDAP has been successfully configured for login.")
                        .value("Now that LDAP has been configured as an authentication source, make sure that the web context policy manager is configured to use ldap for context paths"),
                builder(ButtonActionComponent.class).setMethod(GET)
                        .setUrl(homeUrl)
                        .label("Go Home"));
    }

}
