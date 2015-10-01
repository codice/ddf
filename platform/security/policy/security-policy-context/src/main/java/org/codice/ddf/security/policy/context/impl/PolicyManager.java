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
package org.codice.ddf.security.policy.context.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.codice.ddf.security.policy.context.attributes.ContextAttributeMapping;
import org.codice.ddf.security.policy.context.attributes.DefaultContextAttributeMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ContextPolicyManager. This implementation starts with a default empty policy
 * at the "/" context and accepts new policies as a Map&lt;String, String&gt; orMap&lt;String, String[]&gt;
 */
public class PolicyManager implements ContextPolicyManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyManager.class);

    private static final String REALMS = "realms";

    private static final String AUTH_TYPES = "authenticationTypes";

    private static final String REQ_ATTRS = "requiredAttributes";

    private static final String WHITE_LIST = "whiteListContexts";

    public static final String DEFAULT_REALM = "DDF";

    public static final String DEFAULT_REALM_CONTEXT_VALUE = "karaf";

    private static final String DEFAULT_REALM_CONTEXTS = "/=" + DEFAULT_REALM_CONTEXT_VALUE;

    private Map<String, ContextPolicy> policyStore = new HashMap<>();

    private List<String> whiteListContexts = new ArrayList<>();

    private ContextPolicy defaultPolicy = new Policy("/", DEFAULT_REALM, new ArrayList<String>(),
            new ArrayList<ContextAttributeMapping>());

    private Map<String, Object> policyProperties = new HashMap<>();

    private Collection<ContextPolicy> currentPolicies;

    public PolicyManager() {
        policyStore.put("/", defaultPolicy);
        currentPolicies = Collections.unmodifiableCollection(new ArrayList<>(policyStore.values()));
    }

    @Override
    public ContextPolicy getContextPolicy(String path) {
        ContextPolicy entry = policyStore.get(path);
        if (entry != null) {
            return entry;
        } else if (whiteListContexts.contains(path)) {
            return null;
        } else {
            int idx = path.lastIndexOf("/");
            if (idx <= 0) {
                idx++;
            }
            String pathFragment = path.substring(0, idx);
            if (StringUtils.isNotEmpty(pathFragment)) {
                return getContextPolicy(pathFragment);
            } else {
                //this is just here for safety
                //if we get down to the point where we can never get an entry, return the default
                return policyStore.get("/");
            }
        }
    }

    @Override
    public Collection<ContextPolicy> getAllContextPolicies() {
        return currentPolicies;
    }

    @Override
    public synchronized void setContextPolicy(String path, ContextPolicy contextPolicy) {
        if (path == null) {
            throw new IllegalArgumentException("Context path cannot be null.");
        }
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Context path must start with /");
        }
        if (contextPolicy == null) {
            throw new IllegalArgumentException("Context policy cannot be null.");
        }
        
        LOGGER.debug("setContextPolicy called with path = {}", path);

        //gather all context realms, authorization types, & required attributes
        Map<String, String> contextsToRealms = new HashMap<String, String>();
        Map<String, List<ContextAttributeMapping>> contextsToAttrs = new HashMap<>();
        Map<String, List<String>> contextsToAuths = new HashMap<>();

        for (String contextPath : policyStore.keySet()) {
            contextsToRealms.put(contextPath, policyStore.get(contextPath).getRealm());
            contextsToAttrs.put(contextPath,
                    new ArrayList(((Policy) policyStore.get(contextPath)).getAllowedAttributes()));
            contextsToAuths.put(contextPath,
                    new ArrayList(policyStore.get(contextPath).getAuthenticationMethods()));
        }

        //add the new context policy
        String newContextRealm = contextPolicy.getRealm();
        Collection<ContextAttributeMapping> newContextAttrs = ((Policy) contextPolicy).getAllowedAttributes();
        Collection<String> newContextAuths = contextPolicy.getAuthenticationMethods();

        if (StringUtils.isNotEmpty(newContextRealm)) {
            contextsToRealms.put(path, newContextRealm);
        }
        if (newContextAttrs != null) {
            contextsToAttrs.put(path, new ArrayList(newContextAttrs));
        }
        if (newContextAuths != null) {
            contextsToAuths.put(path, new ArrayList(newContextAuths));
        }
        
        setPolicyStore(contextsToRealms, contextsToAuths, contextsToAttrs);
        currentPolicies = Collections.unmodifiableCollection(new ArrayList<>(policyStore.values()));
    }

    /**
     * Initializes the policy store. This method will be called every time the policy attributes
     * change. This will happen after the component has been initialized (see {@link #configure()}
     * and when an update is made to the {@code org.codice.ddf.security.policy.context.impl.PolicyManager}
     * configuration pid.
     * <br/>
     * See https://osgi.org/javadoc/r6/cmpn/org/osgi/service/cm/ManagedService.html for more
     * details on how and when this method may be called.
     *
     * @param properties map of properties to use to initialize the policy store.
     *                   Since there is no configuration file bound to these properties by default,
     *                   this map may be {@code null}.
     */
    public synchronized void setPolicies(Map<String, Object> properties) {
        if (properties == null) {
            LOGGER.debug("setPolicies() called with null properties map. "
                    + "Policy store should have already been initialized so ignoring.");
            LOGGER.debug("Policy Store already contains {} items", policyStore.size());
            return;
        }

        LOGGER.debug("setPolicies called: {}", properties);
        policyStore.clear();
        policyStore.put("/", defaultPolicy);

        Object realmsObj = properties.get(REALMS);
        String[] realmContexts = null;
        Object authTypesObj = properties.get(AUTH_TYPES);
        String[] authContexts = null;
        Object reqAttrsObj = properties.get(REQ_ATTRS);
        String[] attrContexts = null;
        Object whiteList = properties.get(WHITE_LIST);
        if (realmsObj == null) {
            realmsObj = DEFAULT_REALM_CONTEXTS;
        }
        if (realmsObj instanceof String[]) {
            realmContexts = (String[]) realmsObj;
        } else if (realmsObj instanceof String) {
            realmContexts = ((String) realmsObj).split(",");
        }

        if (authTypesObj != null && authTypesObj instanceof String[]) {
            authContexts = (String[]) authTypesObj;
        } else if (authTypesObj != null) {
            authContexts = ((String) authTypesObj).split(",");
        }

        if (whiteList != null && whiteList instanceof String[]) {
            setWhiteListContexts(Arrays.asList((String[]) whiteList));
        } else if (whiteList != null) {
            setWhiteListContexts((String) whiteList);
        }

        if (reqAttrsObj != null && reqAttrsObj instanceof String[]) {
            attrContexts = (String[]) reqAttrsObj;
        } else if (reqAttrsObj != null) {
            attrContexts = ((String) reqAttrsObj).split(",");
        }
        if (authTypesObj != null && reqAttrsObj != null) {

            Map<String, String> contextToRealm = new HashMap<>();
            Map<String, List<String>> contextToAuth = new HashMap<>();
            Map<String, List<ContextAttributeMapping>> contextToAttr = new HashMap<>();

            List<String> realmContextList = expandStrings(realmContexts);
            List<String> authContextList = expandStrings(authContexts);
            List<String> attrContextList = expandStrings(attrContexts);

            for (String realm : realmContextList) {
                String[] parts = realm.split("=");
                if (parts.length == 2) {
                    contextToRealm.put(parts[0], parts[1]);
                }
            }

            for (String auth : authContextList) {
                String[] parts = auth.split("=");
                if (parts.length == 2) {
                    String[] auths = parts[1].split("\\|");
                    if (auths.length > 0) {
                        contextToAuth.put(parts[0], Arrays.asList(auths));
                    }
                } else if (parts.length == 1) {
                    contextToAuth.put(parts[0], new ArrayList<String>());
                }
            }

            for (String attr : attrContextList) {
                String context = attr.substring(0, attr.indexOf("="));
                String value = attr.substring(attr.indexOf("=") + 1);
                if (StringUtils.isNotEmpty(context) && value != null) {
                    if (value.startsWith("{") && value.endsWith("}")) {
                        if (value.length() == 2) {
                            value = "";
                        } else {
                            value = value.substring(1, value.length() - 1);
                        }
                    }
                    String[] attributes = value.split(";");
                    List<ContextAttributeMapping> attrMaps = new ArrayList<>();
                    for (String attribute : attributes) {
                        String[] parts = attribute.split("=");
                        if (parts.length == 2) {
                            attrMaps.add(new DefaultContextAttributeMapping(context, parts[0], parts[1]));
                        }
                    }
                    contextToAttr.put(context, attrMaps);
                }
            }

            setPolicyStore(contextToRealm, contextToAuth, contextToAttr);
            currentPolicies = Collections.unmodifiableCollection(new ArrayList<>(policyStore.values()));
        }
        LOGGER.debug("Policy store initialized, now contains {} entries", policyStore.size());
    }

    private void setPolicyStore(Map<String, String> allContextsToRealms,
            Map<String, List<String>> allContextsToAuths,
            Map<String, List<ContextAttributeMapping>> allContextsToAttrs) {

        //add default context values if they do not exist
        if (allContextsToRealms.get("/") == null) {
            allContextsToRealms.put("/", DEFAULT_REALM_CONTEXT_VALUE);
        }

        if (allContextsToAttrs.get("/") == null) {
            allContextsToAttrs.put("/", new ArrayList<ContextAttributeMapping>());
        }

        if (allContextsToAuths.get("/") == null) {
            allContextsToAuths.put("/", new ArrayList<String>());
        }

        //gather all given context paths
        Set<String> allContextPaths = new HashSet<>();
        allContextPaths.addAll(allContextsToRealms.keySet());
        allContextPaths.addAll(allContextsToAuths.keySet());
        allContextPaths.addAll(allContextsToAttrs.keySet());

        //resolve all realms, authorization types & required attributes
        for (String path : allContextPaths) {
            String contextRealm = getContextRealm(path, allContextsToRealms);
            List<String> contextAuthTypes = getContextAuthTypes(path, allContextsToAuths);
            List<ContextAttributeMapping> contextReqAttrs = getContextReqAttrs(path,
                    allContextsToAttrs);
            policyStore
                    .put(path, new Policy(path, contextRealm, contextAuthTypes, contextReqAttrs));
        }
    }

    /**
     * Gets the context realm of the given path. If context realm of given path does not exist it rolls back until a parent path exists with a context realm
     * If no parent path exists, the context defaults to "/" realm
     *
     * @param path           - Path associated to context
     * @param contextToRealm - Map of all paths and their context realms
     * @return context - Realm associated to path
     */
    public String getContextRealm(String path, Map<String, String> contextToRealm) {
        String entry = contextToRealm.get(path);
        if (entry != null) {
            return entry;
        } else {

            //continue splitting by last "/"  down path until value found
            int idx = path.lastIndexOf("/");
            if (idx <= 0) {
                idx++;
            }
            String pathFragment = path.substring(0, idx);
            if (StringUtils.isNotEmpty(pathFragment)) {
                return getContextRealm(pathFragment, contextToRealm);
            } else {
                return DEFAULT_REALM_CONTEXT_VALUE;
            }
        }
    }

    /**
     * Gets the authorization types of the given path. If authorization types of given path do not exist it rolls back until a parent path exists with such authorization types is found
     * If no parent path exists, the authorization types defaults to an empty list
     *
     * @param path               - Path associated with context
     * @param contextToAuthTypes - Map of all paths and their context authorization types
     * @return List of authorization types
     */
    public List<String> getContextAuthTypes(String path,
            Map<String, List<String>> contextToAuthTypes) {
        List<String> entry = contextToAuthTypes.get(path);
        if (entry != null) {
            return entry;
        } else {

            //continue splitting by last "/"  down path until value found, if last "/" is hit return default authentication type
            int idx = path.lastIndexOf("/");
            if (idx <= 0) {
                idx++;
            }
            String pathFragment = path.substring(0, idx);
            if (StringUtils.isNotEmpty(pathFragment)) {
                return getContextAuthTypes(pathFragment, contextToAuthTypes);
            } else {
                return new ArrayList<String>();
            }
        }
    }

    /**
     * Gets the required attributes of the context associated to the given path. If required attributes of given path do not exist it rolls back until a parent context path exists with such required attributes is found
     * If no parent path exists, the context defaults to empty list
     *
     * @param -                 Path associated to context
     * @param contextToReqAttrs - Map of all paths to contexts and their associated required attributes
     * @return List of required attributes
     */
    public List<ContextAttributeMapping> getContextReqAttrs(String path,
            Map<String, List<ContextAttributeMapping>> contextToReqAttrs) {
        List<ContextAttributeMapping> entry = contextToReqAttrs.get(path);
        if (entry != null) {
            return entry;
        } else {

            //Continue splitting by last "/"  down path until value found,
            int idx = path.lastIndexOf("/");
            if (idx <= 0) {
                idx++;
            }
            String pathFragment = path.substring(0, idx);
            if (StringUtils.isNotEmpty(pathFragment)) {
                return getContextReqAttrs(pathFragment, contextToReqAttrs);
            } else {
                return new ArrayList<ContextAttributeMapping>();
            }
        }
    }

    private List<String> expandStrings(String[] itemArr) {
        return expandStrings(Arrays.asList(itemArr));
    }

    private List<String> expandStrings(List<String> itemArr) {
        List<String> itemList = new ArrayList<>();
        for (String item : itemArr) {
            if (item.contains(",")) {
                String[] items = item.split(",");
                itemList.addAll(Arrays.asList(items));
            } else {
                itemList.add(item);
            }
        }
        return itemList;
    }

    public void setAuthenticationTypes(List<String> authenticationTypes) {
        LOGGER.debug("setAuthenticationTypes(List<String>) called with {}", authenticationTypes);
        if (authenticationTypes != null) {
            policyProperties.put(AUTH_TYPES,
                    authenticationTypes.toArray(new String[authenticationTypes.size()]));
        } else {
            policyProperties.put(AUTH_TYPES, null);
        }
    }

    public void setAuthenticationTypes(String authenticationTypes) {
        LOGGER.debug("setAuthenticationTypes(String) called with {}", authenticationTypes);
        policyProperties.put(AUTH_TYPES, authenticationTypes);
    }

    public void setRequiredAttributes(List<String> requiredAttributes) {
        LOGGER.debug("setRequiredAttributes(List<String>) called with {}", requiredAttributes);
        if (requiredAttributes != null) {
            policyProperties.put(REQ_ATTRS,
                    requiredAttributes.toArray(new String[requiredAttributes.size()]));
        } else {
            policyProperties.put(REQ_ATTRS, null);
        }
    }

    public void setRequiredAttributes(String requiredAttributes) {
        LOGGER.debug("setRequiredAttributes(String) called with {}", requiredAttributes);
        policyProperties.put(REQ_ATTRS, requiredAttributes);
    }

    public void setRealms(List<String> realms) {
        LOGGER.debug("setRealms(List<String>) called with {}", realms);
        if (realms != null) {
            policyProperties.put(REALMS, realms.toArray(new String[realms.size()]));
        } else {
            policyProperties.put(REALMS, null);
        }
    }

    public void setRealms(String realms) {
        LOGGER.debug("setRealms(String) called with {}", realms);
        policyProperties.put(REALMS, realms);
    }

    public void setWhiteListContexts(List<String> contexts) {
        LOGGER.debug("setWhiteListContexts(List<String>) called with {}", contexts);
        if (contexts != null && !contexts.isEmpty()) {
            whiteListContexts.clear();
            whiteListContexts.addAll(expandStrings(contexts));
        }
    }

    public void setWhiteListContexts(String contexts) {
        LOGGER.debug("setWhiteListContexts(String) called with {}", contexts);
        if (StringUtils.isNotEmpty(contexts)) {
            String[] contextsArr = contexts.split(",");
            whiteListContexts.clear();
            whiteListContexts.addAll(Arrays.asList(contextsArr));
        }
    }

    public boolean isWhiteListed(String contextPath) {
        return (getContextPolicy(contextPath) == null);
    }

    /**
     * Called by blueprint once all properties have been initialized. This isn't called by
     * configuration manager - it calls the specified update-method (in this case setPolicies).
     */
    public void configure() {
        LOGGER.debug("configure called.");
        setPolicies(policyProperties);
    }
}
