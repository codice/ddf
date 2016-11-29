package org.codice.ui.admin.ldap.stage;

import static org.codice.ui.admin.wizard.stage.components.ButtonActionComponent.Method.POST;
import static org.codice.ui.admin.wizard.stage.components.Component.builder;

import java.util.List;
import java.util.Map;

import org.codice.ui.admin.ldap.config.LdapConfiguration;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.stage.Stage;
import org.codice.ui.admin.wizard.stage.components.BaseComponent;
import org.codice.ui.admin.wizard.stage.components.ButtonActionComponent;
import org.codice.ui.admin.wizard.stage.components.Component;
import org.codice.ui.admin.wizard.stage.components.InfoComponent;

public class LdapIntroductionStage extends Stage<LdapConfiguration> {

    public static final String LDAP_INTRODUCTION_STAGE = "LdapIntroductionStage";

    public LdapIntroductionStage(){
        super(null, null, null);
        canGoBack = false;
    }

    public LdapIntroductionStage(String wizardUrl, Map<String, String> state, LdapConfiguration sourceConfiguration){
        super(wizardUrl, state, sourceConfiguration);
        canGoBack = false;
    }

    @Override
    public Stage probe(Stage<LdapConfiguration> stage, String probeId,
            List<ConfigurationHandler> configurationHandlers) {
        return stage;
    }

    @Override
    public Stage preconfigureStage(Stage<LdapConfiguration> stageToConfigure,
            List<ConfigurationHandler> configurationHandlers) {
        return stageToConfigure;
    }

    @Override
    public Stage testStage(Stage<LdapConfiguration> stageToTest, String testId, List<ConfigurationHandler> configurationHandlers,
            Map<String, String[]> params) {
        return stageToTest;
    }

    @Override
    public Stage commitStage(Stage<LdapConfiguration> stageToCommit,
            List<ConfigurationHandler> configurationHandlers, Map<String, String[]> params) {
        return stageToCommit;
    }

    @Override
    public Component getDefaultRootComponent() {
        return builder(BaseComponent.class)
                .subComponents(
                        builder(InfoComponent.class)
                                .label("Welcome to the LDAP Configuration Wizard")
                                .value("This guide will walk through setting up the LDAP as an authentication source for users. To begin, make sure you have the hostname and port of the LDAP you plan to configure."),

                        builder(ButtonActionComponent.class)
                                .setMethod(POST)
                                .setUrl(getWizardUrl() + "/" + getStageId())
                                .label("Begin LDAP Setup")
                );
    }

    @Override
    public String getStageId() {
        return LDAP_INTRODUCTION_STAGE;
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state, LdapConfiguration sourceConfiguration) {
        return new LdapIntroductionStage(wizardUrl, state, sourceConfiguration);
    }

    @Override
    public Stage getNewInstance(String wizardUrl, Map<String, String> state) {
        return new LdapIntroductionStage(wizardUrl, state, new LdapConfiguration());
    }
}
