package org.codice.ui.admin.ldap.stage;

import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LDAP_CONFIGURATION_HANDLER_ID;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LDAP_DIRECTORY_STRUCT_TEST_ID;
import static org.codice.ui.admin.wizard.stage.components.ButtonActionComponent.Method.POST;
import static org.codice.ui.admin.wizard.stage.components.Component.builder;
import static org.codice.ui.admin.wizard.stage.components.test.TestComponent.createTestComponent;

import java.util.List;
import java.util.Map;

import org.codice.ui.admin.ldap.config.LdapConfiguration;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.api.ConfigurationTestMessage;
import org.codice.ui.admin.wizard.stage.Stage;
import org.codice.ui.admin.wizard.stage.components.BaseComponent;
import org.codice.ui.admin.wizard.stage.components.ButtonActionComponent;
import org.codice.ui.admin.wizard.stage.components.Component;
import org.codice.ui.admin.wizard.stage.components.HostnameComponent;
import org.codice.ui.admin.wizard.stage.components.PortComponent;
import org.codice.ui.admin.wizard.stage.components.StringComponent;

public class LdapSettingsConfirmationStage extends Stage<LdapConfiguration> {

    public static final String LDAP_CONFIRMATION_STAGE = "LdapConfirmationStage";

    public LdapSettingsConfirmationStage() {
        super(null, null, null);
    }

    public LdapSettingsConfirmationStage(String wizardUrl, Map<String, String> state,
            LdapConfiguration sourceConfiguration) {
        super(wizardUrl, state, sourceConfiguration);
    }

    @Override
    public String getStageId() {
        return LDAP_CONFIRMATION_STAGE;
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state,
            LdapConfiguration configuration) {
        return new LdapSettingsConfirmationStage(wizardUrl, state, configuration);
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state) {
        return new LdapSettingsConfirmationStage(wizardUrl, state, null);
    }

    @Override
    public Stage<LdapConfiguration> probe(Stage<LdapConfiguration> stage, String probeId,
            List<ConfigurationHandler> configurationHandlers) {
        return stage;
    }

    @Override
    public Stage<LdapConfiguration> preconfigureStage(Stage<LdapConfiguration> stageToConfigure,
            List<ConfigurationHandler> configurationHandlers) {

        LdapConfiguration cfg = stageToConfigure.getConfiguration();

        stageToConfigure.getRootComponent()
                .label("Create LDAP Configuration?")
                .description("Please review the follow configuration information.")
                .subComponents(builder(HostnameComponent.class).value(cfg.hostName())
                                .label("Hostname")
                                .disabled(),
                        builder(PortComponent.class).value(cfg.port())
                                .label("Port")
                                .disabled(),
                        builder(StringComponent.class).value(cfg.encryptionMethod())
                                .label("Encryption Method")
                                .disabled(),
                        builder(StringComponent.class).value(cfg.bindUserDN())
                                .label("User")
                                .disabled(),
                        builder(ButtonActionComponent.class).setMethod(POST)
                                .setUrl(getWizardUrl() + "/" + LDAP_CONFIRMATION_STAGE)
                                .label("Finish"));

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

        List<ConfigurationTestMessage> persistResults = getConfigurationHandler(configurationHandlers,
                LDAP_CONFIGURATION_HANDLER_ID).persist(stageToPersist.getConfiguration());

        for(ConfigurationTestMessage testMsg : persistResults) {
            stageToPersist.getRootComponent()
                    .subComponents(createTestComponent(testMsg));
        }

        return stageToPersist;
    }

    @Override
    public Component getDefaultRootComponent() {
        return builder(BaseComponent.class);
    }
}
