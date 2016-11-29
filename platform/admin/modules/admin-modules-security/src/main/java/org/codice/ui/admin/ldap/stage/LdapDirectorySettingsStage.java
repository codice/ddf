package org.codice.ui.admin.ldap.stage;

import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.BASE_GROUP_DN;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.BASE_USER_DN;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.QUERY;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.QUERY_BASE;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.USERNAME_ATTRIBUTE;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LDAP_CONFIGURATION_HANDLER_ID;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LDAP_DIRECTORY_STRUCT_TEST_ID;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LDAP_QUERY_PROBE_ID;
import static org.codice.ui.admin.wizard.stage.components.ButtonActionComponent.Method.POST;
import static org.codice.ui.admin.wizard.stage.components.Component.builder;
import static org.codice.ui.admin.wizard.stage.components.test.TestComponent.createTestComponent;

import java.util.ArrayList;
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
import org.codice.ui.admin.wizard.stage.components.ContextPanelComponent;
import org.codice.ui.admin.wizard.stage.components.ListComponent;
import org.codice.ui.admin.wizard.stage.components.ListItemComponent;
import org.codice.ui.admin.wizard.stage.components.StringComponent;
import org.codice.ui.admin.wizard.stage.components.ldap.LdapQueryComponent;

import com.google.common.collect.ImmutableMap;

public class LdapDirectorySettingsStage extends Stage<LdapConfiguration> {

    public static final String LDAP_QUERY_CONTEXT_PANEL = "ldapQueryPanel";

    public static final String SKIP_LDAP_DIR_STRUCT_BTN_ID = "skipLdapDirTestBtn";

    public static final String LDAP_QUERY_TEST = "ldapQueryTest";

    public static final String LDAP_DIRECTORY_SETTINGS_STAGE_ID = "ldapDirectorySettingStage";

    public static final String LDAP_QUERY_RESULTS_COMPONENT_ID = "ldapQueryResultsComponentId";

    public static final String LDAP_QUERY_COMP_ID = "ldapQuery";

    public static final String LDAP_QUERY_BUTTON_ID = "ldapQueryButtonId";

    public static final String LDAP_QUERY_BASE_COMP_ID = "ldapQueryBase";

    public static final String BASE_USER_DN_COMP_ID = "baseUserDn";

    public static final String BASE_GROUP_DN_COMP_ID = "baseGroupDn";

    public static final String BASE_USERNAME_ATTRI_COMP_ID = "baseUserAttri";

    public static final String BUTTON_BOX_ID = "btnBoxId";


    public static final Map<String, LdapConfiguration.LDAP_CONFIGURATION_KEYS> ldapProbeComponentIdsToConfigIds = ImmutableMap.of(
            LDAP_QUERY_COMP_ID, QUERY,
            LDAP_QUERY_BASE_COMP_ID, QUERY_BASE);

    public static final Map<String, LdapConfiguration.LDAP_CONFIGURATION_KEYS> componentIdsToConfigurationIds = ImmutableMap.of(
            BASE_USER_DN_COMP_ID, BASE_USER_DN,
            BASE_GROUP_DN_COMP_ID, BASE_GROUP_DN,
            BASE_USERNAME_ATTRI_COMP_ID, USERNAME_ATTRIBUTE);

    public LdapDirectorySettingsStage() {
        super(null, null, null);
    }

    public LdapDirectorySettingsStage(String wizardUrl, Map<String, String> state, LdapConfiguration ldapConfiguration) {
        super(wizardUrl, state, ldapConfiguration);
    }

    @Override
    public Stage<LdapConfiguration> probe(Stage<LdapConfiguration> ldapDirectorySettingsStage, String probeId,
            List<ConfigurationHandler> configurationHandlers) {
        // TODO: tbatie - 10/28/16 - Change the probeId to be specific to ldap Query

        ldapDirectorySettingsStage.getComponent(LDAP_QUERY_CONTEXT_PANEL).validate();
        if (ldapDirectorySettingsStage.containsErrors()) {
            return ldapDirectorySettingsStage;
        }

        Configuration configToProbe = getConfigurationFromStage(ldapProbeComponentIdsToConfigIds, ldapDirectorySettingsStage);
        LdapConfiguration updatedConfig = (LdapConfiguration) getConfigurationHandler(configurationHandlers,
                LDAP_CONFIGURATION_HANDLER_ID).probe(LDAP_QUERY_PROBE_ID, configToProbe);

        List<Component> entries = new ArrayList<>();

        for (Map<String, String> result : updatedConfig.queryResult()) {
            entries.add(builder(ListItemComponent.class).attributes(result));
        }

        if(ldapDirectorySettingsStage.getComponent(LDAP_QUERY_RESULTS_COMPONENT_ID) == null) {
            Component newEntriesList = builder(ListComponent.class).subComponents(entries);
            Component queryResultsComponent = builder(ContextPanelComponent.class,
                    LDAP_QUERY_RESULTS_COMPONENT_ID).label("LDAP Query Results").subComponents(newEntriesList);
            ldapDirectorySettingsStage.getComponent(LDAP_QUERY_CONTEXT_PANEL).subComponents(queryResultsComponent);
        } else {
            ldapDirectorySettingsStage.getComponent(LDAP_QUERY_RESULTS_COMPONENT_ID).clearChildren();
            ldapDirectorySettingsStage.getComponent(LDAP_QUERY_RESULTS_COMPONENT_ID).subComponents(entries);
        }

        return ldapDirectorySettingsStage;
    }

