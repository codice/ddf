/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.registry.report.action.provider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.Action;
import ddf.action.MultiActionProvider;
import ddf.action.impl.ActionImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.source.Source;

public class RegistryReportActionProvider implements MultiActionProvider {

    private static final String REGISTRY_PATH = "/registries";

    private static final String REPORT_PATH = "/report";

    private static final String FORMAT = ".html";

    private static final String SOURCE_ID_QUERY_PARAM = "?sourceId=";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RegistryReportActionProvider.class);

    private final String actionProviderId;

    private ConfigurationAdmin configurationAdmin;

    private String description;

    private String title;

    public RegistryReportActionProvider(String id) {
        this.actionProviderId = id;
    }

    public <T> List<Action> getActions(T subject) {

        if (canHandle(subject)) {
            if (subject instanceof Metacard) {
                return processSubject((Metacard) subject);
            } else if (subject instanceof Source) {
                return processSubject((Source) subject);
            } else if (subject instanceof Configuration) {
                return processSubject((Configuration) subject);
            }
        }
        return Collections.emptyList();
    }

    private List<Action> processSubject(Metacard metacard) {

        if (metacard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID) == null
                || StringUtils.isBlank(metacard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                .getValue()
                .toString())) {
            LOGGER.debug("No registry id given. No action to provide.");
            return Collections.emptyList();
        }

        try {
            String registryId =
                    URLEncoder.encode(metacard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                            .getValue()
                            .toString(), (StandardCharsets.UTF_8).toString());
            String sourceId = URLEncoder.encode(getSource(metacard),
                    (StandardCharsets.UTF_8).toString());
            Action action = getAction(registryId, sourceId);

            if (action == null) {
                return Collections.emptyList();
            }

            return Collections.singletonList(action);

        } catch (UnsupportedEncodingException e) {
            LOGGER.debug("Unsupported Encoding exception", e);
            return Collections.emptyList();
        }
    }

    private List<Action> processSubject(Source source) {
        String configId = source.getId();
        if (configId != null) {
            Configuration[] configurations;
            try {

                configurations = configurationAdmin.listConfigurations(String.format("(id=%s)",
                        configId));

                if (configurations.length > 0) {
                    for (Configuration configuration : configurations) {
                        if (configuration.getProperties()
                                .get(RegistryObjectMetacardType.REGISTRY_ID) != null) {
                            return processSubject(configuration);
                        }
                    }
                }
            } catch (IOException | InvalidSyntaxException e) {
                LOGGER.debug("Unable to access source configurations", e);
            }
        }
        return Collections.emptyList();
    }

    private List<Action> processSubject(Configuration configuration) {
        if (configuration.getProperties()
                .get(RegistryObjectMetacardType.REGISTRY_ID) != null) {
            try {
                String registryId = URLEncoder.encode(configuration.getProperties()
                        .get(RegistryObjectMetacardType.REGISTRY_ID)
                        .toString(), (StandardCharsets.UTF_8).toString());
                Action action = getAction(registryId, "");

                if (action == null) {
                    return Collections.emptyList();
                }

                return Collections.singletonList(action);
            } catch (UnsupportedEncodingException e) {
                LOGGER.debug("Unsupported Encoding exception", e);
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    private Action getAction(String metacardId, String sourceId) {

        if (StringUtils.isNotBlank(sourceId)) {
            sourceId = SOURCE_ID_QUERY_PARAM + sourceId;
        }

        URL url;
        try {

            URI uri = new URI(SystemBaseUrl.constructUrl(String.format("%s/%s%s%s%s",
                    REGISTRY_PATH, metacardId, REPORT_PATH, FORMAT, sourceId), true));
            url = uri.toURL();

        } catch (MalformedURLException e) {
            LOGGER.debug("Malformed URL exception", e);
            return null;
        } catch (URISyntaxException e) {
            LOGGER.debug("URI Syntax exception", e);
            return null;
        }

        return new ActionImpl(getId(), title, description, url);
    }

    public String getId() {
        return this.actionProviderId;
    }

    public <T> boolean canHandle(T subject) {

        return (subject instanceof Metacard && ((Metacard) subject).getTags()
                .contains(RegistryConstants.REGISTRY_TAG)) || subject instanceof Source || (
                subject instanceof Configuration && ((Configuration) subject).getProperties()
                        .get(RegistryObjectMetacardType.REGISTRY_ID) != null);

    }

    protected String getSource(Metacard metacard) {

        if (StringUtils.isNotBlank(metacard.getSourceId())) {
            return metacard.getSourceId();
        }

        return "";
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
