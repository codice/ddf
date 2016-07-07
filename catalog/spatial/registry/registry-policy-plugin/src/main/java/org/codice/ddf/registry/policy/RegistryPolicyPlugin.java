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
package org.codice.ddf.registry.policy;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;
import ddf.catalog.util.impl.Requests;

public class RegistryPolicyPlugin implements PolicyPlugin {

    private boolean whiteList = false;

    private boolean registryDisabled = false;

    private Set<String> registryEntryIds = new HashSet<>();

    private List<String> registryBypassPolicyStrings;

    private List<String> writeAccessPolicyStrings;

    private List<String> readAccessPolicyStrings;

    private Map<String, Set<String>> bypassAccessPolicy = new HashMap<>();

    private Map<String, Set<String>> writeAccessPolicy = new HashMap<>();

    private Map<String, Set<String>> readAccessPolicy = new HashMap<>();

    /**
     * @return Returns true if the registry entry ids represents a set of 'white listed' entries. Default is false.
     */
    public boolean isWhiteList() {
        return whiteList;
    }

    /**
     * Sets the whether or not the registry entry ids list is a 'white list' or not.
     *
     * @param whiteList boolean value for whiteList
     */
    public void setWhiteList(boolean whiteList) {
        this.whiteList = whiteList;
    }

    /**
     * Get a string set of the federation service ids allowed to be queried
     *
     * @return A set of registry entry id strings
     */
    public Set<String> getRegistryEntryIds() {
        return registryEntryIds;
    }

    /**
     * Setter for the list of allowed federation service ids. All registry ids in this list will be
     * allowed to be queried and returned
     *
     * @param registryEntryIds Set of registry entry ids
     */
    public void setRegistryEntryIds(Set<String> registryEntryIds) {
        this.registryEntryIds = registryEntryIds;
    }

    public boolean isRegistryDisabled() {
        return registryDisabled;
    }

    public void setRegistryDisabled(boolean registryDisabled) {
        this.registryDisabled = registryDisabled;
    }

    /**
     * Getter for the permissions string array
     *
     * @return The list of bypass permissions
     */
    public List<String> getRegistryBypassPolicyStrings() {
        return this.registryBypassPolicyStrings;
    }

    public List<String> getWriteAccessPolicyStrings() {
        return writeAccessPolicyStrings;
    }

    public void setWriteAccessPolicyStrings(List<String> writeAccessPolicyStrings) {
        this.writeAccessPolicyStrings = writeAccessPolicyStrings;
        parsePermissionsFromString(writeAccessPolicyStrings, writeAccessPolicy);
    }

    public List<String> getReadAccessPolicyStrings() {
        return readAccessPolicyStrings;
    }

    public void setReadAccessPolicyStrings(List<String> readAccessPolicyStrings) {
        this.readAccessPolicyStrings = readAccessPolicyStrings;
        parsePermissionsFromString(readAccessPolicyStrings, readAccessPolicy);
    }

    /**
     * Setter used by the ui to set the permissions/attributes
     *
     * @param permStrings The list of bypass permissions
     */
    public void setRegistryBypassPolicyStrings(List<String> permStrings) {
        this.registryBypassPolicyStrings = permStrings;
        parsePermissionsFromString(permStrings, bypassAccessPolicy);
    }

    /**
     * Parses a string  array representation of permission attributes
     *
     * @param permStrings String array of permissions to parse
     * @param policy      The policy map to put the parsed permissions in
     */
    private void parsePermissionsFromString(List<String> permStrings,
            Map<String, Set<String>> policy) {
        policy.clear();
        if (permStrings != null) {
            for (String perm : permStrings) {
                String[] parts = perm.split("=");
                if (parts.length == 2) {
                    String attributeName = parts[0];
                    String attributeValue = parts[1];
                    policy.put(attributeName, Collections.singleton(attributeValue));
                }
            }
        }
    }

    private PolicyResponse getWritePolicy(Metacard input, Map<String, Serializable> properties) {
        HashMap<String, Set<String>> operationPolicy = new HashMap<>();
        if (Requests.isLocal(properties) && input != null && input.getTags()
                .contains(RegistryConstants.REGISTRY_TAG)) {
            Attribute attribute = input.getAttribute(RegistryObjectMetacardType.REGISTRY_BASE_URL);
            if (isRegistryDisabled() || (attribute != null && attribute.getValue() instanceof String
                    && ((String) attribute.getValue()).startsWith(SystemBaseUrl.getBaseUrl()))) {
                operationPolicy.putAll(bypassAccessPolicy);
            } else {
                operationPolicy.putAll(writeAccessPolicy);
            }
        }
        return new PolicyResponseImpl(operationPolicy, new HashMap<>());
    }

    @Override
    public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return getWritePolicy(input, properties);
    }

    @Override
    public PolicyResponse processPreUpdate(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return getWritePolicy(input, properties);
    }

    @Override
    public PolicyResponse processPreDelete(List<Metacard> metacards,
            Map<String, Serializable> properties) throws StopProcessingException {
        if (metacards != null) {
            for (Metacard metacard : metacards) {
                PolicyResponse response = getWritePolicy(metacard, properties);
                if (!response.operationPolicy()
                        .isEmpty()) {
                    return response;
                }
            }
        }
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostDelete(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPreQuery(Query query, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostQuery(Result input, Map<String, Serializable> properties)
            throws StopProcessingException {
        HashMap<String, Set<String>> itemPolicy = new HashMap<>();
        Metacard metacard = input.getMetacard();
        if (metacard.getTags()
                .contains(RegistryConstants.REGISTRY_TAG)) {
            if ((whiteList && !registryEntryIds.contains(metacard.getId())) || (!whiteList
                    && registryEntryIds.contains(metacard.getId()))) {
                itemPolicy.putAll(bypassAccessPolicy);
            } else {
                itemPolicy.putAll(readAccessPolicy);
            }
        }
        return new PolicyResponseImpl(new HashMap<>(), itemPolicy);
    }

    @Override
    public PolicyResponse processPreResource(ResourceRequest resourceRequest)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostResource(ResourceResponse resourceResponse, Metacard metacard)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    public Map<String, Set<String>> getBypassAccessPolicy() {
        return Collections.unmodifiableMap(bypassAccessPolicy);
    }

    public Map<String, Set<String>> getWriteAccessPolicy() {
        return Collections.unmodifiableMap(writeAccessPolicy);
    }

    public Map<String, Set<String>> getReadAccessPolicy() {
        return Collections.unmodifiableMap(readAccessPolicy);
    }
}
