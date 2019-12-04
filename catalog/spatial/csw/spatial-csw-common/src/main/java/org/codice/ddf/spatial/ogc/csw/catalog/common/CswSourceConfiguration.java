/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import ddf.security.encryption.EncryptionService;
import ddf.security.permission.Permissions;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Domain object to encapsulate the configuration of an instance of a {@link CswSource}. CSW
 * converters, readers, etc. will access this object to determine the latest configuration of the
 * {@link CswSource} they are working on.
 */
public class CswSourceConfiguration {

  private static final String DEFAULT_SSL_PROTOCOL = "TLSv1.2";

  private String cswUrl;

  private String id;

  private String authenticationType;

  private String username;

  private String password;

  private String oauthDiscoveryUrl;

  private String oauthClientId;

  private String oauthClientSecret;

  private String oauthFlow;

  private String certAlias;

  private String keystorePath;

  private String sslProtocol = DEFAULT_SSL_PROTOCOL;

  private boolean disableCnCheck = false;

  private Map<String, String> metacardCswMappings = new HashMap<>();

  private CswAxisOrder cswAxisOrder;

  private boolean usePosList;

  private Integer pollIntervalMinutes;

  private Integer connectionTimeout;

  private Integer receiveTimeout;

  private boolean isCqlForced;

  private String outputSchema;

  private String queryTypeName;

  private String queryTypeNamespace;

  private String eventServiceAddress;

  private boolean registerForEvents;

  private EncryptionService encryptionService;

  private Map<String, Set<String>> securityAttributes = new HashMap<>();

  public CswSourceConfiguration(EncryptionService encryptionService) {
    this.encryptionService = encryptionService;
  }

  @Deprecated
  public CswSourceConfiguration() {}

  public String getCswUrl() {
    return cswUrl;
  }

  public void setCswUrl(String cswUrl) {
    this.cswUrl = cswUrl;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAuthenticationType() {
    return authenticationType;
  }

  public void setAuthenticationType(String authenticationType) {
    this.authenticationType = authenticationType;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    String updatedPassword = password;
    if (encryptionService != null) {
      updatedPassword = encryptionService.decryptValue(password);
    }
    this.password = updatedPassword;
  }

  public String getOauthDiscoveryUrl() {
    return oauthDiscoveryUrl;
  }

  public void setOauthDiscoveryUrl(String oauthDiscoveryUrl) {
    this.oauthDiscoveryUrl = oauthDiscoveryUrl;
  }

  public String getOauthClientId() {
    return oauthClientId;
  }

  public void setOauthClientId(String oauthClientId) {
    this.oauthClientId = oauthClientId;
  }

  public String getOauthClientSecret() {
    return oauthClientSecret;
  }

  public void setOauthClientSecret(String oauthClientSecret) {
    this.oauthClientSecret = oauthClientSecret;
  }

  public String getOauthFlow() {
    return oauthFlow;
  }

  public void setOauthFlow(String oauthFlow) {
    this.oauthFlow = oauthFlow;
  }

  public void setMetacardCswMappings(Map<String, String> mapping) {
    this.metacardCswMappings.clear();
    this.metacardCswMappings.putAll(mapping);
  }

  public void putMetacardCswMapping(String key, String value) {
    this.metacardCswMappings.put(key, value);
  }

  public String getMetacardMapping(String key) {
    return metacardCswMappings.get(key);
  }

  public boolean getDisableCnCheck() {
    return disableCnCheck;
  }

  public void setDisableCnCheck(boolean disableCnCheck) {
    this.disableCnCheck = disableCnCheck;
  }

  public Map<String, String> getMetacardCswMappings() {
    Map<String, String> newMap = new HashMap<>();
    newMap.putAll(metacardCswMappings);
    return newMap;
  }

  public void setCswAxisOrder(CswAxisOrder cswAxisOrder) {
    this.cswAxisOrder = cswAxisOrder;
  }

  public CswAxisOrder getCswAxisOrder() {
    return this.cswAxisOrder;
  }

  public boolean isSetUsePosList() {
    return usePosList;
  }

  public void setUsePosList(boolean usePosList) {
    this.usePosList = usePosList;
  }

  public Integer getPollIntervalMinutes() {
    return pollIntervalMinutes;
  }

  public void setPollIntervalMinutes(Integer pollIntervalMinutes) {
    this.pollIntervalMinutes = pollIntervalMinutes;
  }

  public Integer getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(Integer connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public Integer getReceiveTimeout() {
    return receiveTimeout;
  }

  public void setReceiveTimeout(Integer receiveTimeout) {
    this.receiveTimeout = receiveTimeout;
  }

  public void setIsCqlForced(boolean isForceCql) {
    this.isCqlForced = isForceCql;
  }

  public boolean isCqlForced() {
    return this.isCqlForced;
  }

  public String getOutputSchema() {
    return outputSchema;
  }

  public void setOutputSchema(String outputSchema) {
    this.outputSchema = outputSchema;
  }

  public String getQueryTypeName() {
    return queryTypeName;
  }

  public void setQueryTypeName(String queryTypeName) {
    this.queryTypeName = queryTypeName;
  }

  public String getQueryTypeNamespace() {
    return queryTypeNamespace;
  }

  public void setQueryTypeNamespace(String queryTypeNamespace) {
    this.queryTypeNamespace = queryTypeNamespace;
  }

  public Map<String, Set<String>> getSecurityAttributes() {
    return Collections.unmodifiableMap(securityAttributes);
  }

  public void setSecurityAttributes(String[] securityAttributStrings) {
    if (securityAttributStrings != null) {
      securityAttributes.clear();
      securityAttributes.putAll(Permissions.parsePermissionsFromString(securityAttributStrings));
    }
  }

  public boolean isRegisterForEvents() {
    return registerForEvents;
  }

  public void setRegisterForEvents(Boolean registerForEvents) {
    this.registerForEvents = registerForEvents;
  }

  public String getEventServiceAddress() {
    return eventServiceAddress;
  }

  public void setEventServiceAddress(String eventServiceAddress) {
    this.eventServiceAddress = eventServiceAddress;
  }

  public String getCertAlias() {
    return certAlias;
  }

  public void setCertAlias(String certAlias) {
    this.certAlias = certAlias;
  }

  public String getKeystorePath() {
    return keystorePath;
  }

  public void setKeystorePath(String keystorePath) {
    this.keystorePath = keystorePath;
  }

  public String getSslProtocol() {
    return sslProtocol;
  }

  public void setSslProtocol(String sslProtocol) {
    this.sslProtocol = sslProtocol;
  }
}
