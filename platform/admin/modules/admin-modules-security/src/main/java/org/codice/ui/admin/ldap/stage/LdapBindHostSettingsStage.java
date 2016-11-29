package org.codice.ui.admin.ldap.stage;

import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.BIND_USER_DN;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.BIND_USER_PASS;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LDAP_BIND_TEST_ID;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LDAP_CONFIGURATION_HANDLER_ID;
import static org.codice.ui.admin.wizard.stage.components.ButtonActionComponent.Method.POST;
import static org.codice.ui.admin.wizard.stage.components.Component.builder;
import static org.codice.ui.admin.wizard.stage.components.test.TestComponent.createTestComponent;

import java.util.List;
import java.util.Map;

import org.codice.ui.admin.ldap.config.LdapConfiguration;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.api.ConfigurationTestMessage;
import org.codice.ui.admin.wizard.config.Configuration;
import org.codice.ui.admin.wizard.stage.Stage;
import org.codice.ui.admin.wizard.stage.components.ButtonActionComponent;
import org.codice.ui.admin.wizard.stage.components.BaseComponent;
import org.codice.ui.admin.wizard.stage.components.ButtonBoxComponent;
import org.codice.ui.admin.wizard.stage.components.Component;
import org.codice.ui.admin.wizard.stage.components.PasswordComponent;
import org.codice.ui.admin.wizard.stage.components.StringComponent;

import com.google.common.collect.ImmutableMap;

public class LdapBindHostSettingsStage extends Stage<LdapConfiguration> {

    public static final String BIND_USER_DN_COMP_ID = "ldapBindUserDN";

    public static final String BIND_USER_PASS_COMP_ID = "ldapBindUserPassword";

    public static final String SKIP_BIND_HOST_TEST_BTN_ID = "skipBindHostBtnId";

    public static final String LDAP_BIND_HOST_SETTINGS_STAGE_ID = "ldapBindHostSettingsStageId";

    public static final String BUTTON_BOX_ID = "btnBox";


    public Map<String, LdapConfiguration.LDAP_CONFIGURATION_KEYS> compIdsToConfigIds = ImmutableMap.of(BIND_USER_DN_COMP_ID, BIND_USER_DN,
            BIND_USER_PASS_COMP_ID, BIND_USER_PASS);

    public LdapBindHostSettingsStage() {
        super(null, null, null);
    }

    public LdapBindHostSettingsStage(String wizardUrl, Map<String, String> state, LdapConfiguration ldapConfiguration) {
        super(wizardUrl, state, ldapConfiguration);
    }

    @Override
    public Stage<LdapConfiguration> probe(Stage<LdapConfiguration> stage, String probeId,
            List<ConfigurationHandler> configurationHandlers) {
        return stage;
    }

    @Override
    public Component getDefaultRootComponent() {
        return builder(BaseComponent.class).label("LDAP Bind User Settings").description("Now that we've figured out the network environment, we need to bind a user to the LDAP Store to retrieve additional information.").subComponents(
                    builder(StringComponent.class, BIND_USER_DN_COMP_ID)
                            .label("LDAP Bind User DN"),

                    builder(PasswordComponent.class, BIND_USER_PASS_COMP_ID)
                            .label("LDAP Bind User Password"),

                    builder(ButtonBoxComponent.class, BUTTON_BOX_ID).subComponents(
                        builder(ButtonActionComponent.class)
                                .setMethod(POST)
                                .setUrl(getWizardUrl() + "/" + getStageId())
                                .label("Check")
                    )
        );
    }

    @Override
    public Stage<LdapConfiguration> preconfigureStage(Stage<LdapConfiguration> stageToConfigure,
            List<ConfigurationHandler> configurationHandlers) {
        return stageToConfigure;
    }

    @Override
    public Stage<LdapConfiguration> testStage(Stage<LdapConfiguration> ldapBindHostSettingsStage, String testId,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {
        Configuration testConfiguration = getConfigurationFromStage(compIdsToConfigIds, ldapBindHostSettingsStage);
        List<ConfigurationTestMessage> testResults = getConfigurationHandler(configurationHandlers, LDAP_CONFIGURATION_HANDLER_ID).test(LDAP_BIND_TEST_ID, testConfiguration);

        if (!testResults.isEmpty()) {
            if (ldapBindHostSettingsStage.getComponent(SKIP_BIND_HOST_TEST_BTN_ID) == null) {
                ldapBindHostSettingsStage.getRootComponent().getComponent(BUTTON_BOX_ID)
                        .subComponents(new ButtonActionComponent(SKIP_BIND_HOST_TEST_BTN_ID).setMethod(
                                POST)
                                .setUrl(getWizardUrl() + "/" + getStageId() + "?skipTest=true")
                                .label("skip"));
            }

            for(ConfigurationTestMessage testMsg : testResults) {
                ldapBindHostSettingsStage.getRootComponent()
                        .subComponents(createTestComponent(testMsg));
            }
        }

        return ldapBindHostSettingsStage;
    }

    @Override
    public Stage<LdapConfiguration> commitStage(Stage<LdapConfiguration> stageToPersist,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {
        stageToPersist.setConfiguration(getConfigurationFromStage(compIdsToConfigIds, stageToPersist));
        return stageToPersist;
    }

    @Override
    public String getStageId() {
        return LDAP_BIND_HOST_SETTINGS_STAGE_ID;
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state, LdapConfiguration ldapConfiguration) {
        return new LdapBindHostSettingsStage(wizardUrl, state, ldapConfiguration);
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state) {
        return new LdapBindHostSettingsStage(wizardUrl, state, new LdapConfiguration());
    }
}