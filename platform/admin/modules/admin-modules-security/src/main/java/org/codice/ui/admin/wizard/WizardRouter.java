package org.codice.ui.admin.wizard;

import static spark.Spark.after;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.post;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.codice.ui.admin.sources.config.SourceInfo;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.api.StageFactory;
import org.codice.ui.admin.wizard.api.Wizard;
import org.codice.ui.admin.wizard.stage.Stage;
import org.codice.ui.admin.wizard.stage.StageComposer;
import org.codice.ui.admin.wizard.stage.components.BaseComponent;
import org.codice.ui.admin.wizard.stage.components.ButtonActionComponent;
import org.codice.ui.admin.wizard.stage.components.ButtonBoxComponent;
import org.codice.ui.admin.wizard.stage.components.Component;
import org.codice.ui.admin.wizard.stage.components.ContextPanelComponent;
import org.codice.ui.admin.wizard.stage.components.ErrorInfoComponent;
import org.codice.ui.admin.wizard.stage.components.HostnameComponent;
import org.codice.ui.admin.wizard.stage.components.InfoComponent;
import org.codice.ui.admin.wizard.stage.components.ListComponent;
import org.codice.ui.admin.wizard.stage.components.ListItemComponent;
import org.codice.ui.admin.wizard.stage.components.PasswordComponent;
import org.codice.ui.admin.wizard.stage.components.PortComponent;
import org.codice.ui.admin.wizard.stage.components.ResultComponent;
import org.codice.ui.admin.wizard.stage.components.SourceInfoComponent;
import org.codice.ui.admin.wizard.stage.components.SourceRadioButtonsComponent;
import org.codice.ui.admin.wizard.stage.components.StringComponent;
import org.codice.ui.admin.wizard.stage.components.StringEnumComponent;
import org.codice.ui.admin.wizard.stage.components.ldap.LdapQueryComponent;
import org.codice.ui.admin.wizard.stage.components.test.TestFailureComponent;
import org.codice.ui.admin.wizard.stage.components.test.TestInfoComponent;
import org.codice.ui.admin.wizard.stage.components.test.TestSuccessComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

import spark.Request;
import spark.servlet.SparkApplication;

