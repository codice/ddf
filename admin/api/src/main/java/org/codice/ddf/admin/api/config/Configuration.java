/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.admin.api.config;

public abstract class Configuration {

    public static final String CONFIGURATION_HANDLER_ID = "configurationHandlerId";
    public static final String FACTORY_PID = "factoryPid";
    public static final String SERVICE_PID = "servicePid";

    private String factoryPid;
    private String servicePid;
    private String configurationHandlerId;

    public Configuration() {
    }

    public Configuration(Configuration configuration) {
        this.factoryPid = configuration.factoryPid;
        this.servicePid = configuration.servicePid;
        this.configurationHandlerId = configuration.configurationHandlerId;
    }

    public abstract ConfigurationType getConfigurationType();

    //Getters
    public String configurationHandlerId() {
        return configurationHandlerId;
    }
    public String servicePid(){
        return servicePid;
    }
    public String factoryPid() {
        return factoryPid;
    }

    //Setters
    public Configuration configurationHandlerId(String configurationHandlerId) {
        this.configurationHandlerId = configurationHandlerId;
        return this;
    }
    public Configuration servicePid(String servicePid) {
        this.servicePid = servicePid;
        return this;
    }
    public Configuration factoryPid(String factoryPid) {
        this.factoryPid = factoryPid;
        return this;
    }


}
