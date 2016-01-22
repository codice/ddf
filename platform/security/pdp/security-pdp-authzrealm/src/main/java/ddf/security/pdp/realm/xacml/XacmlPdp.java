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

package ddf.security.pdp.realm.xacml;

import java.util.List;

import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.common.audit.SecurityLogger;
import ddf.security.pdp.realm.xacml.processor.BalanaClient;
import ddf.security.pdp.realm.xacml.processor.PdpException;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributesType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.DecisionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RequestType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ResponseType;

/**
 * Performs authorization backed by a XACML-based PDP.
 */
public class XacmlPdp {

    private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdp.class);

    private BalanaClient pdp;

    /**
     * Creates a general
     */
    public XacmlPdp(String dirPath) throws PdpException {
        super();
        pdp = new BalanaClient(dirPath);
        LOGGER.debug("Creating new PDP-backed Authorizing Realm");
    }

    public boolean isPermitted(String primaryPrincipal, AuthorizationInfo info,
            KeyValueCollectionPermission curPermission) {
        boolean curResponse;
        LOGGER.debug("Checking if {} has access for action {}", primaryPrincipal,
                curPermission.getAction());

        SecurityLogger.logInfo("Checking if [" + primaryPrincipal + "] has access for action "
                + curPermission.getAction());

        if (CollectionUtils.isEmpty(info.getObjectPermissions()) && CollectionUtils.isEmpty(
                info.getStringPermissions()) && CollectionUtils.isEmpty(info.getRoles())
                && !CollectionUtils.isEmpty(curPermission.getKeyValuePermissionList())) {
            return false;
        }

        if ((!CollectionUtils.isEmpty(info.getObjectPermissions()) || !CollectionUtils.isEmpty(
                info.getStringPermissions()) || !CollectionUtils.isEmpty(info.getRoles()))
                && CollectionUtils.isEmpty(curPermission.getKeyValuePermissionList())) {
            return true;
        }

        LOGGER.debug("Received authZ info, creating XACML request.");
        RequestType curRequest = createXACMLRequest(primaryPrincipal, info, curPermission);
        LOGGER.debug("Created XACML request, calling PDP.");

        curResponse = isPermitted(curRequest);
        return curResponse;
    }

    protected RequestType createXACMLRequest(String subject, AuthorizationInfo info,
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
        actionValue.getContent()
                .add(permission.getAction());
        actionAttribute.getAttributeValue()
                .add(actionValue);
        actionAttributes.getAttribute()
                .add(actionAttribute);

        xacmlRequestType.getAttributes()
                .add(actionAttributes);

        // Adding permissions for the calling subject
        AttributesType subjectAttributes = createSubjectAttributes(subject, info);
        xacmlRequestType.getAttributes()
                .add(subjectAttributes);

        // Adding permissions for the resource
        AttributesType metadataAttributes = new AttributesType();
        metadataAttributes.setCategory(XACMLConstants.RESOURCE_CATEGORY);

        if (permission instanceof KeyValueCollectionPermission) {
            List<KeyValuePermission> tmpList = ((KeyValueCollectionPermission) permission).getKeyValuePermissionList();
            for (KeyValuePermission curPermission : tmpList) {
                for (String curPermValue : curPermission.getValues()) {
                    AttributeType resourceAttribute = new AttributeType();
                    AttributeValueType resourceAttributeValue = new AttributeValueType();
                    resourceAttribute.setAttributeId(curPermission.getKey());
                    resourceAttribute.setIncludeInResult(false);
                    resourceAttributeValue.setDataType(XACMLConstants.STRING_DATA_TYPE);
                    LOGGER.trace("Adding permission: {}:{} for incoming resource",
                            new Object[] {curPermission.getKey(), curPermValue});
                    resourceAttributeValue.getContent()
                            .add(curPermValue);
                    resourceAttribute.getAttributeValue()
                            .add(resourceAttributeValue);
                    metadataAttributes.getAttribute()
                            .add(resourceAttribute);
                }

            }

            xacmlRequestType.getAttributes()
                    .add(metadataAttributes);
        } else {
            LOGGER.warn(
                    "Permission on the resource need to be of type KeyValueCollectionPermission, cannot process this resource.");
        }

        return xacmlRequestType;
    }

    protected boolean isPermitted(RequestType xacmlRequest) {
        boolean permitted;
        ResponseType xacmlResponse;

        try {
            LOGGER.debug("Calling PDP to evaluate XACML request.");
            xacmlResponse = pdp.evaluate(xacmlRequest);
            LOGGER.debug("Received response from PDP.");
            permitted = xacmlResponse != null && xacmlResponse.getResult()
                    .get(0)
                    .getDecision() == DecisionType.PERMIT;
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
        subjectValue.getContent()
                .add(subject);
        subjectAttribute.getAttributeValue()
                .add(subjectValue);
        subjectAttributes.getAttribute()
                .add(subjectAttribute);

        for (String curRole : info.getRoles()) {
            AttributeType roleAttribute = new AttributeType();
            roleAttribute.setAttributeId(XACMLConstants.ROLE_CLAIM);
            roleAttribute.setIncludeInResult(false);
            AttributeValueType roleValue = new AttributeValueType();
            roleValue.setDataType(XACMLConstants.STRING_DATA_TYPE);
            LOGGER.trace("Adding role: {} for subject: {}", curRole, subject);
            roleValue.getContent()
                    .add(curRole);
            roleAttribute.getAttributeValue()
                    .add(roleValue);
            subjectAttributes.getAttribute()
                    .add(roleAttribute);
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
                            ((KeyValuePermission) curPermission).getKey(), curPermValue, subject);
                    subjAttrValue.getContent()
                            .add(curPermValue);
                    subjAttr.getAttributeValue()
                            .add(subjAttrValue);
                    subjectAttributes.getAttribute()
                            .add(subjAttr);
                }
            } else {
                LOGGER.warn(
                        "Permissions for subject were not of type KeyValuePermission, cannot add any subject permissions to the request.");
            }
        }
        return subjectAttributes;
    }

}
