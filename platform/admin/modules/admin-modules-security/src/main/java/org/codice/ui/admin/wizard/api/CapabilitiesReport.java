package org.codice.ui.admin.wizard.api;

public class CapabilitiesReport {
    private String configurationType;
    private Class configurationClass;

    public CapabilitiesReport(String configurationType, Class configurationClass){
        this.configurationType = configurationType;
        this.configurationClass = configurationClass;
    }

    public String configurationType() {
        return configurationType;
    }

    public CapabilitiesReport configurationType(String configurationType) {
        this.configurationType = configurationType;
        return this;
    }

    public Class ConfigurationClass() {
        return configurationClass;
    }

    public CapabilitiesReport configurationClass(Class configurationClass) {
        this.configurationClass = configurationClass;
        return this;
    }

}

