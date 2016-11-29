package org.codice.ui.admin.wizard.api;

import java.util.List;

import org.codice.ui.admin.wizard.stage.StageComposer;

public interface Wizard {

    StageComposer getStageComposer(String contextPath, List<StageFactory> stages, List<ConfigurationHandler> configurationHandlers);

    String getWizardId();

    String getTitle();

    String getDescription();

    String initialStageId();
}
