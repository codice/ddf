package org.codice.ui.admin.sources.stage;

import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SOURCE_NAME;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandler.MANUAL_URL_PROBE_ID;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandler.SOURCE_CONFIGURATION_HANDLER_ID;
import static org.codice.ui.admin.wizard.stage.components.ButtonActionComponent.Method.POST;
import static org.codice.ui.admin.wizard.stage.components.Component.builder;

import java.util.List;
import java.util.Map;

import org.codice.ui.admin.sources.config.SourceConfiguration;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.stage.Stage;
import org.codice.ui.admin.wizard.stage.components.BaseComponent;
import org.codice.ui.admin.wizard.stage.components.ButtonActionComponent;
import org.codice.ui.admin.wizard.stage.components.Component;
import org.codice.ui.admin.wizard.stage.components.SourceInfoComponent;
import org.codice.ui.admin.wizard.stage.components.StringComponent;

import com.google.common.collect.ImmutableMap;

public class SourcesConfirmationStage extends Stage<SourceConfiguration> {

    public static final String SOURCES_CONFIRMATION_STAGE_ID = "SourcesConfirmationStage";
    public static final String SOURCE_NAME_ID = "SourceName";

    public SourcesConfirmationStage(){
        super(null, null, null);
    }

    public SourcesConfirmationStage(String wizardUrl, Map<String, String> state, SourceConfiguration sourceConfiguration){
        super(wizardUrl, state, sourceConfiguration);
    }

    @Override
    public Stage<SourceConfiguration> probe(Stage<SourceConfiguration> stage, String probeId,
            List<ConfigurationHandler> configurationHandlers) {
        SourceConfiguration discoveredConfig = (SourceConfiguration) getConfigurationHandler(configurationHandlers,
                SOURCE_CONFIGURATION_HANDLER_ID).probe(probeId, stage.getConfiguration());
        stage.setConfiguration(discoveredConfig);
        return stage;
    }

    @Override
    public Stage<SourceConfiguration> preconfigureStage(Stage<SourceConfiguration> stageToConfigure,
            List<ConfigurationHandler> configurationHandlers) {

        SourceConfiguration cfg = stageToConfigure.getConfiguration();
        if(cfg.sourceManualUrl() != null) {
            stageToConfigure = probe(stageToConfigure, MANUAL_URL_PROBE_ID, configurationHandlers);
            cfg = stageToConfigure.getConfiguration();
        }

        // TODO: tbatie - 11/4/16 - Create a component that can sets itself by taking in the SourceConfiguration as a parameter
        stageToConfigure.getRootComponent()
                .label("Finalize Source Configuration")
                .description("Name your source, confirm details, and press finish to add source")
                .subComponents(
                        builder(StringComponent.class, SOURCE_NAME_ID)
                                .label("Source Name")
                                .required(true),

                        builder(SourceInfoComponent.class, "Url")
                                .label("Source Address")
                                .value(cfg.selectedSource().getUrl()),

                        builder(SourceInfoComponent.class, "Username")
                                .label("Username")
                                .value(cfg.sourcesUsername() == null ? "none" : cfg.sourcesUsername())
                                .required(false),

                        builder(SourceInfoComponent.class, "Password")
                                .label("Password")
                                .value(cfg.sourcesPassword() == null ? "none" : "******")
                                .required(false),

                        builder(ButtonActionComponent.class)
                                .setMethod(POST)
                                .setUrl(getWizardUrl() + "/" + SOURCES_CONFIRMATION_STAGE_ID)
                                .label("Finish")
                );

        return stageToConfigure;
    }

    @Override
    public Stage<SourceConfiguration> testStage(Stage<SourceConfiguration> stageToTest, String testId, List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {
        //TODO: this
        return stageToTest;
    }

    @Override
    public Stage<SourceConfiguration> commitStage(Stage<SourceConfiguration> stageToCommit,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {
        stageToCommit.setConfiguration(getConfigurationFromStage(ImmutableMap.of(SOURCE_NAME_ID, SOURCE_NAME), stageToCommit));
        getConfigurationHandler(configurationHandlers, SOURCE_CONFIGURATION_HANDLER_ID).persist(stageToCommit.getConfiguration());
        return stageToCommit;
    }

    @Override
    public Component getDefaultRootComponent() {
        return Component.builder(BaseComponent.class, "4");

    }

    @Override
    public String getStageId() {
        return SOURCES_CONFIRMATION_STAGE_ID;
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state, SourceConfiguration sourceConfiguration) {
        return new SourcesConfirmationStage(wizardUrl, state, sourceConfiguration);
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state) {
        return new SourcesConfirmationStage(wizardUrl, state, new SourceConfiguration());
    }
}
