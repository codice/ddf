package org.codice.ui.admin.sources.stage;

import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SELECTED_SOURCE;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandler.DISCOVER_SOURCES_ID;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandler.SOURCE_CONFIGURATION_HANDLER_ID;
import static org.codice.ui.admin.sources.stage.SourcesAdvancedStage.SOURCES_ADVANCED_STAGE_ID;
import static org.codice.ui.admin.wizard.stage.components.ButtonActionComponent.Method.GET;
import static org.codice.ui.admin.wizard.stage.components.ButtonActionComponent.Method.POST;
import static org.codice.ui.admin.wizard.stage.components.Component.builder;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.codice.ui.admin.sources.config.SourceConfiguration;
import org.codice.ui.admin.sources.config.SourceInfo;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.stage.Stage;
import org.codice.ui.admin.wizard.stage.components.BaseComponent;
import org.codice.ui.admin.wizard.stage.components.ButtonActionComponent;
import org.codice.ui.admin.wizard.stage.components.Component;
import org.codice.ui.admin.wizard.stage.components.SourceRadioButtonsComponent;

import com.google.common.collect.ImmutableMap;

public class SourcesResultsStage extends Stage<SourceConfiguration> {

    public static final String SOURCES_RESULTS_STAGE_ID = "SourcesResultsStage";

    public static final String AVAILABLE_SOURCES_RADIO_ID = "AvailableSourcesRadio";

    private static final Map<String, SourceConfiguration.SOURCE_CONFIG_KEYS> componentToConfig = ImmutableMap.of(
            AVAILABLE_SOURCES_RADIO_ID,
            SELECTED_SOURCE);

    public SourcesResultsStage(){
        super(null, null, null);
    }

    public SourcesResultsStage(String wizardUrl, Map<String, String> state, SourceConfiguration sourceConfiguration){
        super(wizardUrl, state, sourceConfiguration);
    }

    @Override
    public Stage<SourceConfiguration> probe(Stage<SourceConfiguration> stage, String probeId,
            List<ConfigurationHandler> configurationHandlers) {

        // TODO: 11/3/16 Not sure if we should be adding all of the config to the stage or just what we wanted to discover from probing
        SourceConfiguration discoveredConfig = (SourceConfiguration) getConfigurationHandler(configurationHandlers,
                SOURCE_CONFIGURATION_HANDLER_ID).probe(probeId, stage.getConfiguration());
        stage.setConfiguration(discoveredConfig);


        List<SourceInfo> results = stage.getConfiguration().sourcesDiscoveredSources();

        if (results != null && !results.isEmpty()) {
            stage.getRootComponent()
                    .label("Sources Found!")
                    .description("Choose which source to add")
                    .subComponents(
                            builder(SourceRadioButtonsComponent.class, AVAILABLE_SOURCES_RADIO_ID).setOptions(
                                results.stream().collect(Collectors.toMap(SourceInfo::getSourceType,
                                        Function.identity()))
                            ),

                            builder(ButtonActionComponent.class).setMethod(POST)
                                    .setUrl(getWizardUrl() + "/" + SOURCES_RESULTS_STAGE_ID)
                                    .label("Next"));
        } else {
            stage.getRootComponent()
                    .label("No Sources Found")
                    .description("Go back to edit sources or input sources manually")
                    .subComponents(
                            builder(ButtonActionComponent.class).setMethod(GET)
                                    .setUrl(getWizardUrl() + "/" + SOURCES_ADVANCED_STAGE_ID)
                                    .label("Enter Source URL Manually"));
        }

        return stage;
    }

    @Override
    public Stage<SourceConfiguration> preconfigureStage(Stage<SourceConfiguration> stageToConfigure,
            List<ConfigurationHandler> configurationHandlers) {
        return probe(stageToConfigure, DISCOVER_SOURCES_ID, configurationHandlers);
    }

    @Override
    public Stage<SourceConfiguration> testStage(Stage<SourceConfiguration> stageToTest, String testId, List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {
        //TODO: this
        return stageToTest;
    }

    @Override
    public Stage<SourceConfiguration> commitStage(Stage<SourceConfiguration> stageToCommit,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {
        stageToCommit.setConfiguration(getConfigurationFromStage(componentToConfig, stageToCommit));
        return stageToCommit;
    }

    @Override
    public Component getDefaultRootComponent() {
        return Component.builder(BaseComponent.class).label("3");
    }

    @Override
    public String getStageId() {
        return SOURCES_RESULTS_STAGE_ID;
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state, SourceConfiguration sourceConfiguration) {
        return new SourcesResultsStage(wizardUrl, state, sourceConfiguration);
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state) {
        return new SourcesResultsStage(wizardUrl, state, new SourceConfiguration());
    }
}
