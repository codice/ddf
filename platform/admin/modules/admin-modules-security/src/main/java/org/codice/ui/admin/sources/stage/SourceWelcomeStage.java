package org.codice.ui.admin.sources.stage;

import static org.codice.ui.admin.sources.stage.SourcesAdvancedStage.SOURCES_ADVANCED_STAGE_ID;
import static org.codice.ui.admin.wizard.stage.components.ButtonActionComponent.Method.GET;
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
import org.codice.ui.admin.wizard.stage.components.InfoComponent;

public class SourceWelcomeStage extends Stage<SourceConfiguration> {

    public static final String SOURCES_INITIAL_STAGE_ID = "SourceWelcomeStage";

    public SourceWelcomeStage(){
        super(null, null, null);
        canGoBack = false;
    }

    public SourceWelcomeStage(String wizardUrl, Map<String, String> state, SourceConfiguration sourceConfiguration){
        super(wizardUrl, state, sourceConfiguration);
        canGoBack = false;
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
        return stageToTest;
    }

    @Override
    public Stage<SourceConfiguration> commitStage(Stage<SourceConfiguration> stageToCommit,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {
        return stageToCommit;
    }

    @Override
    public Component getDefaultRootComponent() {
        return builder(BaseComponent.class)
                .subComponents(
                        builder(InfoComponent.class)
                                .label("Welcome to the Source Configuration Wizard")
                                .value("This guide will walk you through the discovery and configuration of the various sources that are used to query metadata in other DDF's or external systems. To begin, make sure you have the hostname and port of the source you plan to configure."),

                        builder(ButtonActionComponent.class)
                                .setMethod(POST)
                                .setUrl(getWizardUrl() + "/" + getStageId())
                                .label("Begin Source Setup")
                );
    }

    @Override
    public String getStageId() {
        return SOURCES_INITIAL_STAGE_ID;
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state, SourceConfiguration sourceConfiguration) {
        return new SourceWelcomeStage(wizardUrl, state, sourceConfiguration);
    }

    @Override
    public Stage<SourceConfiguration> getNewInstance(String wizardUrl, Map<String, String> state) {
        return new SourceWelcomeStage(wizardUrl, state, new SourceConfiguration());
    }
}
