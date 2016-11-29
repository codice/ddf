package org.codice.ui.admin.ldap.stage;

import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.ENCRYPTION_METHOD;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.HOST_NAME;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.PORT;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.NONE;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.TLS;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAPS;

import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LDAP_CONFIGURATION_HANDLER_ID;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LDAP_CONNECTION_TEST_ID;
import static org.codice.ui.admin.wizard.stage.components.ButtonActionComponent.Method.POST;

import static org.codice.ui.admin.wizard.stage.components.Component.builder;
import static org.codice.ui.admin.wizard.stage.components.test.TestComponent.createTestComponent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codice.ui.admin.ldap.config.LdapConfiguration;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.api.ConfigurationTestMessage;
import org.codice.ui.admin.wizard.stage.Stage;
import org.codice.ui.admin.wizard.stage.components.ButtonActionComponent;
import org.codice.ui.admin.wizard.stage.components.BaseComponent;
import org.codice.ui.admin.wizard.stage.components.ButtonBoxComponent;
import org.codice.ui.admin.wizard.stage.components.Component;
import org.codice.ui.admin.wizard.stage.components.HostnameComponent;
import org.codice.ui.admin.wizard.stage.components.PortComponent;
import org.codice.ui.admin.wizard.stage.components.StringEnumComponent;

import com.google.common.collect.ImmutableMap;

public class LdapNetworkSettingsStage extends Stage<LdapConfiguration> {

    // LDAP Network Configuration Ids
    public static final String LDAP_HOST_NAME_COMP_ID = "ldapHostName";

    public static final String LDAP_PORT_COMP_ID = "ldapPort";

    public static final String LDAP_ENCRYPTION_METHOD_COMP_ID = "ldapEncryptionMethod";

    public static final String SKIP_NETWORK_TESTING_BTN_ID = "skipNetworkTestBtnId";

    public static final String LDAP_NETWORK_SETTINGS_STAGE_ID = "ldapNetworkSettingsStage";

    public static final String BUTTON_BOX_ID = "btnBox";


    public static final Map LDAP_ENCRYPTION_METHODS_MAP = ImmutableMap.of("No encryption", NONE,
            "Use ldaps", LDAPS,
            "Use startTls", TLS);

    public LdapNetworkSettingsStage() {
        super(null, null, null);
    }

    public LdapNetworkSettingsStage(String wizardUrl, Map<String, String> state, LdapConfiguration ldapConfiguration) {
        super(wizardUrl, state, ldapConfiguration);
    }

    // TODO: tbatie - 11/1/16 - rename component id's
    public static final Map<String, LdapConfiguration.LDAP_CONFIGURATION_KEYS> componentIdsToConfigurationIds = ImmutableMap.of(LDAP_HOST_NAME_COMP_ID, HOST_NAME,
            LDAP_PORT_COMP_ID, PORT,
            LDAP_ENCRYPTION_METHOD_COMP_ID, ENCRYPTION_METHOD);

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
    public Stage<LdapConfiguration> testStage(Stage<LdapConfiguration> ldapNetworkSettingsStage, String testId,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {

        LdapConfiguration testConfiguration = getConfigurationFromStage(componentIdsToConfigurationIds, ldapNetworkSettingsStage);
        List<ConfigurationTestMessage> testResults = getConfigurationHandler(configurationHandlers,
                LDAP_CONFIGURATION_HANDLER_ID).test(LDAP_CONNECTION_TEST_ID, testConfiguration);

        if (!testResults.isEmpty()) {
            if (ldapNetworkSettingsStage.getComponent(SKIP_NETWORK_TESTING_BTN_ID) == null) {
                ldapNetworkSettingsStage.getRootComponent()
                        .getComponent(BUTTON_BOX_ID)
                        .subComponents(builder(ButtonActionComponent.class, SKIP_NETWORK_TESTING_BTN_ID)
                                .setMethod(POST)
                                .setUrl(getWizardUrl() + "/" + getStageId() + "?skipTest=true")
                                .label("skip network test"));
            }

            for(ConfigurationTestMessage testMsg : testResults) {
                ldapNetworkSettingsStage.getRootComponent()
                        .subComponents(createTestComponent(testMsg));
            }
        }

        return ldapNetworkSettingsStage;
    }

    @Override
    public Stage<LdapConfiguration> commitStage(Stage<LdapConfiguration> stageToPersist,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {
        LdapConfiguration newConfiguration = getConfigurationFromStage(componentIdsToConfigurationIds, stageToPersist);
        newConfiguration.encryptionMethod((String) LDAP_ENCRYPTION_METHODS_MAP.get(newConfiguration.encryptionMethod()));
        stageToPersist.setConfiguration(newConfiguration);
        return stageToPersist;
    }

    @Override
    public Component getDefaultRootComponent() {
        List encryptionMethods = Arrays.asList(LDAP_ENCRYPTION_METHODS_MAP.keySet()
                .toArray());

        return builder(BaseComponent.class).label("LDAP Network Settings").description("Let's start with the network configurations of your LDAP store.")
                .subComponents(
                        builder(HostnameComponent.class, LDAP_HOST_NAME_COMP_ID)
                        .label("LDAP Host name"),

                        builder(PortComponent.class, LDAP_PORT_COMP_ID)
                            .defaults(389.0, 636.0)
                            .label("LDAP PortComponent")
                            .value(389.0),

                        builder(StringEnumComponent.class, LDAP_ENCRYPTION_METHOD_COMP_ID)
                                .defaults(LDAP_ENCRYPTION_METHODS_MAP.keySet())
                                .value((String) encryptionMethods.get(1))
                                .label("Encryption method"),

                        builder(ButtonBoxComponent.class, BUTTON_BOX_ID).subComponents(
                                builder(ButtonActionComponent.class)
                                        .setUrl(getWizardUrl() + "/" + getStageId())
                                        .setMethod(POST)
                                        .label("check")
                        )

                );
    }

    @Override
    public String getStageId() {
        return LDAP_NETWORK_SETTINGS_STAGE_ID;
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state, LdapConfiguration ldapConfiguration) {
        return new LdapNetworkSettingsStage(wizardUrl, state, ldapConfiguration);
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state) {
        return new LdapNetworkSettingsStage(wizardUrl, state, new LdapConfiguration());
    }
}