package org.codice.ui.admin.ldap;

import static org.codice.ui.admin.ldap.stage.LdapBindHostSettingsStage.LDAP_BIND_HOST_SETTINGS_STAGE_ID;
import static org.codice.ui.admin.ldap.stage.LdapDirectorySettingsStage.LDAP_DIRECTORY_SETTINGS_STAGE_ID;
import static org.codice.ui.admin.ldap.stage.LdapIntroductionStage.LDAP_INTRODUCTION_STAGE;
import static org.codice.ui.admin.ldap.stage.LdapNetworkSettingsStage.LDAP_NETWORK_SETTINGS_STAGE_ID;
import static org.codice.ui.admin.ldap.stage.LdapSettingsConfirmationStage.LDAP_CONFIRMATION_STAGE;
import static org.codice.ui.admin.ldap.stage.LdapSuccessStage.LDAP_SUCCESS_STAGE;

import java.util.List;

import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.api.StageFactory;
import org.codice.ui.admin.wizard.api.Wizard;
import org.codice.ui.admin.wizard.stage.StageComposer;

public class LdapWizard implements Wizard {

    @Override
    public String getTitle() {
        return "LDAP Wizard";
    }

    @Override
    public String getDescription() {
        return "Help setup that thing called LDAP!";
    }

    @Override
    public String initialStageId() {
        return LDAP_INTRODUCTION_STAGE;
    }

    @Override
    public StageComposer getStageComposer(String contextPath, List<StageFactory> allStages,
            List<ConfigurationHandler> configurationHandlers) {
        return StageComposer.builder(contextPath, allStages, configurationHandlers)
                .link(LDAP_INTRODUCTION_STAGE, LDAP_NETWORK_SETTINGS_STAGE_ID)
                .link(LDAP_NETWORK_SETTINGS_STAGE_ID, LDAP_BIND_HOST_SETTINGS_STAGE_ID)
                .link(LDAP_BIND_HOST_SETTINGS_STAGE_ID, LDAP_DIRECTORY_SETTINGS_STAGE_ID)
                .link(LDAP_DIRECTORY_SETTINGS_STAGE_ID, LDAP_CONFIRMATION_STAGE)
                .link(LDAP_CONFIRMATION_STAGE, LDAP_SUCCESS_STAGE);
    }

    @Override
    public String getWizardId() {
        return "ldap";
    }
}
