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
package ddf.security.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.CollectionUtils;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.assertion.SecurityAssertion;
import ddf.security.expansion.Expansion;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;

/**
 * Abstraction class used to perform authorization for a realm. This class contains generic methods
 * that can be used to parse out the credentials from an incoming security token. It also handles
 * caching tokens for later use.
 */
public abstract class AbstractAuthorizingRealm extends AuthorizingRealm {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAuthorizingRealm.class);

    private static final String SAML_ROLE = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

    protected Map<ServiceReference, Expansion> expansionServices = new ConcurrentHashMap<>();

    public void addExpansion(ServiceReference<Expansion> expansionServiceRef) {
        Bundle bundle = FrameworkUtil.getBundle(AbstractAuthorizingRealm.class);
        if (bundle != null) {
            Expansion expansion = bundle.getBundleContext()
                    .getService(expansionServiceRef);
            addExpansion(expansionServiceRef, expansion);
        }
    }

    public void addExpansion(ServiceReference<Expansion> expansionServiceRef, Expansion expansion) {
        if (expansionServiceRef != null) {
            expansionServices.put(expansionServiceRef, expansion);
        }
    }

    public void removeExpansion(ServiceReference<Expansion> expansionServiceRef) {
        expansionServices.remove(expansionServiceRef);
    }

    public AbstractAuthorizingRealm() {
        setAuthorizationCachingEnabled(false);
    }

    /**
     * Takes the security attributes about the subject of the incoming security token and builds
     * sets of permissions and roles for use in further checking.
     *
     * @param principalCollection holds the security assertions for the primary principal of this request
     * @return a new collection of permissions and roles corresponding to the security assertions
     * @throws AuthorizationException if there are no security assertions associated with this principal collection or
     *                                if the token cannot be processed successfully.
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        LOGGER.debug("Retrieving authorization info for {}",
                principalCollection.getPrimaryPrincipal());
        SecurityAssertion assertion = principalCollection.oneByType(SecurityAssertion.class);
        if (assertion == null) {
            String msg = "No assertion found, cannot retrieve authorization info.";
            LOGGER.warn(msg);
            throw new AuthorizationException(msg);
        }
        List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
        Set<Permission> permissions = new HashSet<>();
        Set<String> roles = new HashSet<>();

        Map<String, Set<String>> permissionsMap = new HashMap<>();
        Collection<Expansion> expansionServices = getExpansionServices();
        for (AttributeStatement curStatement : attributeStatements) {
            addAttributesToMap(curStatement.getAttributes(), permissionsMap,
                    expansionServices);
        }

        for (Map.Entry<String, Set<String>> entry : permissionsMap.entrySet()) {
            permissions.add(new KeyValuePermission(entry.getKey(), entry.getValue()));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Adding permission: {} : {}", entry.getKey(),
                        StringUtils.join(entry.getValue(), ","));
            }
        }

        if (permissionsMap.containsKey(SAML_ROLE)) {
            roles.addAll(permissionsMap.get(SAML_ROLE));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Adding roles to authorization info: {}",
                        StringUtils.join(roles, ","));
            }
        }

        info.setObjectPermissions(permissions);
        info.setRoles(roles);

        return info;
    }

    private void addAttributesToMap(List<Attribute> attributes,
            Map<String, Set<String>> permissionsMap, Collection<Expansion> expansions) {
        Set<String> attributeSet;
        for (Attribute curAttribute : attributes) {
            attributeSet = expandAttributes(curAttribute, expansions);
            if (attributeSet != null) {
                if (permissionsMap.containsKey(curAttribute.getName())) {
                    permissionsMap.get(curAttribute.getName())
                            .addAll(attributeSet);
                } else {
                    permissionsMap.put(curAttribute.getName(), new HashSet<>(attributeSet));
                }
            }
        }
    }

    /**
     * Takes an {@link org.opensaml.saml.saml2.core.Attribute} and utilizes the
     * {@link ddf.security.expansion.Expansion} service to potentially expand it to a
     * different/enhanced set of attributes. This expansion is controlled by the configuration of
     * the expansion service but relies on the name of this attribute as a key. The returned set of
     * Strings represent the possibly expanded set of attributes to be added to the current
     * permissions.
     *
     * @param attribute current attribute whose values are to be potentially expanded
     * @return a set of potentially expanded values
     */
    private Set<String> expandAttributes(Attribute attribute, Collection<Expansion> expansions) {
        Set<String> attributeSet = new HashSet<>();
        String attributeName = attribute.getName();
        for (XMLObject curValue : attribute.getAttributeValues()) {
            if (curValue instanceof XSString) {
                attributeSet.add(((XSString) curValue).getValue());
            } else {
                LOGGER.info(
                        "Unexpected attribute type (non-string) for attribute named {} - ignored",
                        attributeName);
            }
        }
        for (Expansion expansionService : expansions) {
            LOGGER.debug("Expanding attributes for {} - original values: {}", attributeName,
                    attributeSet);
            attributeSet = expansionService.expand(attributeName, attributeSet);
        }
        LOGGER.debug("Expanded attributes for {} - values: {}", attributeName, attributeSet);
        return attributeSet;
    }

    protected List<Permission> expandPermissions(List<Permission> permissions) {
        Collection<Expansion> expansionServices = getExpansionServices();
        if (CollectionUtils.isEmpty(expansionServices)) {
            return permissions;
        }
        List<Permission> expandedPermissions = new ArrayList<>(permissions.size());
        for (Permission permission : permissions) {
            if (permission instanceof KeyValuePermission) {
                for (Expansion expansionService : expansionServices) {
                    Set<String> expandedSet = expansionService.expand(
                            ((KeyValuePermission) permission).getKey(),
                            new HashSet<>(((KeyValuePermission) permission).getValues()));
                    expandedPermissions.add(
                            new KeyValuePermission(((KeyValuePermission) permission).getKey(),
                                    expandedSet));
                }
            } else if (permission instanceof KeyValueCollectionPermission) {
                List<Permission> keyValuePermissionList = ((KeyValueCollectionPermission) permission).getKeyValuePermissionList();
                List<Permission> expandedCollection = expandPermissions(keyValuePermissionList);
                //we know that everything in a key value collection is a key value permission so just do the unchecked cast
                List<KeyValuePermission> castedList = castToKeyValueList(expandedCollection);
                expandedPermissions.add(new KeyValueCollectionPermission(
                        ((KeyValueCollectionPermission) permission).getAction(), castedList));
            } else {
                expandedPermissions.add(permission);
            }
        }

        return expandedPermissions;
    }

    private Collection<Expansion> getExpansionServices() {
        return expansionServices.values();
    }

    private <T> List<T> castToKeyValueList(List<Permission> permissionList) {
        return (List<T>) permissionList;
    }

}
