/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.security.assertion.impl;

import ddf.security.SecurityConstants;
import ddf.security.assertion.SecurityAssertion;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.AuthzDecisionStatement;
import org.opensaml.saml2.core.EncryptedAttribute;
import org.opensaml.xml.Namespace;
import org.opensaml.xml.NamespaceManager;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSBooleanValue;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.util.AttributeMap;
import org.opensaml.xml.util.IDIndex;
import org.opensaml.xml.validation.ValidationException;
import org.opensaml.xml.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the SecurityAssertion interface. This class wraps a SecurityToken.
 *
 * @author tustisos
 */
public class SecurityAssertionImpl implements SecurityAssertion {
    private static final String ISSUER_TAG = "Issuer";

    private static final String SUBJECT_TAG = "NameID";

    private static final String ATTR_STMT_TAG = "AttributeStatement";

    private static final String ATTR_TAG = "Attribute";

    private static final String ATTR_VAL_TAG = "AttributeValue";

    /**
     * Log4j Logger
     */
    private Logger LOGGER = LoggerFactory.getLogger(SecurityConstants.SECURITY_LOGGER);

    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Default Hash Value
     */
    private static final int DEFAULT_HASH = 127;

    /**
     * Wrapped SecurityToken.
     */
    private SecurityToken securityToken;

    /**
     * Principal associated with the security token
     */
    private Principal principal;

    private String name;

    private String issuer;

    private transient List<AttributeStatement> attributeStatements;

    /**
     * Uninitialized Constructor
     */
    public SecurityAssertionImpl() {
        init();
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        init();
    }

    /**
     * Default Constructor
     *
     * @param securityToken - token to wrap
     */
    public SecurityAssertionImpl(SecurityToken securityToken) {
        init();
        this.securityToken = securityToken;
        parseToken(securityToken);
    }

    private void init() {
        attributeStatements = new ArrayList<>();
    }

