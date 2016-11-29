package org.codice.ui.admin.sources.config;

import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SELECTED_SOURCE;
import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SOURCES_CSW_URL;
import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SOURCES_DISCOVERED;
import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SOURCE_HOSTNAME;
import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SOURCE_MANUAL_URL;
import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SOURCE_MANUAL_URL_TYPE;
import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SOURCE_NAME;
import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SOURCE_PASSWORD;
import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SOURCE_PORT;
import static org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS.SOURCE_USERNAME;

import java.util.List;
import java.util.Map;

import org.codice.ui.admin.ldap.config.LdapConfiguration;
import org.codice.ui.admin.wizard.config.Configuration;
import org.codice.ui.admin.sources.config.SourceConfiguration.SOURCE_CONFIG_KEYS;

import com.google.gson.internal.LinkedTreeMap;

public class SourceConfiguration extends Configuration<SOURCE_CONFIG_KEYS> {
    public String sourcesPassword() {
        return getValue(SOURCE_PASSWORD) == null ? null : (String) getValue(SOURCE_PASSWORD);
    }

    public String sourcesUsername() {
        return getValue(SOURCE_USERNAME) == null ? null : (String) getValue(SOURCE_USERNAME);
    }

    public String sourcesCswUrl() {
        return getValue(SOURCES_CSW_URL) == null ? null : (String) getValue(SOURCES_CSW_URL);
    }

    public List<SourceInfo> sourcesDiscoveredSources() {
        return getValue(SOURCES_DISCOVERED) == null ? null : (List<SourceInfo>) getValue(SOURCES_DISCOVERED);
    }

    public String sourceHostname() {
        return getValue(SOURCE_HOSTNAME) == null ? null : (String) getValue(SOURCE_HOSTNAME);
    }

    public int sourcePort() {
        return getValue(SOURCE_PORT) == null ? null : Integer.valueOf(((Double)getValue(SOURCE_PORT)).intValue());
    }

    public String sourceManualUrl() {
        return getValue(SOURCE_MANUAL_URL) == null ? null : (String) getValue(SOURCE_MANUAL_URL);
    }

    public String sourceManualUrlType() {
        return getValue(SOURCE_MANUAL_URL_TYPE) == null ? null : (String) getValue(SOURCE_MANUAL_URL_TYPE);
    }

    public String sourceName() {
        return getValue(SOURCE_NAME) == null ? null : (String) getValue(SOURCE_NAME);
    }

    public SourceConfiguration sourcesPassword(String sourcesPassword) {
        addValue(SOURCE_PASSWORD, sourcesPassword);
        return this;
    }

    public SourceConfiguration sourcesUsername(String sourcesUsername) {
        addValue(SOURCE_USERNAME, sourcesUsername);
        return this;
    }

    public SourceConfiguration sourcesCswUrl(String sourcesCswUrl) {
        addValue(SOURCES_CSW_URL, sourcesCswUrl);
        return this;
    }

    public SourceConfiguration sourcesDiscoveredSources(List<SourceInfo> sourcesDiscoveredSources) {
        addValue(SOURCES_DISCOVERED, sourcesDiscoveredSources);
        return this;
    }

    public SourceConfiguration sourceHostname(String sourceHostname) {
        addValue(SOURCE_HOSTNAME, sourceHostname);
        return this;
    }

    public SourceConfiguration sourcePort(int sourcePort) {
        addValue(SOURCE_PORT, sourcePort);
        return this;
    }

    public SourceConfiguration selectedSource(SourceInfo sourceInfo) {
        addValue(SELECTED_SOURCE, sourceInfo);
        return this;
    }

    public SourceConfiguration sourceManualUrl(String sourceManualUrl) {
        addValue(SOURCE_MANUAL_URL, sourceManualUrl);
        return this;
    }

    public SourceConfiguration sourceManualUrlType(String sourceManualUrlType) {
        addValue(SOURCE_MANUAL_URL, sourceManualUrlType);
        return this;
    }

    public SourceInfo selectedSource() {
        return getValue(SELECTED_SOURCE) == null ? null : getValue(SELECTED_SOURCE) instanceof LinkedTreeMap ?
                new SourceInfo((LinkedTreeMap<String, String>)getValue(SELECTED_SOURCE)) : (SourceInfo) getValue(SELECTED_SOURCE);
    }

    public SourceConfiguration sourceName(String sourceName) {
        addValue(SOURCE_NAME, sourceName);
        return this;
    }

    @Override
    public SourceConfiguration copy() {
        SourceConfiguration newConfig = new SourceConfiguration();
        for(Map.Entry<SourceConfiguration.SOURCE_CONFIG_KEYS, Object> entry : getValues().entrySet()) {
            newConfig.addValue(entry.getKey(), entry.getValue());
        }

        return newConfig;
    }

    public enum SOURCE_CONFIG_KEYS {
        SOURCE_PASSWORD, SOURCE_USERNAME, SOURCES_CSW_URL, SOURCES_DISCOVERED, SOURCE_HOSTNAME, SOURCE_PORT,
        SELECTED_SOURCE, SOURCE_MANUAL_URL, SOURCE_MANUAL_URL_TYPE, SOURCE_NAME
    }
}
