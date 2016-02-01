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
package ddf.catalog.registry.policy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.subject.ExecutionException;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;
import ddf.catalog.registry.common.filter.RegistryQueryDelegate;
import ddf.catalog.registry.common.metacard.RegistryObjectMetacardType;
import ddf.catalog.util.impl.Requests;
import ddf.security.Subject;
import ddf.security.common.util.Security;

public class RegistryPolicyPlugin implements PolicyPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryPolicyPlugin.class);

    private boolean whiteList = false;

    private boolean registryDisabled = false;

    private Set<String> registryEntryIds;

    private List<String> registryBypassPolicyStrings;

    private List<String> writeAccessPolicyStrings;

    private List<String> readAccessPolicyStrings;

    private HashMap<String, Set<String>> bypassAccessPolicy;

    private HashMap<String, Set<String>> writeAccessPolicy;

    private HashMap<String, Set<String>> readAccessPolicy;

    private FilterBuilder filterBuilder;

    private CatalogFramework framework;

    public RegistryPolicyPlugin(CatalogFramework framework, FilterBuilder filterBuilder) {
        this.framework = framework;
        this.filterBuilder = filterBuilder;
        registryEntryIds = new HashSet<>();
        bypassAccessPolicy = new HashMap<>();
        writeAccessPolicy = new HashMap<>();
        readAccessPolicy = new HashMap<>();
    }

    protected Subject getSubject() {
        return Security.getSystemSubject();
    }

    protected boolean catalogRegistryEntriesIncluded(String attributeName,
            List<Serializable> attributeValues) {
        Filter registryFilter = filterBuilder.attribute(Metacard.CONTENT_TYPE)
                .is()
                .like()
                .text(RegistryObjectMetacardType.REGISTRY_METACARD_TYPE_NAME
                        + RegistryQueryDelegate.WILDCARD_CHAR);
        List<Filter> filters = new ArrayList<>(attributeValues.size());
        for (Serializable value : attributeValues) {
            filters.add(filterBuilder.attribute(attributeName)
                    .is()
                    .like()
                    .text(value.toString()));
        }

        try {
            Boolean result = getSubject().execute(() -> {
                QueryRequest request = new QueryRequestImpl(new QueryImpl(filterBuilder.allOf(
                        registryFilter,
                        filterBuilder.anyOf(filters))));

                QueryResponse response = framework.query(request);
                return response.getResults()
                        .size() != 0;

            });
            return result != null && result;
        } catch (ExecutionException e) {
            LOGGER.error("Exception when querying catalog for registry entries.", e);
        }
        return false;
    }

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

    @Override
    public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {
        HashMap<String, Set<String>> operationPolicy = new HashMap<>();
        if (Requests.isLocal(properties) && input.getContentTypeName() != null
                && input.getContentTypeName()
                .startsWith(RegistryObjectMetacardType.REGISTRY_METACARD_TYPE_NAME)) {
            if (isRegistryDisabled()) {
                operationPolicy.putAll(bypassAccessPolicy);
            } else {
                operationPolicy.putAll(writeAccessPolicy);
            }
        }
        return new PolicyResponseImpl(operationPolicy, new HashMap<>());
    }

    @Override
    public PolicyResponse processPreUpdate(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {
        HashMap<String, Set<String>> operationPolicy = new HashMap<>();
        if (Requests.isLocal(properties) && input.getContentTypeName() != null
                && input.getContentTypeName()
                .startsWith(RegistryObjectMetacardType.REGISTRY_METACARD_TYPE_NAME)) {
            if (isRegistryDisabled()) {
                operationPolicy.putAll(bypassAccessPolicy);
            } else {
                operationPolicy.putAll(writeAccessPolicy);
            }
        }
        return new PolicyResponseImpl(operationPolicy, new HashMap<>());
    }

    @Override
    public PolicyResponse processPreDelete(String attributeName, List<Serializable> attributeValues,
            Map<String, Serializable> properties) throws StopProcessingException {
        HashMap<String, Set<String>> operationPolicy = new HashMap<>();
        if (Requests.isLocal(properties) && catalogRegistryEntriesIncluded(attributeName,
                attributeValues)) {
            if (isRegistryDisabled()) {
                operationPolicy.putAll(bypassAccessPolicy);
            } else {
                operationPolicy.putAll(writeAccessPolicy);
            }
        }
        return new PolicyResponseImpl(operationPolicy, new HashMap<>());
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
        if (metacard.getContentTypeName() != null && metacard.getContentTypeName()
                .startsWith(RegistryObjectMetacardType.REGISTRY_METACARD_TYPE_NAME)) {
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

    public HashMap<String, Set<String>> getBypassAccessPolicy() {
        return bypassAccessPolicy;
    }

    public HashMap<String, Set<String>> getWriteAccessPolicy() {
        return writeAccessPolicy;
    }

    public HashMap<String, Set<String>> getReadAccessPolicy() {
        return readAccessPolicy;
    }
}
