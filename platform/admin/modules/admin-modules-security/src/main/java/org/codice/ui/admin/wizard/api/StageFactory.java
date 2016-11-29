package org.codice.ui.admin.wizard.api;

import java.util.Map;

import org.codice.ui.admin.wizard.config.Configuration;
import org.codice.ui.admin.wizard.stage.Stage;

public interface StageFactory<S extends Configuration> {

    String getStageId();
    // TODO: tbatie - 11/2/16 - We need to break this out into a seperate service or change the way we register stages instead of keeping it within the stage
    Stage getNewInstance(String wizardUrl, Map<String, String> state, S configuration);

    Stage getNewInstance(String wizardUrl, Map<String, String> state);
}