public class WizardRouter implements SparkApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(WizardRouter.class);

    public static final String APPLICATION_JSON = "application/json";

    public static String contextPath;

    private List<Wizard> wizards;

    private List<StageFactory> stages;

    private List<ConfigurationHandler> configurationHandlers;

    private Optional<Wizard> findWizard(String id) {
        return wizards.stream()
                .filter(w -> w.getWizardId()
                        .equals(id))
                .findFirst();
    }

    public String getContextPath() {
        return contextPath;
    }

    private static Map<String, Object> toMap(Wizard w) {
        // @formatter:off
        return ImmutableMap.of(
                "id", w.getWizardId(),
                "title", w.getTitle(),
                "description", w.getDescription());
        // @formatter:on
    }

    @Override
    public void init() {

        get("/", (req, res) -> {
            return wizards.stream()
                    .map(WizardRouter::toMap)
                    .collect(Collectors.toList());
        }, new Gson()::toJson);

        get("/:wizardId", (req, res) -> {
            Optional<Wizard> wizard = findWizard(req.params(":wizardId"));

            if (!wizard.isPresent()) {
                res.status(404);
                return null;
            }

            String stageId = wizard.get().initialStageId();

            return wizard.get()
                    .getStageComposer(req.params(":wizardId"), stages, configurationHandlers)
                    .findStage(stageId,
                            getContextPath() + "/" + req.params(":wizardId"),
                            new HashMap<>());

        }, getGsonParser()::toJson);

        get("/:wizardId/:stageId", (req, res) -> {
            Optional<Wizard> wizard = findWizard(req.params(":wizardId"));

            if (!wizard.isPresent()) {
                res.status(404);
                return null;
            }

            StageComposer stageComposer = wizard.get()
                    .getStageComposer(req.params(":wizardId"), stages, configurationHandlers);

            Stage fromRequest = stageComposer.findStage(req.params(":stageId"),
                    getContextPath() + "/" + req.params(":wizardId"),
                    new HashMap<>());

            Stage nextStage = fromRequest.preconfigureStage(fromRequest, configurationHandlers);

            return nextStage;

        }, getGsonParser()::toJson);

        post("/:wizardId/:stageId", (req, res) -> {
            Optional<Wizard> wizard = findWizard(req.params(":wizardId"));

            if (!wizard.isPresent()) {
                res.status(404);
                return null;
            }

            Map<String, String[]> params = req.queryMap()
                    .toMap();
            Stage fromRequest = getStageFromRequest(req);

            StageComposer stageComposer = wizard.get()
                    .getStageComposer(getContextPath() + "/" + req.params(":wizardId"),
                            stages,
                            configurationHandlers);

            Stage nextStage = stageComposer.processStage(fromRequest, params);

            if (nextStage.containsErrors()) {
                res.status(HttpStatus.SC_BAD_REQUEST);
                return nextStage;
            }

            return Arrays.asList(fromRequest, nextStage);
        }, getGsonParser()::toJson);

        post("/:wizardId/:stageId/probe/:probeId", (req, res) -> {
            Optional<Wizard> wizard = findWizard(req.params(":wizardId"));

            if (!wizard.isPresent()) {
                res.status(404);
                return null;
            }

            Map<String, String[]> params = req.queryMap()
                    .toMap();

            StageComposer stageComposer = wizard.get()
                    .getStageComposer(getContextPath() + "/" + req.params(":wizardId"),
                            stages,
                            configurationHandlers);

            Stage nextStage = stageComposer.probeStage(getStageFromRequest(req),
                    req.params("probeId"),
                    params);

            res.status(HttpStatus.SC_BAD_REQUEST);
            return nextStage;
        }, getGsonParser()::toJson);

        after("/*", (req, res) -> res.type(APPLICATION_JSON));

        exception(Exception.class, (ex, req, res) -> {
            LOGGER.error("Wizard router error: ", ex);
            // TODO: tbatie - 10/26/16 - Remove this on merge
            res.status(500);
            res.body(exToJSON(ex));
        });
    }

    private String exToJSON(Exception ex) {
        Map<String, Object> e = new HashMap<>();
        e.put("stackTrace", ex.getStackTrace());
        e.put("cause", ex.toString());
        return new Gson().toJson(e);
    }

    /**
     * This method searches for the stage by it's id, grabs its class and converts the req into that class
     * @param req
     * @return
     */
    public Stage getStageFromRequest(Request req) {
        Stage stageFoundById = stages.stream()
                .filter(stageFactory -> stageFactory.getStageId()
                        .equals(req.params(":stageId")))
                .findFirst()
                .get()
                .getNewInstance(null, null);

        return getGsonParser().fromJson(req.body(), stageFoundById.getClass());
    }

    private static Gson getGsonParser() {
        RuntimeTypeAdapterFactory rtaf = RuntimeTypeAdapterFactory.of(Component.class, "type")
                .registerSubtype(BaseComponent.class, "BASE_CONTAINER")
                .registerSubtype(ButtonActionComponent.class, "BUTTON_ACTION")
                .registerSubtype(HostnameComponent.class, "HOSTNAME")
                .registerSubtype(PasswordComponent.class, "PASSWORD")
                .registerSubtype(PortComponent.class, "PORT")
                .registerSubtype(StringEnumComponent.class, "STRING_ENUM")
                .registerSubtype(StringComponent.class, "STRING")
                .registerSubtype(InfoComponent.class, "INFO")
                .registerSubtype(ResultComponent.class, "STATUS_PAGE")
                .registerSubtype(SourceRadioButtonsComponent.class, "RADIO_BUTTONS")
                .registerSubtype(ContextPanelComponent.class, "CONTEXT_PANEL")
                .registerSubtype(LdapQueryComponent.class, "LDAP_QUERY")
                .registerSubtype(ErrorInfoComponent.class, "ERROR_INFO")
                .registerSubtype(SourceInfoComponent.class, "SOURCE_INFO")
                .registerSubtype(TestFailureComponent.class, "TEST_FAIL")
                .registerSubtype(TestInfoComponent.class, "TEST_INFO")
                .registerSubtype(TestSuccessComponent.class, "TEST_SUCCESS")
                .registerSubtype(ListComponent.class, "LIST")
                .registerSubtype(ListItemComponent.class, "LIST_ITEM")
                .registerSubtype(ButtonBoxComponent.class, "BUTTON_BOX");

        RuntimeTypeAdapterFactory rtaf2 = RuntimeTypeAdapterFactory.of(SourceInfo.class, "type")
                .registerSubtype(SourceInfo.class, "SOURCE_INFO");

        return new GsonBuilder().registerTypeAdapterFactory(rtaf).registerTypeAdapterFactory(rtaf2)
                .create();
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public void setStages(List<StageFactory> stages) {
        this.stages = stages;
    }

    public void setWizards(List<Wizard> wizards) {
        this.wizards = wizards;
    }

    public void setConfigurationHandlers(List<ConfigurationHandler> configurationHandlers) {
        this.configurationHandlers = configurationHandlers;
    }
}
