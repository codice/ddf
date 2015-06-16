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

package ddf.security.pdp.xacml.realm;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.pdp.xacml.PdpException;
import ddf.security.pdp.xacml.XACMLConstants;
import ddf.security.pdp.xacml.processor.BalanaPdp;
import ddf.security.permission.ActionPermission;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.service.impl.AbstractAuthorizingRealm;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributesType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.DecisionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RequestType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ResponseType;

/**
 * Performs authorization backed by a XACML-based PDP.
 */
public class XACMLRealm extends AbstractAuthorizingRealm {

    private static Logger LOGGER = LoggerFactory.getLogger(XACMLRealm.class);

    private static final String AUTHZ_PERMITTED_EXCEPTION = " does not have permission to perform action.";

    private static final String AUTHZ_ROLE_EXCEPTION = " does not have the checked role(s).";

    private static Logger logger = LoggerFactory.getLogger(XACMLRealm.class);

    private BalanaPdp pdp;

    /**
     * Creates a general
     */
    public XACMLRealm(String dirPath) throws PdpException {
        super();
        pdp = new BalanaPdp(dirPath);
        LOGGER.debug("Creating new PDP-backed Authorizing Realm");
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        // This Realm is only for AuthZ, it does not support any AuthN tokens.
        return false;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
            throws AuthenticationException {
        return null;
    }

    @Override
    public boolean isPermitted(PrincipalCollection principals, String permission) {
        Permission p = getPermissionResolver().resolvePermission(permission);
        return isPermitted(principals, p);
    }

    @Override
    public boolean isPermitted(PrincipalCollection subjectPrincipal, Permission permission) {
        ArrayList<Permission> permissions = new ArrayList<Permission>(1);
        permissions.add(permission);
        return isPermitted(subjectPrincipal, permissions)[0];
    }

    @Override
    public boolean[] isPermitted(PrincipalCollection subjectPrincipal, String... permissions) {
        List<Permission> perms = new ArrayList<Permission>(permissions.length);
        for (String permString : permissions) {
            perms.add(getPermissionResolver().resolvePermission(permString));
        }
        return isPermitted(subjectPrincipal, perms);
    }

    @Override
    public boolean[] isPermitted(PrincipalCollection subjectPrincipal,
            List<Permission> permissions) {
        boolean[] results = new boolean[permissions.size()];
        String primaryPrincipal = "";
        Principal principal = subjectPrincipal.oneByType(SecurityAssertion.class).getPrincipal();
        if (null != principal) {
            primaryPrincipal = principal.getName();
        } else {
            primaryPrincipal = subjectPrincipal.getPrimaryPrincipal().toString();
        }
        AuthorizationInfo info = getAuthorizationInfo(subjectPrincipal);
        Permission curPermission;
        boolean curResponse;
        String curAction;
        for (int i = 0; i < permissions.size(); i++) {
            curPermission = permissions.get(i);
            if (curPermission instanceof ActionPermission) {
                curAction = ((ActionPermission) curPermission).getAction();
                LOGGER.debug("Checking if {} has access to perform a {} action", primaryPrincipal,
                        curAction);

                SecurityLogger.logInfo(
                        "Checking if [" + primaryPrincipal + "] has access to perform a ["
                                + curAction + "] action");

                LOGGER.debug("Received authZ info, creating XACML request.");
                RequestType curRequest = createActionXACMLRequest(primaryPrincipal, info,
                        curAction);
                LOGGER.debug("Created XACML request, calling PDP.");

                curResponse = isPermitted(curRequest);
                LOGGER.debug("Received response from PDP, returning {}.", curResponse);
                results[i] = curResponse;
            } else if (curPermission instanceof KeyValueCollectionPermission) {
                LOGGER.debug("Checking if {} has access to current metacard", primaryPrincipal);

                SecurityLogger.logInfo("Checking if [" + primaryPrincipal
                        + "] has access to view current metacard");

                LOGGER.debug("Received authZ info, creating XACML request.");
                RequestType curRequest = createRedactXACMLRequest(primaryPrincipal, info,
                        (KeyValueCollectionPermission) curPermission);
                LOGGER.debug("Created XACML request, calling PDP.");

                curResponse = isPermitted(curRequest);
                LOGGER.debug("Received response from PDP, returning {}.", curResponse);
                results[i] = curResponse;
            } else if (curPermission instanceof KeyValuePermission) {
                //Need to refactor this into a private method with the above condition
                //This is to handle the case where there is a single KeyValuePermission
                LOGGER.debug("Checking if {} has access to current metacard", primaryPrincipal);

                SecurityLogger.logInfo("Checking if [" + primaryPrincipal
                        + "] has access to view current metacard");
                KeyValueCollectionPermission keyValueCollectionPermission = new KeyValueCollectionPermission(
                        (KeyValuePermission) curPermission);
                LOGGER.debug("Received authZ info, creating XACML request.");
                RequestType curRequest = createRedactXACMLRequest(primaryPrincipal, info,
                        keyValueCollectionPermission);
                LOGGER.debug("Created XACML request, calling PDP.");

                curResponse = isPermitted(curRequest);
                LOGGER.debug("Received response from PDP, returning {}.", curResponse);
                results[i] = curResponse;
            } else {
                LOGGER.warn(
                        "Could not check permissions with {}, permission being requested MUST be an ActionPermission or RedactionPermission",
                        curPermission);
                results[i] = false;
            }
        }
        return results;
    }

    protected RequestType createActionXACMLRequest(String subject, AuthorizationInfo info,
            String action) {
        LOGGER.debug("Creating XACML request for subject: {} with action: {}", subject, action);

        RequestType xacmlRequestType = new RequestType();
        xacmlRequestType.setCombinedDecision(false);
        xacmlRequestType.setReturnPolicyIdList(false);

        AttributesType actionAttributes = new AttributesType();
        actionAttributes.setCategory(XACMLConstants.ACTION_CATEGORY);
        AttributeType actionAttribute = new AttributeType();
        actionAttribute.setAttributeId(XACMLConstants.ACTION_ID);
        actionAttribute.setIncludeInResult(false);
        AttributeValueType actionValue = new AttributeValueType();
        actionValue.setDataType(XACMLConstants.STRING_DATA_TYPE);
        LOGGER.trace("Adding action: {} for subject: {}", action, subject);
        actionValue.getContent().add(action);
        actionAttribute.getAttributeValue().add(actionValue);
        actionAttributes.getAttribute().add(actionAttribute);
        xacmlRequestType.getAttributes().add(actionAttributes);

        // Adding permissions for the calling subject
        AttributesType subjectAttributes = createSubjectAttributes(subject, info);
        xacmlRequestType.getAttributes().add(subjectAttributes);

        LOGGER.debug("Successfully created XACML request for subject: {} with action: {}", subject,
                action);

        return xacmlRequestType;
    }

    protected RequestType createRedactXACMLRequest(String subject, AuthorizationInfo info,
            CollectionPermission permission) {
        LOGGER.debug("Creating XACML request for subject: {} and metacard permissions {}", subject,
                permission);

        RequestType xacmlRequestType = new RequestType();
        xacmlRequestType.setCombinedDecision(false);
        xacmlRequestType.setReturnPolicyIdList(false);

        // Adding filter action
        AttributesType actionAttributes = new AttributesType();
        actionAttributes.setCategory(XACMLConstants.ACTION_CATEGORY);
        AttributeType actionAttribute = new AttributeType();
        actionAttribute.setAttributeId(XACMLConstants.ACTION_ID);
        actionAttribute.setIncludeInResult(false);
        AttributeValueType actionValue = new AttributeValueType();
        actionValue.setDataType(XACMLConstants.STRING_DATA_TYPE);
        LOGGER.trace("Adding action: {} for subject: {}", XACMLConstants.FILTER_ACTION, subject);
        actionValue.getContent().add(XACMLConstants.FILTER_ACTION);
        actionAttribute.getAttributeValue().add(actionValue);
        actionAttributes.getAttribute().add(actionAttribute);

        xacmlRequestType.getAttributes().add(actionAttributes);

        // Adding permissions for the calling subject
        AttributesType subjectAttributes = createSubjectAttributes(subject, info);
        xacmlRequestType.getAttributes().add(subjectAttributes);

        // Adding permissions for the resource
        AttributesType metadataAttributes = new AttributesType();
        metadataAttributes.setCategory(XACMLConstants.RESOURCE_CATEGORY);

        if (permission instanceof KeyValueCollectionPermission) {
            List<KeyValuePermission> tmpList = ((KeyValueCollectionPermission) permission)
                    .getKeyValuePermissionList();
            for (KeyValuePermission curPermission : tmpList) {
                for (String curPermValue : ((KeyValuePermission) curPermission).getValues()) {
                    AttributeType resourceAttribute = new AttributeType();
                    AttributeValueType resourceAttributeValue = new AttributeValueType();
                    resourceAttribute.setAttributeId(((KeyValuePermission) curPermission).getKey());
                    resourceAttribute.setIncludeInResult(false);
                    resourceAttributeValue.setDataType(XACMLConstants.STRING_DATA_TYPE);
                    LOGGER.trace("Adding permission: {}:{} for incoming resource",
                            new Object[] {((KeyValuePermission) curPermission).getKey(),
                                    curPermValue});
                    resourceAttributeValue.getContent().add(curPermValue);
                    resourceAttribute.getAttributeValue().add(resourceAttributeValue);
                    metadataAttributes.getAttribute().add(resourceAttribute);
                }

            }

            xacmlRequestType.getAttributes().add(metadataAttributes);
        } else {
            LOGGER.warn(
                    "Permission on the resource need to be of type KeyValueCollectionPermission, cannot process this resource.");
        }

        return xacmlRequestType;
    }

    protected boolean isPermitted(RequestType xacmlRequest) {
        boolean permitted = false;
        ResponseType xacmlResponse = null;

        try {
            LOGGER.debug("Calling PDP to evaluate XACML request.");
            xacmlResponse = pdp.evaluate(xacmlRequest);
            LOGGER.debug("Received response from PDP.");
            permitted = xacmlResponse != null
                    && xacmlResponse.getResult().get(0).getDecision() == DecisionType.PERMIT ?
                    true :
                    false;
            LOGGER.debug("Permitted: " + permitted);
        } catch (PdpException e) {
            LOGGER.error(e.getMessage(), e);
            permitted = false;
        }

        return permitted;
    }

    private AttributesType createSubjectAttributes(String subject, AuthorizationInfo info) {
        AttributesType subjectAttributes = new AttributesType();
        subjectAttributes.setCategory(XACMLConstants.ACCESS_SUBJECT_CATEGORY);
        AttributeType subjectAttribute = new AttributeType();
        subjectAttribute.setAttributeId(XACMLConstants.SUBJECT_ID);
        subjectAttribute.setIncludeInResult(false);
        AttributeValueType subjectValue = new AttributeValueType();
        subjectValue.setDataType(XACMLConstants.STRING_DATA_TYPE);
        LOGGER.debug("Adding subject: {}", subject);
        subjectValue.getContent().add(subject);
        subjectAttribute.getAttributeValue().add(subjectValue);
        subjectAttributes.getAttribute().add(subjectAttribute);

        for (String curRole : info.getRoles()) {
            AttributeType roleAttribute = new AttributeType();
            roleAttribute.setAttributeId(XACMLConstants.ROLE_CLAIM);
            roleAttribute.setIncludeInResult(false);
            AttributeValueType roleValue = new AttributeValueType();
            roleValue.setDataType(XACMLConstants.STRING_DATA_TYPE);
            LOGGER.trace("Adding role: {} for subject: {}", curRole, subject);
            roleValue.getContent().add(curRole);
            roleAttribute.getAttributeValue().add(roleValue);
            subjectAttributes.getAttribute().add(roleAttribute);
        }

        for (Permission curPermission : info.getObjectPermissions()) {
            if (curPermission instanceof KeyValuePermission) {
                for (String curPermValue : ((KeyValuePermission) curPermission).getValues()) {
                    AttributeType subjAttr = new AttributeType();
                    AttributeValueType subjAttrValue = new AttributeValueType();
                    subjAttr.setAttributeId(((KeyValuePermission) curPermission).getKey());
                    subjAttr.setIncludeInResult(false);
                    subjAttrValue.setDataType(XACMLConstants.STRING_DATA_TYPE);
                    LOGGER.trace("Adding permission: {}:{} for subject: {}",
                            new Object[] {((KeyValuePermission) curPermission).getKey(),
                                    curPermValue, subject});
                    subjAttrValue.getContent().add(curPermValue);
                    subjAttr.getAttributeValue().add(subjAttrValue);
                    subjectAttributes.getAttribute().add(subjAttr);
                }
            } else {
                LOGGER.warn(
                        "Permissions for subject were not of type KeyValuePermission, cannot add any subject permissions to the request.");
            }
        }
        return subjectAttributes;
    }

    @Override
    public boolean isPermittedAll(PrincipalCollection subjectPrincipal, String... permissions) {
        boolean[] results = isPermitted(subjectPrincipal, permissions);
        for (int i = 0; i < results.length; i++) {
            if (!results[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isPermittedAll(PrincipalCollection subjectPrincipal,
            Collection<Permission> permissions) {
        List<Permission> permissionList = new ArrayList<Permission>(permissions);
        boolean[] results = isPermitted(subjectPrincipal, permissionList);
        for (int i = 0; i < results.length; i++) {
            if (!results[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void checkPermission(PrincipalCollection subjectPrincipal, String permission)
            throws AuthorizationException {
        if (!isPermitted(subjectPrincipal, permission)) {
            throw new AuthorizationException(
                    subjectPrincipal.getPrimaryPrincipal() + AUTHZ_PERMITTED_EXCEPTION);
        }
    }

    @Override
    public void checkPermission(PrincipalCollection subjectPrincipal, Permission permission)
            throws AuthorizationException {
        if (!isPermitted(subjectPrincipal, permission)) {
            throw new AuthorizationException(
                    subjectPrincipal.getPrimaryPrincipal() + AUTHZ_PERMITTED_EXCEPTION);
        }
    }

    @Override
    public void checkPermissions(PrincipalCollection subjectPrincipal, String... permissions)
            throws AuthorizationException {
        if (!isPermittedAll(subjectPrincipal, permissions)) {
            throw new AuthorizationException(
                    subjectPrincipal.getPrimaryPrincipal() + AUTHZ_PERMITTED_EXCEPTION);
        }
    }

    @Override
    public void checkPermissions(PrincipalCollection subjectPrincipal,
            Collection<Permission> permissions) throws AuthorizationException {
        if (!isPermittedAll(subjectPrincipal, permissions)) {
            throw new AuthorizationException(
                    subjectPrincipal.getPrimaryPrincipal() + AUTHZ_PERMITTED_EXCEPTION);
        }
    }

    @Override
    public boolean hasRole(PrincipalCollection subjectPrincipal, String roleIdentifier) {
        AuthorizationInfo info = getAuthorizationInfo(subjectPrincipal);
        return info.getRoles().contains(roleIdentifier);
    }

    @Override
    public boolean[] hasRoles(PrincipalCollection subjectPrincipal, List<String> roleIdentifiers) {
        boolean[] hasRoleArray = new boolean[roleIdentifiers.size()];
        for (int i = 0; i < roleIdentifiers.size(); i++) {
            hasRoleArray[i] = hasRole(subjectPrincipal, roleIdentifiers.get(i));
        }
        return hasRoleArray;
    }

    @Override
    public boolean hasAllRoles(PrincipalCollection subjectPrincipal,
            Collection<String> roleIdentifiers) {
        List<String> roleList = new ArrayList<String>(roleIdentifiers);
        boolean[] results = hasRoles(subjectPrincipal, roleList);
        for (int i = 0; i < results.length; i++) {
            if (!results[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void checkRole(PrincipalCollection subjectPrincipal, String roleIdentifier)
            throws AuthorizationException {
        if (!hasRole(subjectPrincipal, roleIdentifier)) {
            throw new AuthorizationException(
                    subjectPrincipal.getPrimaryPrincipal() + AUTHZ_ROLE_EXCEPTION);
        }

    }

    @Override
    public void checkRoles(PrincipalCollection subjectPrincipal, Collection<String> roleIdentifiers)
            throws AuthorizationException {
        if (!hasAllRoles(subjectPrincipal, roleIdentifiers)) {
            throw new AuthorizationException(
                    subjectPrincipal.getPrimaryPrincipal() + AUTHZ_ROLE_EXCEPTION);
        }
    }

    @Override
    public void checkRoles(PrincipalCollection subjectPrincipal, String... roleIdentifiers)
            throws AuthorizationException {
        if (!hasAllRoles(subjectPrincipal, Arrays.asList(roleIdentifiers))) {
            throw new AuthorizationException(
                    subjectPrincipal.getPrimaryPrincipal() + AUTHZ_ROLE_EXCEPTION);
        }
    }

}