    @Override
    public Stage<LdapConfiguration> preconfigureStage(Stage<LdapConfiguration> stageToConfigure,
            List<ConfigurationHandler> configurationHandlers) {
        return stageToConfigure;
    }

    @Override
    public Stage<LdapConfiguration> testStage(Stage<LdapConfiguration> ldapDirectorySettingsStage, String testId,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {

        Configuration ldapDirStructureConfig = getConfigurationFromStage(
                componentIdsToConfigurationIds, ldapDirectorySettingsStage);
        List<ConfigurationTestMessage> testResults = getConfigurationHandler(configurationHandlers,
                LDAP_CONFIGURATION_HANDLER_ID).test(LDAP_DIRECTORY_STRUCT_TEST_ID,
                ldapDirStructureConfig);

        if (!testResults.isEmpty()) {

            if (ldapDirectorySettingsStage.getComponent(SKIP_LDAP_DIR_STRUCT_BTN_ID) == null) {
                ldapDirectorySettingsStage.getRootComponent().getComponent(BUTTON_BOX_ID)
                        .subComponents(new ButtonActionComponent(SKIP_LDAP_DIR_STRUCT_BTN_ID).setMethod(
                                POST)
                                .setUrl(getWizardUrl() + "?skip=true")
                                .label("skip"));
            }

            for(ConfigurationTestMessage testMsg : testResults) {
                ldapDirectorySettingsStage.getRootComponent()
                        .subComponents(createTestComponent(testMsg));
            }

        }

        return ldapDirectorySettingsStage;
    }

    @Override
    public Stage<LdapConfiguration> commitStage(Stage<LdapConfiguration> stageToPersist,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {
        LdapConfiguration newConfiguration = getConfigurationFromStage(componentIdsToConfigurationIds, stageToPersist);
        stageToPersist.setConfiguration(newConfiguration);
        return stageToPersist;
    }

    @Override
    public Component getDefaultRootComponent() {
        Component ldapQueryBox = builder(ContextPanelComponent.class, LDAP_QUERY_CONTEXT_PANEL).label("LDAP Query Tool")
                .description("The search field below can be used to execute searches to help find the Base User DN, Group User DN and User name attribute required to setup LDAP")
                .subComponents(builder(StringComponent.class, LDAP_QUERY_BASE_COMP_ID).defaults(
                        "dc=example,dc=com")
                                .label("Search base DN")
                                .description(
                                        "This is the distinguished name of the directory to be searched."),

                        builder(LdapQueryComponent.class, LDAP_QUERY_COMP_ID).label("LDAP query").defaults(
                                "objectclass=*")
                                .required(false),

                            builder(ButtonActionComponent.class, LDAP_QUERY_BUTTON_ID).setMethod(POST)
                                    .setUrl(getWizardUrl() + "/" + getStageId() + "/probe/" + "ALL")
                                .label("Query")
                );

        return builder(BaseComponent.class).label("LDAP Directory Settings").description("One last step, let's figure out the directory path to your users and groups so we can authenticate them.")
                .subComponents(ldapQueryBox,
                        builder(StringComponent.class, BASE_USER_DN_COMP_ID)
                                .label("Base User DN"),

                        builder(StringComponent.class, BASE_GROUP_DN_COMP_ID)
                                .label("Group User DN"),

                        builder(StringComponent.class,
                                BASE_USERNAME_ATTRI_COMP_ID)
                                .label("User name attribute")
                                .description("Attribute used to designate the user’s name in LDAP. Usually this is uid or cn"),
                        builder(ButtonBoxComponent.class, BUTTON_BOX_ID).subComponents(
                            builder(ButtonActionComponent.class, null)
                                    .setUrl(getWizardUrl() + "/" + LDAP_DIRECTORY_SETTINGS_STAGE_ID)
                                    .setMethod(POST)
                                    .label("Check")
                        )
                );
    }

    @Override
    public String getStageId() {
        return LDAP_DIRECTORY_SETTINGS_STAGE_ID;
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state, LdapConfiguration ldapConfiguration) {
        return new LdapDirectorySettingsStage(wizardUrl, state, ldapConfiguration);
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state) {
        return new LdapDirectorySettingsStage(wizardUrl, state, new LdapConfiguration());
    }
}