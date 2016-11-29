package org.codice.ui.admin.sources.stage;

import static org.codice.ui.admin.sources.stage.SourceWelcomeStage.SOURCES_INITIAL_STAGE_ID;
import static org.codice.ui.admin.wizard.stage.components.ButtonActionComponent.Method.GET;
import static org.codice.ui.admin.wizard.stage.components.Component.builder;

import java.util.List;
import java.util.Map;

import org.codice.ui.admin.sources.config.SourceConfiguration;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.stage.Stage;
import org.codice.ui.admin.wizard.stage.components.BaseComponent;
import org.codice.ui.admin.wizard.stage.components.ButtonActionComponent;
import org.codice.ui.admin.wizard.stage.components.ButtonBoxComponent;
import org.codice.ui.admin.wizard.stage.components.Component;
import org.codice.ui.admin.wizard.stage.components.ResultComponent;

public class SourcesPersistedStage extends Stage<SourceConfiguration> {

    public static final String SOURCES_PERSISTED_STAGE_ID = "SourcesPersistedStage";

    public SourcesPersistedStage(){
        super(null, null, null);
        canGoBack = false;
    }

    public SourcesPersistedStage(String wizardUrl, Map<String, String> state, SourceConfiguration sourceConfiguration){
        super(wizardUrl, state, sourceConfiguration);
        canGoBack = false;
    }

    @Override
    public Stage<SourceConfiguration> probe(Stage<SourceConfiguration> stage, String probeId,
            List<ConfigurationHandler> configurationHandlers) {
        return stage;
    }

    @Override
    public Stage<SourceConfiguration> preconfigureStage(Stage<SourceConfiguration> stageToConfigure, List<ConfigurationHandler> configurationHandlers) {
        return stageToConfigure;
    }

    @Override
    public Stage<SourceConfiguration> testStage(Stage<SourceConfiguration> stageToTest, String stageId,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {
        // TODO: tbatie - 10/31/16 - This
        return stageToTest;
    }

    @Override
    public Stage<SourceConfiguration> commitStage(Stage<SourceConfiguration> stageToCommit,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {
        //TODO: this
        return stageToCommit;
    }

    @Override
    public Component getDefaultRootComponent() {
        // TODO: 11/3/16 Getting null pointer because the stageFactory service calls this on construction whod oesnt have a wizard url
        String homeUrl = getWizardUrl() == null ? "" : getWizardUrl().substring(getWizardUrl().lastIndexOf('/') + 1);

        return builder(BaseComponent.class).label("All Done!")
                .description("Your source has been added successfully")
                .subComponents(
                        builder(ResultComponent.class).succeeded(true),
                        builder(ButtonBoxComponent.class).subComponents(
                            builder(ButtonActionComponent.class).setMethod(GET)
                                    .setUrl(getWizardUrl() + "/" + SOURCES_INITIAL_STAGE_ID)
                                    .label("Add Another Source"),

                            builder(ButtonActionComponent.class).setMethod(GET)
                                    .setUrl(homeUrl)
                                    .label("Go Home")));
    }

    @Override
    public String getStageId() {
        return SOURCES_PERSISTED_STAGE_ID;
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state, SourceConfiguration sourceConfiguration) {
        return new SourcesPersistedStage(wizardUrl, state, sourceConfiguration);
    }

    @Override
    public Stage<SourceConfiguration> getNewInstance(String wizardUrl, Map<String, String> state) {
        return new SourcesDiscoverStage(wizardUrl, state, new SourceConfiguration());
    }
}
