package org.codice.ui.admin.sources.stage;

import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SOURCE_HOSTNAME;
import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SOURCE_PASSWORD;
import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SOURCE_PORT;
import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SOURCE_USERNAME;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandler.SOURCE_CONFIGURATION_HANDLER_ID;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandler.VALID_URL_TEST_ID;
import static org.codice.ui.admin.wizard.stage.components.ButtonActionComponent.Method.POST;
import static org.codice.ui.admin.wizard.stage.components.Component.builder;
import static org.codice.ui.admin.wizard.stage.components.test.TestComponent.createTestComponent;

import java.util.List;
import java.util.Map;

import org.codice.ui.admin.sources.config.SourceConfiguration;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.api.ConfigurationTestMessage;
import org.codice.ui.admin.wizard.stage.Stage;
import org.codice.ui.admin.wizard.stage.components.BaseComponent;
import org.codice.ui.admin.wizard.stage.components.ButtonActionComponent;
import org.codice.ui.admin.wizard.stage.components.Component;
import org.codice.ui.admin.wizard.stage.components.HostnameComponent;
import org.codice.ui.admin.wizard.stage.components.PasswordComponent;
import org.codice.ui.admin.wizard.stage.components.PortComponent;
import org.codice.ui.admin.wizard.stage.components.StringComponent;

import com.google.common.collect.ImmutableMap;

public class SourcesDiscoverStage extends Stage<SourceConfiguration> {

    public static final String SOURCES_DISCOVER_STAGE_ID = "SourcesDiscoverStage";

    public static final String SOURCE_HOSTNAME_ID = "sourceHostnameId";
    public static final String SOURCE_PORT_ID = "sourcePortId";
    public static final String SOURCE_USERNAME_ID = "sourceUsernameId";
    public static final String SOURCE_PASSWORD_ID = "sourcePasswordId";

    Map<String, SourceConfiguration.SOURCE_CONFIG_KEYS> componentToConfigIds = ImmutableMap.of(SOURCE_HOSTNAME_ID, SOURCE_HOSTNAME,
                SOURCE_PORT_ID, SOURCE_PORT,
                SOURCE_USERNAME_ID, SOURCE_USERNAME,
                SOURCE_PASSWORD_ID, SOURCE_PASSWORD);

    public SourcesDiscoverStage(){
        super(null, null, null);
    }

    public SourcesDiscoverStage(String wizardUrl, Map<String, String> state, SourceConfiguration sourceConfiguration){
        super(wizardUrl, state, sourceConfiguration);
    }

    @Override
    public Stage<SourceConfiguration> preconfigureStage(Stage<SourceConfiguration> stageToConfigure,
            List<ConfigurationHandler> configurationHandlers) {
            return stageToConfigure;
    }

    @Override
    public Stage<SourceConfiguration> probe(Stage<SourceConfiguration> stage, String probeId,
            List<ConfigurationHandler> configurationHandlers) {
        return stage;
    }

    @Override
    public Stage<SourceConfiguration> validateStage(Stage<SourceConfiguration> stageToCheck, Map<String, String[]> params) {
        stageToCheck.validateComponents();
        return stageToCheck;
    }

    @Override
    public Stage<SourceConfiguration> testStage(Stage<SourceConfiguration> stageToTest, String testId, List<ConfigurationHandler> configurationHandlers,
            Map<String, String[]> params) {
        SourceConfiguration testConfiguration = getConfigurationFromStage(componentToConfigIds, stageToTest);
        List<ConfigurationTestMessage>testResults = getConfigurationHandler(configurationHandlers,
                SOURCE_CONFIGURATION_HANDLER_ID).test(VALID_URL_TEST_ID,
                testConfiguration);

        if (!testResults.isEmpty()) {
            for (ConfigurationTestMessage testMsg : testResults) {
                stageToTest.getRootComponent()
                        .subComponents(createTestComponent(testMsg));
            }
        }
        return stageToTest;
    }

    @Override
    public Stage<SourceConfiguration> commitStage(Stage<SourceConfiguration> stageToCommit,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {
        stageToCommit.setConfiguration(getConfigurationFromStage(componentToConfigIds, stageToCommit));
        return stageToCommit;
    }

    @Override
    public Component getDefaultRootComponent() {
        return builder(BaseComponent.class)
                .label("Discover Available Sources")
                .description("Enter source information to scan for available sources")
                .subComponents(
                        builder(HostnameComponent.class, SOURCE_HOSTNAME_ID)
                                .label("Hostname"),

                        builder(PortComponent.class, SOURCE_PORT_ID)
                                .defaults(8993.0)
                                .value(8993.0).
                                label("Port"),

                        builder(StringComponent.class, SOURCE_USERNAME_ID)
                                .label("Username (optional)")
                                .required(false),

                        builder(PasswordComponent.class, SOURCE_PASSWORD_ID)
                                .label("Password (optional)")
                                .required(false),

                        builder(ButtonActionComponent.class)
                                .setMethod(POST)
                                .setUrl(getWizardUrl() + "/" + getStageId())
                                .label("Check")
        );
    }

    @Override
    public String getStageId() {
        return SOURCES_DISCOVER_STAGE_ID;
    }

    @Override
    public Stage<SourceConfiguration> getNewInstance(String wizardUrl, Map<String, String> state, SourceConfiguration sourceConfiguration) {
        return new SourcesDiscoverStage(wizardUrl, state, sourceConfiguration);
    }

    @Override
    public Stage<SourceConfiguration> getNewInstance(String wizardUrl, Map<String, String> state) {
        return new SourcesDiscoverStage(wizardUrl, state, new SourceConfiguration());
    }
}
