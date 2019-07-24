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
package org.codice.ddf.security.policy.context.impl;

import ddf.security.common.audit.SecurityLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.codice.ddf.security.policy.context.attributes.ContextAttributeMapping;
import org.codice.ddf.security.policy.context.attributes.DefaultContextAttributeMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ContextPolicyManager. This implementation starts with a default empty policy at
 * the "/" context and accepts new policies as a Map&lt;String, String&gt; orMap&lt;String,
 * String[]&gt;
 */
public class PolicyManager implements ContextPolicyManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyManager.class);

  private static final String AUTH_TYPES = "authenticationTypes";

  private static final String REQ_ATTRS = "requiredAttributes";

  private static final String WHITE_LIST = "whiteListContexts";

  private static final String GUEST_ACCESS = "guestAccess";

  private static final String SESSION_ACCESS = "sessionAccess";

  private static final int MAX_TRAVERSAL_DEPTH = 500;

  private Map<String, ContextPolicy> policyStore = new HashMap<>();

  private List<String> whiteListContexts = new ArrayList<>();

  private ContextPolicy defaultPolicy = new Policy("/", new ArrayList<>(), new ArrayList<>());

  private Map<String, Object> policyProperties = new HashMap<>();

  private int traversalDepth;

  private boolean guestAccess;

  private boolean sessionAccess;

  public PolicyManager() {
    policyStore.put("/", defaultPolicy);
  }

  @Override
  public ContextPolicy getContextPolicy(String path) {
    return getContextPolicy(path, getPolicyStore(), getWhiteListContexts(), 0);
  }

  private ContextPolicy getContextPolicy(
      String path,
      Map<String, ContextPolicy> policyStore,
      List<String> whiteListContexts,
      int depth) {
    ContextPolicy entry;
    entry = policyStore.get(path);

    if (entry != null) {
      return entry;
    } else if (whiteListContexts.contains(path)) {
      return null;
    } else {
      String pathFragment = rollbackPath(path);
      if (StringUtils.isNotEmpty(pathFragment) && depth <= traversalDepth) {
        return getContextPolicy(pathFragment, policyStore, whiteListContexts, ++depth);
      } else {
        // this is just here for safety
        // if we get down to the point where we can never get an entry, return the default
        return policyStore.get("/");
      }
    }
  }

  @Override
  public Collection<ContextPolicy> getAllContextPolicies() {
    return getPolicyStore().values();
  }

  @Override
  public void setContextPolicy(String path, ContextPolicy newContextPolicy) {
    if (path == null) {
      throw new IllegalArgumentException("Context path cannot be null.");
    }
    if (!path.startsWith("/")) {
      throw new IllegalArgumentException("Context path must start with /");
    }
    if (newContextPolicy == null) {
      throw new IllegalArgumentException("Context policy cannot be null.");
    }

    LOGGER.debug("setContextPolicy called with path = {}", path);

    // gather all authorization types & required attributes
    Map<String, List<ContextAttributeMapping>> contextsToAttrs = new HashMap<>();
    Map<String, List<String>> contextsToAuths = new HashMap<>();

    for (ContextPolicy contextPolicy : getPolicyStore().values()) {
      contextsToAttrs.put(
          contextPolicy.getContextPath(), new ArrayList<>(contextPolicy.getAllowedAttributes()));
      contextsToAuths.put(
          contextPolicy.getContextPath(),
          new ArrayList<>(contextPolicy.getAuthenticationMethods()));
    }

    // duplicate and add the new context policy
    List<ContextAttributeMapping> newContextAttrs =
        newContextPolicy
            .getAllowedAttributes()
            .stream()
            .map(
                contextAttribute ->
                    new DefaultContextAttributeMapping(
                        contextAttribute.getContext(),
                        contextAttribute.getAttributeName(),
                        contextAttribute.getAttributeValue()))
            .collect(Collectors.toList());

    Collection<String> newContextAuths = new ArrayList<>();
    newContextAuths.addAll(newContextPolicy.getAuthenticationMethods());

    if (newContextAttrs != null) {
      contextsToAttrs.put(path, new ArrayList<>(newContextAttrs));
    }
    contextsToAuths.put(path, new ArrayList<>(newContextAuths));

    setPolicyStore(contextsToAuths, contextsToAttrs);
  }

  /**
   * Initializes the policy store. This method will be called every time the policy attributes
   * change. This will happen after the component has been initialized (see {@link #configure()} and
   * when an update is made to the {@code org.codice.ddf.security.policy.context.impl.PolicyManager}
   * configuration pid. <br>
   * See https://osgi.org/javadoc/r6/cmpn/org/osgi/service/cm/ManagedService.html for more details
   * on how and when this method may be called.
   *
   * @param properties map of properties to use to initialize the policy store. Since there is no
   *     configuration file bound to these properties by default, this map may be {@code null}.
   */
  public void setPolicies(Map<String, Object> properties) {
    if (properties == null) {
      LOGGER.debug(
          "setPolicies() called with null properties map. "
              + "Policy store should have already been initialized so ignoring.");
      LOGGER.debug("Policy Store already contains {} items", policyStore.size());
      return;
    }

    LOGGER.debug("setPolicies called: {}", properties);
    Map<String, ContextPolicy> originalPolicyStore = getPolicyStore();

    setGuestAccess((boolean) properties.get(GUEST_ACCESS));
    setSessionAccess((boolean) properties.get(SESSION_ACCESS));

    String[] authContexts = (String[]) properties.get(AUTH_TYPES);
    String[] attrContexts = (String[]) properties.get(REQ_ATTRS);
    String[] whiteList = (String[]) properties.get(WHITE_LIST);

    if (whiteList != null) {
      setWhiteListContexts(Arrays.asList(whiteList));
    }

    if (authContexts != null && attrContexts != null) {

      Map<String, List<String>> contextToAuth = new HashMap<>();
      Map<String, List<ContextAttributeMapping>> contextToAttr = new HashMap<>();

      List<String> authContextList = new ArrayList<>();
      Collections.addAll(authContextList, authContexts);

      List<String> attrContextList = new ArrayList<>();
      Collections.addAll(attrContextList, attrContexts);

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
        int index = attr.indexOf('=');
        if (index < 1) {
          throw new IllegalArgumentException("Invalid attribute context: " + attr);
        }
        String context = attr.substring(0, index);
        String value = attr.substring(index + 1);
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

      setPolicyStore(contextToAuth, contextToAttr);
    }
    LOGGER.debug("Policy store initialized, now contains {} entries", policyStore.size());

    SecurityLogger.audit(
        "Policy store changed from:\n{} \nto:\n{}", originalPolicyStore, getPolicyStore());
  }

  private void setPolicyStore(
      Map<String, List<String>> allContextsToAuths,
      Map<String, List<ContextAttributeMapping>> allContextsToAttrs) {

    // add default context values if they do not exist
    if (allContextsToAttrs.get("/") == null) {
      allContextsToAttrs.put("/", new ArrayList<>());
    }

    if (allContextsToAuths.get("/") == null) {
      allContextsToAuths.put("/", new ArrayList<>());
    }

    // gather all given context paths
    Set<String> allContextPaths = new HashSet<>();
    allContextPaths.addAll(allContextsToAuths.keySet());
    allContextPaths.addAll(allContextsToAttrs.keySet());

    Map<String, ContextPolicy> newPolicyStore = new HashMap<>();
    newPolicyStore.put("/", defaultPolicy);

    // resolve all authorization types & required attributes
    for (String path : allContextPaths) {
      List<String> contextAuthTypes = getContextAuthTypes(path, allContextsToAuths);
      List<ContextAttributeMapping> contextReqAttrs = getContextReqAttrs(path, allContextsToAttrs);

      newPolicyStore.put(path, new Policy(path, contextAuthTypes, contextReqAttrs));
    }

    policyStore = newPolicyStore;
  }

  /**
   * Returns a duplicate of the current policy store.
   *
   * @return duplicate policy store
   */
  public Map<String, ContextPolicy> getPolicyStore() {
    Map<String, ContextPolicy> copiedPolicyStore = new HashMap<>();

    for (ContextPolicy contextPolicy : policyStore.values()) {
      copiedPolicyStore.put(contextPolicy.getContextPath(), copyContextPolicy(contextPolicy));
    }

    return copiedPolicyStore;
  }

  /**
   * Returns a duplicate of the current white list contexts
   *
   * @return
   */
  public List<String> getWhiteListContexts() {
    List<String> copiedWhiteListContexts = new ArrayList<>();
    copiedWhiteListContexts.addAll(whiteListContexts);
    return copiedWhiteListContexts;
  }

  public void setWhiteListContexts(List<String> contexts) {
    LOGGER.debug("setWhiteListContexts(List<String>) called with {}", contexts);
    if (contexts != null && !contexts.isEmpty()) {
      this.whiteListContexts = PropertyResolver.resolveProperties(contexts);
    }
  }

  /**
   * Duplicates the given context policy
   *
   * @param contextPolicy
   * @return copy of contextPolicy
   */
  public ContextPolicy copyContextPolicy(ContextPolicy contextPolicy) {
    Collection<ContextAttributeMapping> copiedContextAttributes = new ArrayList<>();
    Collection<String> copiedAuthenticationMethods = new ArrayList<>();

    copiedAuthenticationMethods.addAll(contextPolicy.getAuthenticationMethods());

    copiedContextAttributes.addAll(
        contextPolicy
            .getAllowedAttributes()
            .stream()
            .map(
                contextAttribute ->
                    new DefaultContextAttributeMapping(
                        contextAttribute.getContext(),
                        contextAttribute.getAttributeName(),
                        contextAttribute.getAttributeValue()))
            .collect(Collectors.toList()));

    return new Policy(
        contextPolicy.getContextPath(), copiedAuthenticationMethods, copiedContextAttributes);
  }

  /**
   * Gets the authorization types of the given path. If authorization types of given path do not
   * exist it rolls back until a parent path exists with such authorization types is found If no
   * parent path exists, the authorization types defaults to an empty list
   *
   * @param path - Path associated with context
   * @param contextToAuthTypes - Map of all paths and their context authorization types
   * @return List of authorization types
   */
  public List<String> getContextAuthTypes(
      String path, Map<String, List<String>> contextToAuthTypes) {
    List<String> entry = contextToAuthTypes.get(path);
    if (entry != null) {
      return entry;
    } else {
      String pathFragment = rollbackPath(path);
      if (StringUtils.isNotEmpty(pathFragment)) {
        return getContextAuthTypes(pathFragment, contextToAuthTypes);
      } else {
        return new ArrayList<>();
      }
    }
  }

  /**
   * Gets the required attributes of the context associated to the given path. If required
   * attributes of given path do not exist it rolls back until a parent context path exists with
   * such required attributes is found If no parent path exists, the context defaults to empty list
   *
   * @param - Path associated to context
   * @param contextToReqAttrs - Map of all paths to contexts and their associated required
   *     attributes
   * @return List of required attributes
   */
  public List<ContextAttributeMapping> getContextReqAttrs(
      String path, Map<String, List<ContextAttributeMapping>> contextToReqAttrs) {
    List<ContextAttributeMapping> entry = contextToReqAttrs.get(path);
    if (entry != null) {
      return entry;
    } else {
      String pathFragment = rollbackPath(path);
      if (StringUtils.isNotEmpty(pathFragment)) {
        return getContextReqAttrs(pathFragment, contextToReqAttrs);
      } else {
        return new ArrayList<>();
      }
    }
  }

  public String rollbackPath(String path) {
    // Continue splitting by last "/"  down path until value found,
    if (path.endsWith("/")) {
      while (path.endsWith("/") && path.length() > 1) {
        path = path.substring(0, path.length() - 1);
      }
      return path;
    } else {
      int idx = path.lastIndexOf('/');
      if (idx <= 0) {
        idx++;
      }
      return path.substring(0, idx);
    }
  }

  public void setAuthenticationTypes(List<String> authenticationTypes) {
    LOGGER.debug("setAuthenticationTypes(List<String>) called with {}", authenticationTypes);
    if (authenticationTypes != null) {
      policyProperties.put(
          AUTH_TYPES, authenticationTypes.toArray(new String[authenticationTypes.size()]));
    } else {
      policyProperties.put(AUTH_TYPES, null);
    }
  }

  public void setRequiredAttributes(List<String> requiredAttributes) {
    LOGGER.debug("setRequiredAttributes(List<String>) called with {}", requiredAttributes);
    if (requiredAttributes != null) {
      policyProperties.put(
          REQ_ATTRS, requiredAttributes.toArray(new String[requiredAttributes.size()]));
    } else {
      policyProperties.put(REQ_ATTRS, null);
    }
  }

  public boolean isWhiteListed(String contextPath) {
    return (getContextPolicy(contextPath) == null);
  }

  public void setTraversalDepth(int traversalDepth) {
    this.traversalDepth = traversalDepth;
    if (this.traversalDepth > MAX_TRAVERSAL_DEPTH) {
      this.traversalDepth = MAX_TRAVERSAL_DEPTH;
    }
  }

  public void setGuestAccess(boolean guestAccess) {
    policyProperties.put(GUEST_ACCESS, guestAccess);
    this.guestAccess = guestAccess;
  }

  @Override
  public boolean getGuestAccess() {
    return guestAccess;
  }

  public void setSessionAccess(boolean sessionAccess) {
    policyProperties.put(SESSION_ACCESS, sessionAccess);
    this.sessionAccess = sessionAccess;
  }

  @Override
  public boolean getSessionAccess() {
    return sessionAccess;
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
