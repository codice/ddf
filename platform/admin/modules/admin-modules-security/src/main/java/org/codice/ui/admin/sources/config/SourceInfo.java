package org.codice.ui.admin.sources.config;

import com.google.gson.internal.LinkedTreeMap;

public class SourceInfo {

    private String sourceType;

    private String genericType;

    private String url;

    private SourceConfigurationHandler.CERT_STATUS certStatus;

    public SourceInfo(String sourceType, String genericType, String url, SourceConfigurationHandler.CERT_STATUS certStatus) {
        this.sourceType = sourceType;
        this.genericType = genericType;
        this.url = url;
        this.certStatus = certStatus;
    }

    public SourceInfo(String sourceType, String genericType, String url) {
        this.sourceType = sourceType;
        this.genericType = genericType;
        this.url = url;
    }

    public SourceInfo(LinkedTreeMap<String, String> map){
        sourceType = map.get("sourceType");
        genericType = map.get("genericType");
        url = map.get("url");
    }
    public String getSourceType() {
        return sourceType;
    }

    public String getGenericType() {
        return genericType;
    }

    public String getUrl() {
        return url;
    }

    public SourceConfigurationHandler.CERT_STATUS getCertStatus() { return certStatus; }
}
