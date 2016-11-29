package org.codice.ui.admin.sources.stage;

import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SOURCE_MANUAL_URL;
import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SOURCE_MANUAL_URL_TYPE;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandler.GENERIC_TYPES;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandler.MANUAL_URL_TEST_ID;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandler.SOURCE_CONFIGURATION_HANDLER_ID;
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
import org.codice.ui.admin.wizard.stage.components.ButtonBoxComponent;
import org.codice.ui.admin.wizard.stage.components.Component;
import org.codice.ui.admin.wizard.stage.components.StringComponent;
import org.codice.ui.admin.wizard.stage.components.StringEnumComponent;

import com.google.common.collect.ImmutableMap;

public class SourcesAdvancedStage extends Stage<SourceConfiguration> {

    public static final String SOURCES_ADVANCED_STAGE_ID = "SourcesAdvancedStage";
    public static final String SOURCE_MANUAL_URL_TYPE_ID = "sourceManualUrlType";
    public static final String SOURCE_MANUAL_URL_ID = "sourceManualUrl";
    public static final String BUTTON_BOX_ID = "buttonBox";
    public static final String SKIP_TEST_BUTTON_ID = "skipTestButton";

    Map<String, SourceConfiguration.SOURCE_CONFIG_KEYS> componentToConfigIds = ImmutableMap.of(
            SOURCE_MANUAL_URL_TYPE_ID, SOURCE_MANUAL_URL_TYPE,
            SOURCE_MANUAL_URL_ID, SOURCE_MANUAL_URL);

    public SourcesAdvancedStage(){
        super(null, null, null);
    }

    public SourcesAdvancedStage(String wizardUrl, Map<String, String> state, SourceConfiguration sourceConfiguration){
        super(wizardUrl, state, sourceConfiguration);
    }

    @Override
    public Stage<SourceConfiguration> probe(Stage<SourceConfiguration> stage, String probeId,
            List<ConfigurationHandler> configurationHandlers) {
        return stage;
    }

    @Override
    public Stage<SourceConfiguration> preconfigureStage(Stage<SourceConfiguration> stageToConfigure,
            List<ConfigurationHandler> configurationHandlers) {
        return stageToConfigure;
    }

    @Override
    public Stage<SourceConfiguration> testStage(Stage<SourceConfiguration> stageToTest, String testId, List<ConfigurationHandler> configurationHandlers,
            Map<String, String[]> params) {
        SourceConfiguration testConfiguration = getConfigurationFromStage(componentToConfigIds, stageToTest);
        List<ConfigurationTestMessage>testResults = getConfigurationHandler(configurationHandlers,
                SOURCE_CONFIGURATION_HANDLER_ID)
                .test(MANUAL_URL_TEST_ID, testConfiguration);

        if (!testResults.isEmpty()) {
            for(ConfigurationTestMessage testMsg : testResults) {
                stageToTest.getRootComponent()
                        .subComponents(createTestComponent(testMsg));
            }
            if(stageToTest.containsErrors() && stageToTest.getComponent(SKIP_TEST_BUTTON_ID) == null) {
                stageToTest.getComponent(BUTTON_BOX_ID).subComponents(
                        builder(ButtonActionComponent.class, SKIP_TEST_BUTTON_ID)
                                .setMethod(POST)
                                .setUrl(getWizardUrl() + "/" + getStageId() + "?skipTest=true")
                                .label("Skip Check and Add")
                );
            }
        }
        return stageToTest;
    }

    @Override
    public Stage<SourceConfiguration> commitStage(Stage<SourceConfiguration> stageToCommit,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {
        SourceConfiguration commitConfiguration = getConfigurationFromStage(ImmutableMap.of(
                SOURCE_MANUAL_URL_ID, SOURCE_MANUAL_URL,
                SOURCE_MANUAL_URL_TYPE_ID, SOURCE_MANUAL_URL_TYPE),
                stageToCommit);
        stageToCommit.setConfiguration(commitConfiguration);
        return stageToCommit;
    }

    @Override
    public Component getDefaultRootComponent() {
        return builder(BaseComponent.class).label("Manual Source Entry").value("Choose a source configuration type and enter a source URL.")
                .subComponents(
                        builder(StringEnumComponent.class, SOURCE_MANUAL_URL_TYPE_ID)
                                .label("Source Configuration Type")
                                .defaults(GENERIC_TYPES)
                                .value(GENERIC_TYPES.get(0)),
                        builder(StringComponent.class, SOURCE_MANUAL_URL_ID)
                                .label("Source URL"),
                        builder(ButtonBoxComponent.class, BUTTON_BOX_ID).subComponents(
                                builder(ButtonActionComponent.class)
                                        .setMethod(POST)
                                        .setUrl(getWizardUrl() + "/" + getStageId())
                                        .label("Check URL")
                        )
                );
    }

    @Override
    public String getStageId() {
        return SOURCES_ADVANCED_STAGE_ID;
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state, SourceConfiguration sourceConfiguration) {
        return new SourcesAdvancedStage(wizardUrl, state, sourceConfiguration);
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state) {
        return new SourcesAdvancedStage(wizardUrl, state, new SourceConfiguration());
    }
}
