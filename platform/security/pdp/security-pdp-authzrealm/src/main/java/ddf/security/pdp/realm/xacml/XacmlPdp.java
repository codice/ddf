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
package ddf.security.pdp.realm.xacml;

import com.google.common.net.InetAddresses;
import ddf.security.audit.SecurityLogger;
import ddf.security.pdp.realm.xacml.processor.PdpException;
import ddf.security.pdp.realm.xacml.processor.XacmlClient;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import java.util.List;
import java.util.Locale;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributesType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.DecisionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RequestType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ResponseType;
import org.apache.commons.validator.routines.CalendarValidator;
import org.apache.commons.validator.routines.DateValidator;
import org.apache.commons.validator.routines.DoubleValidator;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.IntegerValidator;
import org.apache.commons.validator.routines.TimeValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.util.CollectionUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.codice.ddf.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Performs authorization backed by a XACML-based PDP. */
public class XacmlPdp {

  private static final Logger LOGGER = LoggerFactory.getLogger(XacmlPdp.class);

  private static final String ROLE_CLAIM =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

  private static final String STRING_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#string";

  private static final String BOOLEAN_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#boolean";

  private static final String INTEGER_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#integer";

  private static final String DOUBLE_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#double";

  private static final String TIME_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#time";

  private static final String DATE_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#date";

  private static final String DATE_TIME_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#dateTime";

  private static final String URI_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#anyURI";

  private static final String RFC822_NAME_DATA_TYPE =
      "urn:oasis:names:tc:xacml:1.0:data-type:rfc822Name";

  private static final String IP_ADDRESS_DATA_TYPE =
      "urn:oasis:names:tc:xacml:2.0:data-type:ipAddress";

  private static final String X500_NAME_DATA_TYPE =
      "urn:oasis:names:tc:xacml:1.0:data-type:x500Name";

  private static final String ACTION_CATEGORY =
      "urn:oasis:names:tc:xacml:3.0:attribute-category:action";

  private static final String RESOURCE_CATEGORY =
      "urn:oasis:names:tc:xacml:3.0:attribute-category:resource";

  private static final String ENVIRONMENT_CATEGORY =
      "urn:oasis:names:tc:xacml:3.0:attribute-category:environment";

  private static final String ACTION_ID = "urn:oasis:names:tc:xacml:1.0:action:action-id";

  private static final String ACCESS_SUBJECT_CATEGORY =
      "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";

  private static final String SUBJECT_ID = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";

  private static final String FILTER_ACTION = "filter";

  private XacmlClient pdp;

  private List<String> environmentAttributes;

  private SecurityLogger securityLogger;

  /** Creates a general */
  public XacmlPdp(
      String dirPath,
      Parser parser,
      List<String> environmentAttributes,
      SecurityLogger securityLogger)
      throws PdpException {
    super();
    this.securityLogger = securityLogger;
    pdp = new XacmlClient(dirPath, parser, securityLogger);
    this.environmentAttributes = environmentAttributes;
    LOGGER.debug("Creating new PDP-backed Authorizing Realm");
  }

  public boolean isPermitted(
      String primaryPrincipal, AuthorizationInfo info, KeyValueCollectionPermission curPermission) {
    boolean curResponse;
    LOGGER.debug(
        "Checking if {} has access for action {}", primaryPrincipal, curPermission.getAction());

    if (CollectionUtils.isEmpty(info.getObjectPermissions())
        && CollectionUtils.isEmpty(info.getStringPermissions())
        && CollectionUtils.isEmpty(info.getRoles())
        && !CollectionUtils.isEmpty(curPermission.getKeyValuePermissionList())) {
      securityLogger.audit(
          "XACML short-circuit denied ["
              + primaryPrincipal
              + "] access for action "
              + curPermission.getAction());
      return false;
    }

    if ((!CollectionUtils.isEmpty(info.getObjectPermissions())
            || !CollectionUtils.isEmpty(info.getStringPermissions())
            || !CollectionUtils.isEmpty(info.getRoles()))
        && CollectionUtils.isEmpty(curPermission.getKeyValuePermissionList())) {
      securityLogger.audit(
          "XACML short-circuit permitted ["
              + primaryPrincipal
              + "] access for action "
              + curPermission.getAction());
      return true;
    }

    LOGGER.debug("Received authZ info, creating XACML request.");
    RequestType curRequest = createXACMLRequest(primaryPrincipal, info, curPermission);
    LOGGER.debug("Created XACML request, calling PDP.");

    curResponse = isPermitted(curRequest);
    if (curResponse) {
      securityLogger.audit(
          "XACML permitted ["
              + primaryPrincipal
              + "] access for action "
              + curPermission.getAction());
    } else {
      securityLogger.audit(
          "XACML denied [" + primaryPrincipal + "] access for action " + curPermission.getAction());
    }
    return curResponse;
  }

