package org.codice.ui.admin.wizard.stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.api.StageFactory;
import org.codice.ui.admin.wizard.config.Configuration;
import org.codice.ui.admin.wizard.stage.components.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Stage<S extends Configuration> implements StageFactory<S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Stage.class);

    /**
     * Adding this id as a key to the state with a stageId as a value will result in dictating which stage the stage composer should look up next. Should be set during commitStage method
     */
    public static final String NEXT_STAGE_ID = "nextStageId";

    private String wizardUrl;

    protected boolean canGoBack = true;

    /**
     * The configuration object that is built up and persisted between stages
     */
    protected S configuration;

    /**
     * The component that will be rendered in the UI
     */
    private Component rootComponent;

    /**
     * A map used to indicate status of the wizard and any type of useful information along the lifecycle of the stages
     */
    private Map<String, String> state;

    public Stage(S configuration) {
        state = new HashMap<>();
        rootComponent = getDefaultRootComponent();
        this.configuration = configuration;
    }

    public Stage(String wizardUrl, Map<String, String> state, S configuration) {
        this.wizardUrl = wizardUrl;
        this.state = state;
        this.configuration = configuration;
        rootComponent = getDefaultRootComponent();
    }

    /**
     * This method is not apart of the stage life cycle and is used to retrieve information in the system to inform the user depending on the probeId and the stage's content
     *
     * @param stage - The stage to configure before being return to the user
     * @return Informed stage
     */
    public abstract Stage<S> probe(Stage<S> stage, String probeId, List<ConfigurationHandler> configurationHandlers);

    /**
     * This method is invoked when a new stage is returned from the stageComposer. This method should perform default value look and population as well as any any additional preconfiguration
     *
     * @param stageToConfigure - The stage to configure before being return to the user
     * @return Preconfigured stage
     */
    public abstract Stage<S> preconfigureStage(Stage<S> stageToConfigure, List<ConfigurationHandler> configurationHandlers);

    /**
     * Invokes the components of the stage to validate themselves and performs any additional field validation and testing
     *
     * @param stageToCheck - Instance of the stage object to validate
     * @param params       - parameters of request
     * @return stage that may contain component validation errors
     */
    public Stage<S> validateStage(Stage<S> stageToCheck, Map<String, String[]> params) {
        stageToCheck.validateComponents();
        return stageToCheck;
    }

    /**
     * Tests the fields of the stage to provide default values and verify inputs
     *
     * @param stageToTest - Instance of the stage object to test
     * @param params      - parameters of request
     * @return stage that may contain test errors
     */
    public abstract Stage<S> testStage(Stage<S> stageToTest, String stageId,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params);

    /**
     * Persists the information from the stage to the stage configuration or to the backend
     *
     * @param stageToPersist - Instance of the stage to persist
     * @param configurationHandlers
     * @param params         - parameters of request
     * @return stage that may contain errors that resulted from persisting
     */
    public abstract Stage<S> commitStage(Stage<S> stageToPersist,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params);

    /**
     * The original root component that should be created on new instances of the stage
     *
     * @return root component
     */
    public abstract Component getDefaultRootComponent();

    public ConfigurationHandler getConfigurationHandler(
            List<ConfigurationHandler> configurationHandlers, String configurationId) {
        Optional<ConfigurationHandler> foundConfigHandler = configurationHandlers.stream()
                .filter(handler -> handler.getConfigurationHandlerId()
                        .equals(configurationId))
                .findFirst();

        // TODO: tbatie - 10/25/16 - Return null or throw exception?
        return foundConfigHandler.isPresent() ? foundConfigHandler.get() : null;
    }

    public Map<String, String> getState() {
        return state;
    }

    public void setState(Map<String, String> state) {
        this.state = state;
    }

    public S getConfiguration() {
        return configuration;
    }

    public void setConfiguration(S configuration) {
        this.configuration = configuration;
    }

    public Component getRootComponent() {
        return rootComponent;
    }


    public <T extends Component> Optional<T> getComponent(String componentId, Class<T> clazz) {
        Component component = getComponent(componentId);

        if (clazz.isInstance(component)) {
            return Optional.of((T) component);
        }

        return Optional.empty();
    }

    public Map<String, Object> getComponentValues() {
        return rootComponent.getComponentValues();
    }


    public void validateComponents() {
        rootComponent.validateAll();
    }

    public Component getComponent(String componentId) {
        return rootComponent.getComponent(componentId);
    }

    public String getWizardUrl() {
        return wizardUrl;
    }

    public boolean containsErrors() {
        return rootComponent.containsErrors();
    }

    public void clearErrors() {
        rootComponent.clearAllErrors();
    }

    /**
     * Uses the component id's to retrieve the component value and place them in the stages configuration
     * @param componentToConfiguration - Mape with component id -> configuration id
     * @param stage - Stages map to retrieve components and configuration from
     * @param <E> - Configuration enum key
     * @return Configuration of with values added from the component
     */
    public<E extends Enum> S getConfigurationFromStage(Map<String, E> componentToConfiguration, Stage<S> stage) {
        S newConfig = (S) stage.getConfiguration().copy();
        for(Map.Entry<String, E> entry : componentToConfiguration.entrySet()) {
            Object componentValue = stage.getComponent(entry.getKey()).getValue();
            newConfig.addValue(entry.getValue(), componentValue);
        }

        return newConfig;
    }
}