    /**
     * Parses the SecurityToken by wrapping within an AssertionWrapper.
     *
     * @param securityToken SecurityToken
     */
    private void parseToken(SecurityToken securityToken) {
        XMLStreamReader xmlStreamReader = StaxUtils.createXMLStreamReader(securityToken.getToken());

        try {
            AttrStatement attributeStatement = null;
            Attr attribute = null;
            while (xmlStreamReader.hasNext()) {
                int event = xmlStreamReader.next();
                switch (event) {
                case XMLStreamConstants.START_ELEMENT: {
                    String localName = xmlStreamReader.getLocalName();
                    switch (localName) {
                    case SUBJECT_TAG:
                        name = xmlStreamReader.getElementText();
                        break;
                    case ATTR_STMT_TAG:
                        attributeStatement = new AttrStatement();
                        attributeStatements.add(attributeStatement);
                        break;
                    case ATTR_TAG:
                        attribute = new Attr();
                        if (attributeStatement != null) {
                            attributeStatement.addAttribute(attribute);
                        }
                        int attrs = xmlStreamReader.getAttributeCount();
                        for (int i = 0; i < attrs; i++) {
                            String name = xmlStreamReader.getAttributeLocalName(i);
                            String value = xmlStreamReader.getAttributeValue(i);
                            if (name.equals("Name")) {
                                attribute.setName(value);
                            } else if (name.equals("NameFormat")) {
                                attribute.setNameFormat(value);
                            }
                        }
                        break;
                    case ATTR_VAL_TAG:
                        XSString xsString = new XMLString();
                        xsString.setValue(xmlStreamReader.getElementText());
                        if (attribute != null) {
                            attribute.addAttributeValue(xsString);
                        }
                        break;
                    case ISSUER_TAG:
                        issuer = xmlStreamReader.getElementText();
                        break;
                    }
                    break;
                }
                case XMLStreamConstants.END_ELEMENT: {
                    String localName = xmlStreamReader.getLocalName();
                    switch (localName) {
                    case ATTR_STMT_TAG:
                        attributeStatement = null;
                        break;
                    case ATTR_TAG:
                        attribute = null;
                        break;
                    }
                    break;
                }
                }
            }
        } catch (XMLStreamException e) {
            LOGGER.error("Unable to parse security token.", e);
        } finally {
            try {
                xmlStreamReader.close();
            } catch (XMLStreamException ignore) {
                //ignore
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.security.assertion.SecurityAssertion#getPrincipal()
     */
    @Override
    public Principal getPrincipal() {
        if (securityToken != null) {
            if (principal == null) {
                principal = new AssertionPrincipal(name);
            }
            return principal;
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.security.assertion.SecurityAssertion#getIssuer()
     */
    @Override
    public String getIssuer() {
        return issuer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.security.assertion.SecurityAssertion#getAttibuteStatements()
     */
    @Override
    public List<AttributeStatement> getAttibuteStatements() {
        return Collections.unmodifiableList(attributeStatements);
    }

    @Override
    public List<AuthnStatement> getAuthnStatements() {
        return new ArrayList<>();
    }

    @Override
    public List<AuthzDecisionStatement> getAuthzDecisionStatements() {
        return new ArrayList<>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.security.assertion.SecurityAssertion#getSecurityToken()
     */
    @Override
    public SecurityToken getSecurityToken() {
        return securityToken;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Principal: " + getPrincipal() + ", Attributes: ");
        for (AttributeStatement attributeStatement : getAttibuteStatements()) {
            for (Attribute attr : attributeStatement.getAttributes()) {
                result.append("[ ");
                result.append(attr.getName());
                result.append(" : ");
                for (int i = 0; i < attr.getAttributeValues().size(); i++) {
                    result.append(((XSString) attr.getAttributeValues().get(i)).getValue());
                }
                result.append("] ");
            }
        }
        // add this back in when we support parsing this information
        //        result.append(", AuthnStatements: ");
        //        for (AuthnStatement authStatement : getAuthnStatements()) {
        //            result.append("[ ");
        //            result.append(authStatement.getAuthnContext() + " : ");
        //            result.append(authStatement.getAuthnInstant() + " : ");
        //            result.append(authStatement.getDOM().getTextContent());
        //            result.append("] ");
        //        }
        //        result.append(", AuthzDecisionStatements: ");
        //        for (AuthzDecisionStatement authDecision : getAuthzDecisionStatements()) {
        //            result.append("[ ");
        //            result.append(authDecision.getDecision().toString());
        //            result.append(" ]");
        //        }
        return result.toString();
    }

    /**
     * Represents the String values parsed out of the SAML assertion.
     * This class only has the value field implemented for performance reasons.
     */
    private static class XMLString implements XSString {
        private String value;

        protected XMLString() {
        }

        public String getValue() {
            return value;
        }

        public void setValue(String newValue) {
            value = newValue;
        }

        @Override
        public void addNamespace(Namespace namespace) {

        }

        @Override
        public void detach() {

        }

        @Override
        public Element getDOM() {
            return null;
        }

        @Override
        public QName getElementQName() {
            return null;
        }

        @Override
        public IDIndex getIDIndex() {
            return null;
        }

        @Override
        public NamespaceManager getNamespaceManager() {
            return null;
        }

        @Override
        public Set<Namespace> getNamespaces() {
            return null;
        }

        @Override
        public String getNoNamespaceSchemaLocation() {
            return null;
        }

        public List<XMLObject> getOrderedChildren() {
            return null;
        }

        @Override
        public XMLObject getParent() {
            return null;
        }

        @Override
        public String getSchemaLocation() {
            return null;
        }

        @Override
        public QName getSchemaType() {
            return null;
        }

        @Override
        public boolean hasChildren() {
            return false;
        }

        @Override
        public boolean hasParent() {
            return false;
        }

        @Override
        public void releaseChildrenDOM(boolean propagateRelease) {

        }

        @Override
        public void releaseDOM() {

        }

        @Override
        public void releaseParentDOM(boolean propagateRelease) {

        }

        @Override
        public void removeNamespace(Namespace namespace) {

        }

        @Override
        public XMLObject resolveID(String id) {
            return null;
        }

        @Override
        public XMLObject resolveIDFromRoot(String id) {
            return null;
        }

        @Override
        public void setDOM(Element dom) {

        }

        @Override
        public void setNoNamespaceSchemaLocation(String location) {

        }

        @Override
        public void setParent(XMLObject parent) {

        }

        @Override
        public void setSchemaLocation(String location) {

        }

        @Override
        public Boolean isNil() {
            return false;
        }

        @Override
        public XSBooleanValue isNilXSBoolean() {
            return new XSBooleanValue();
        }

        @Override
        public void setNil(Boolean newNil) {

        }

        @Override
        public void setNil(XSBooleanValue newNil) {

        }

        @Override
        public List<Validator> getValidators() {
            return null;
        }

        @Override
        public void registerValidator(Validator validator) {

        }

        @Override
        public void deregisterValidator(Validator validator) {

        }

        @Override
        public void validate(boolean validateDescendants) throws ValidationException {

        }
    }

    /**
     * This class represents an attribute that has been specified in the SAML assertion.
     * Only the required minimum methods are implemented for performance reasons.
     */
    private static class Attr implements Attribute {

        private String name;

        private String nameFormat;

        private String friendlyName;

        private List<XMLObject> attributeValues = new ArrayList<>();

        protected Attr() {
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getNameFormat() {
            return nameFormat;
        }

        @Override
        public void setNameFormat(String nameFormat) {
            this.nameFormat = nameFormat;
        }

        @Override
        public String getFriendlyName() {
            return friendlyName;
        }

        @Override
        public void setFriendlyName(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        @Override
        public List<XMLObject> getAttributeValues() {
            return Collections.unmodifiableList(attributeValues);
        }

        private void addAttributeValue(XMLObject xmlObject) {
            attributeValues.add(xmlObject);
        }

        @Override
        public AttributeMap getUnknownAttributes() {
            return null;
        }

        @Override
        public void addNamespace(Namespace namespace) {

        }

        @Override
        public void detach() {

        }

        @Override
        public Element getDOM() {
            return null;
        }

        @Override
        public QName getElementQName() {
            return null;
        }

        @Override
        public IDIndex getIDIndex() {
            return null;
        }

        @Override
        public NamespaceManager getNamespaceManager() {
            return null;
        }

        @Override
        public Set<Namespace> getNamespaces() {
            return null;
        }

        @Override
        public String getNoNamespaceSchemaLocation() {
            return null;
        }

        @Override
        public List<XMLObject> getOrderedChildren() {
            return null;
        }

        @Override
        public XMLObject getParent() {
            return null;
        }

        @Override
        public String getSchemaLocation() {
            return null;
        }

        @Override
        public QName getSchemaType() {
            return null;
        }

        @Override
        public boolean hasChildren() {
            return false;
        }

        @Override
        public boolean hasParent() {
            return false;
        }

        @Override
        public void releaseChildrenDOM(boolean propagateRelease) {

        }

        @Override
        public void releaseDOM() {

        }

        @Override
        public void releaseParentDOM(boolean propagateRelease) {

        }

        @Override
        public void removeNamespace(Namespace namespace) {

        }

        @Override
        public XMLObject resolveID(String id) {
            return null;
        }

        @Override
        public XMLObject resolveIDFromRoot(String id) {
            return null;
        }

        @Override
        public void setDOM(Element dom) {

        }

        @Override
        public void setNoNamespaceSchemaLocation(String location) {

        }

        @Override
        public void setParent(XMLObject parent) {

        }

        @Override
        public void setSchemaLocation(String location) {

        }

        @Override
        public Boolean isNil() {
            return false;
        }

        @Override
        public XSBooleanValue isNilXSBoolean() {
            return new XSBooleanValue();
        }

        @Override
        public void setNil(Boolean newNil) {

        }

        @Override
        public void setNil(XSBooleanValue newNil) {

        }

        @Override
        public List<Validator> getValidators() {
            return null;
        }

        @Override
        public void registerValidator(Validator validator) {

        }

        @Override
        public void deregisterValidator(Validator validator) {

        }

        @Override
        public void validate(boolean validateDescendants) throws ValidationException {

        }
    }

    /**
     * This class represents an attribute statement within a SAML assertion.
     * Only the required minimum methods are implemented for performance reasons.
     */
    private static class AttrStatement implements AttributeStatement {

        private List<Attribute> attributes = new ArrayList<>();

        private List<EncryptedAttribute> encryptedAttributes = new ArrayList<>();

        protected AttrStatement() {
        }

        @Override
        public List<Attribute> getAttributes() {
            return Collections.unmodifiableList(attributes);
        }

        private void addAttribute(Attribute attribute) {
            attributes.add(attribute);
        }

        @Override
        public List<EncryptedAttribute> getEncryptedAttributes() {
            return Collections.unmodifiableList(encryptedAttributes);
        }

        private void addEncryptedAttribute(EncryptedAttribute attribute) {
            encryptedAttributes.add(attribute);
        }

        @Override
        public void addNamespace(Namespace namespace) {

        }

        @Override
        public void detach() {

        }

        @Override
        public Element getDOM() {
            return null;
        }

        @Override
        public QName getElementQName() {
            return null;
        }

        @Override
        public IDIndex getIDIndex() {
            return null;
        }

        @Override
        public NamespaceManager getNamespaceManager() {
            return null;
        }

        @Override
        public Set<Namespace> getNamespaces() {
            return null;
        }

        @Override
        public String getNoNamespaceSchemaLocation() {
            return null;
        }

        @Override
        public List<XMLObject> getOrderedChildren() {
            return null;
        }

        @Override
        public XMLObject getParent() {
            return null;
        }

        @Override
        public String getSchemaLocation() {
            return null;
        }

        @Override
        public QName getSchemaType() {
            return null;
        }

        @Override
        public boolean hasChildren() {
            return attributes.size() > 0;
        }

        @Override
        public boolean hasParent() {
            return false;
        }

        @Override
        public void releaseChildrenDOM(boolean propagateRelease) {

        }

        @Override
        public void releaseDOM() {

        }

        @Override
        public void releaseParentDOM(boolean propagateRelease) {

        }

        @Override
        public void removeNamespace(Namespace namespace) {

        }

        @Override
        public XMLObject resolveID(String id) {
            return null;
        }

        @Override
        public XMLObject resolveIDFromRoot(String id) {
            return null;
        }

        @Override
        public void setDOM(Element dom) {

        }

        @Override
        public void setNoNamespaceSchemaLocation(String location) {

        }

        @Override
        public void setParent(XMLObject parent) {

        }

        @Override
        public void setSchemaLocation(String location) {

        }

        @Override
        public Boolean isNil() {
            return false;
        }

        @Override
        public XSBooleanValue isNilXSBoolean() {
            return new XSBooleanValue();
        }

        @Override
        public void setNil(Boolean newNil) {

        }

        @Override
        public void setNil(XSBooleanValue newNil) {

        }

        @Override
        public List<Validator> getValidators() {
            return null;
        }

        @Override
        public void registerValidator(Validator validator) {

        }

        @Override
        public void deregisterValidator(Validator validator) {

        }

        @Override
        public void validate(boolean validateDescendants) throws ValidationException {

        }
    }

    /**
     * Principal implementation that returns values obtained from the assertion.
     */
    private static class AssertionPrincipal implements Principal, Serializable {
        private String name;

        public AssertionPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object another) {
            if (!(another instanceof Principal)) {
                return false;
            }
            Principal tmpPrin = (Principal) another;
            if (tmpPrin.getName() == null && getName() != null) {
                return false;
            }
            if (tmpPrin.getName() != null && getName() == null) {
                return false;
            }
            if (tmpPrin.getName() == null && getName() == null) {
                return super.equals(another);
            }
            return tmpPrin.getName().equals(getName());
        }

        @Override
        public int hashCode() {
            if (getName() == null) {
                return DEFAULT_HASH;
            }
            return getName().hashCode();
        }

        /**
         * Returns the name of the principal in string format.
         */
        public String toString() {
            return getName();
        }
    }
}