  protected RequestType createXACMLRequest(
      String subject, AuthorizationInfo info, CollectionPermission permission) {
    LOGGER.debug(
        "Creating XACML request for subject: {} and metacard permissions {}", subject, permission);

    RequestType xacmlRequestType = new RequestType();
    xacmlRequestType.setCombinedDecision(false);
    xacmlRequestType.setReturnPolicyIdList(false);

    // Adding filter action
    AttributesType actionAttributes = new AttributesType();
    actionAttributes.setCategory(ACTION_CATEGORY);
    AttributeType actionAttribute = new AttributeType();
    actionAttribute.setAttributeId(ACTION_ID);
    actionAttribute.setIncludeInResult(false);
    AttributeValueType actionValue = new AttributeValueType();
    actionValue.setDataType(STRING_DATA_TYPE);
    LOGGER.trace("Adding action: {} for subject: {}", FILTER_ACTION, subject);
    actionValue.getContent().add(permission.getAction());
    actionAttribute.getAttributeValue().add(actionValue);
    actionAttributes.getAttribute().add(actionAttribute);

    xacmlRequestType.getAttributes().add(actionAttributes);

    // Adding permissions for the calling subject
    AttributesType subjectAttributes = createSubjectAttributes(subject, info);
    xacmlRequestType.getAttributes().add(subjectAttributes);

    // Adding permissions for the resource
    AttributesType metadataAttributes = new AttributesType();
    metadataAttributes.setCategory(RESOURCE_CATEGORY);

    AttributesType environmentAttributesType = new AttributesType();
    environmentAttributesType.setCategory(ENVIRONMENT_CATEGORY);
    if (!CollectionUtils.isEmpty(environmentAttributes)) {
      for (String envAttr : environmentAttributes) {
        String[] attr = envAttr.split("=");
        if (attr.length == 2) {
          AttributeType attributeType = new AttributeType();
          attributeType.setAttributeId(attr[0].trim());
          String[] attrVals = attr[1].split(",");
          for (String attrVal : attrVals) {
            AttributeValueType attributeValueType = new AttributeValueType();
            attributeValueType.setDataType(STRING_DATA_TYPE);
            attributeValueType.getContent().add(attrVal.trim());
            attributeType.getAttributeValue().add(attributeValueType);
          }
          environmentAttributesType.getAttribute().add(attributeType);
        }
      }
    }

    if (permission instanceof KeyValueCollectionPermission) {
      List<KeyValuePermission> tmpList =
          ((KeyValueCollectionPermission) permission).getKeyValuePermissionList();
      for (KeyValuePermission curPermission : tmpList) {
        AttributeType resourceAttribute = new AttributeType();
        resourceAttribute.setAttributeId(curPermission.getKey());
        resourceAttribute.setIncludeInResult(false);
        if (!curPermission.getValues().isEmpty()) {
          for (String curPermValue : curPermission.getValues()) {
            AttributeValueType resourceAttributeValue = new AttributeValueType();
            resourceAttributeValue.setDataType(getXacmlDataType(curPermValue));
            LOGGER.trace(
                "Adding permission: {}:{} for incoming resource",
                new Object[] {curPermission.getKey(), curPermValue});
            resourceAttributeValue.getContent().add(curPermValue);
            resourceAttribute.getAttributeValue().add(resourceAttributeValue);
          }
          metadataAttributes.getAttribute().add(resourceAttribute);
        }
      }

      xacmlRequestType.getAttributes().add(metadataAttributes);
      if (!CollectionUtils.isEmpty(environmentAttributes)) {
        xacmlRequestType.getAttributes().add(environmentAttributesType);
      }
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
      permitted =
          xacmlResponse != null
              && xacmlResponse.getResult().get(0).getDecision() == DecisionType.PERMIT;
      LOGGER.debug("Permitted: {}", permitted);
    } catch (PdpException e) {
      LOGGER.debug(e.getMessage(), e);
      permitted = false;
    }

    return permitted;
  }

  private AttributesType createSubjectAttributes(String subject, AuthorizationInfo info) {
    AttributesType subjectAttributes = new AttributesType();
    subjectAttributes.setCategory(ACCESS_SUBJECT_CATEGORY);
    AttributeType subjectAttribute = new AttributeType();
    subjectAttribute.setAttributeId(SUBJECT_ID);
    subjectAttribute.setIncludeInResult(false);
    AttributeValueType subjectValue = new AttributeValueType();
    subjectValue.setDataType(STRING_DATA_TYPE);
    LOGGER.debug("Adding subject: {}", subject);
    subjectValue.getContent().add(subject);
    subjectAttribute.getAttributeValue().add(subjectValue);
    subjectAttributes.getAttribute().add(subjectAttribute);

    AttributeType roleAttribute = new AttributeType();
    roleAttribute.setAttributeId(ROLE_CLAIM);
    roleAttribute.setIncludeInResult(false);
    if (!info.getRoles().isEmpty()) {
      for (String curRole : info.getRoles()) {
        AttributeValueType roleValue = new AttributeValueType();
        roleValue.setDataType(STRING_DATA_TYPE);
        LOGGER.trace("Adding role: {} for subject: {}", curRole, subject);
        roleValue.getContent().add(curRole);
        roleAttribute.getAttributeValue().add(roleValue);
      }
      subjectAttributes.getAttribute().add(roleAttribute);
    }

    for (Permission curPermission : info.getObjectPermissions()) {
      if (curPermission instanceof KeyValuePermission) {
        AttributeType subjAttr = new AttributeType();
        subjAttr.setAttributeId(((KeyValuePermission) curPermission).getKey());
        subjAttr.setIncludeInResult(false);
        if (!((KeyValuePermission) curPermission).getValues().isEmpty()) {
          for (String curPermValue : ((KeyValuePermission) curPermission).getValues()) {
            AttributeValueType subjAttrValue = new AttributeValueType();
            subjAttrValue.setDataType(getXacmlDataType(curPermValue));
            LOGGER.trace(
                "Adding permission: {}:{} for subject: {}",
                ((KeyValuePermission) curPermission).getKey(),
                curPermValue,
                subject);
            subjAttrValue.getContent().add(curPermValue);
            subjAttr.getAttributeValue().add(subjAttrValue);
          }
          subjectAttributes.getAttribute().add(subjAttr);
        }
      } else {
        LOGGER.warn(
            "Permissions for subject were not of type KeyValuePermission, cannot add any subject permissions to the request.");
      }
    }
    return subjectAttributes;
  }

  protected String getXacmlDataType(String curPermValue) {
    if ("false".equalsIgnoreCase(curPermValue) || "true".equalsIgnoreCase(curPermValue)) {
      return BOOLEAN_DATA_TYPE;
    } else if (IntegerValidator.getInstance().validate(curPermValue) != null) {
      return INTEGER_DATA_TYPE;
    } else if (DoubleValidator.getInstance().validate(curPermValue, Locale.getDefault()) != null) {
      return DOUBLE_DATA_TYPE;
    } else if (TimeValidator.getInstance().validate(curPermValue, "H:mm:ss") != null
        || TimeValidator.getInstance().validate(curPermValue, "H:mm:ss.SSS") != null
        || TimeValidator.getInstance().validate(curPermValue, "H:mm:ssXXX") != null
        || TimeValidator.getInstance().validate(curPermValue, "H:mm:ss.SSSXXX") != null) {
      return TIME_DATA_TYPE;
    } else if (DateValidator.getInstance().validate(curPermValue, "yyyy-MM-dd") != null
        || DateValidator.getInstance().validate(curPermValue, "yyyy-MM-ddXXX") != null) {
      return DATE_DATA_TYPE;
    } else if (CalendarValidator.getInstance().validate(curPermValue, "yyyy-MM-dd:ss'T'H:mm")
            != null
        || CalendarValidator.getInstance().validate(curPermValue, "yyyy-MM-dd'T'H:mm:ssXXX") != null
        || CalendarValidator.getInstance().validate(curPermValue, "yyyy-MM-dd'T'H:mm:ss.SSS")
            != null
        || CalendarValidator.getInstance().validate(curPermValue, "yyyy-MM-dd'T'H:mm:ss.SSSXXX")
            != null
        || CalendarValidator.getInstance().validate(curPermValue, "yyyy-MM-dd'T'H:mm:ss") != null) {
      return DATE_TIME_DATA_TYPE;
    } else if (EmailValidator.getInstance().isValid(curPermValue)) {
      return RFC822_NAME_DATA_TYPE;
    } else if (new UrlValidator().isValid(curPermValue)) {
      return URI_DATA_TYPE;
    } else if (InetAddresses.isUriInetAddress(curPermValue)) {
      return IP_ADDRESS_DATA_TYPE;
    } else {

      try {
        if (new X500Name(curPermValue).getRDNs().length > 0) {
          return X500_NAME_DATA_TYPE;
        }
      } catch (IllegalArgumentException e) {

      }
    }
    return STRING_DATA_TYPE;
  }
}
