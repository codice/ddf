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
package ddf.security.sts.claimsHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.sts.claims.Claim;
import org.apache.cxf.sts.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.RequestClaimCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;

public class RoleClaimsHandler implements ClaimsHandler {

    private final Logger logger = LoggerFactory.getLogger(RoleClaimsHandler.class);

    private Map<String, String> claimsLdapAttributeMapping;

    private LdapTemplate ldapTemplate;

    private String delimiter = ";";

    private String objectClass = "groupOfNames";

    private String memberNameAttribute = "member";

    private String userNameAttribute = "uid";

    private String groupNameAttribute = "cn";

    private String userBaseDn;

    private String groupBaseDn;

    private String roleClaimType = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

    private String propertyFileLocation;

    public URI getRoleURI() {
        URI uri = null;
        try {
            uri = new URI(roleClaimType);
        } catch (URISyntaxException e) {
            logger.warn("Unable to add role claim type.", e);
        }
        return uri;
    }

    public String getPropertyFileLocation() {
        return propertyFileLocation;
    }

    public void setPropertyFileLocation(String propertyFileLocation) {
        if (propertyFileLocation != null && !propertyFileLocation.isEmpty()
                && !propertyFileLocation.equals(this.propertyFileLocation)) {
            setClaimsLdapAttributeMapping(AttributeMapLoader
                    .buildClaimsMapFile(propertyFileLocation));
        }
        this.propertyFileLocation = propertyFileLocation;
    }

    public String getRoleClaimType() {
        return roleClaimType;
    }

    public void setRoleClaimType(String roleClaimType) {
        this.roleClaimType = roleClaimType;
    }

    public String getGroupNameAttribute() {
        return groupNameAttribute;
    }

    public void setGroupNameAttribute(String groupNameAttribute) {
        this.groupNameAttribute = groupNameAttribute;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getGroupBaseDn() {
        return groupBaseDn;
    }

    public void setGroupBaseDn(String groupBaseDn) {
        this.groupBaseDn = groupBaseDn;
    }

    public LdapTemplate getLdapTemplate() {
        return ldapTemplate;
    }

    public void setLdapTemplate(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    public String getUserNameAttribute() {
        return userNameAttribute;
    }

    public void setUserNameAttribute(String userNameAttribute) {
        this.userNameAttribute = userNameAttribute;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public String getMemberNameAttribute() {
        return memberNameAttribute;
    }

    public void setMemberNameAttribute(String memberNameAttribute) {
        this.memberNameAttribute = memberNameAttribute;
    }

    public String getUserBaseDn() {
        return userBaseDn;
    }

    public void setUserBaseDn(String userBaseDn) {
        this.userBaseDn = userBaseDn;
    }

    public void setClaimsLdapAttributeMapping(Map<String, String> ldapClaimMapping) {
        this.claimsLdapAttributeMapping = ldapClaimMapping;
    }

    public Map<String, String> getClaimsLdapAttributeMapping() {
        return claimsLdapAttributeMapping;
    }

    @Override
    public List<URI> getSupportedClaimTypes() {
        List<URI> uriList = new ArrayList<URI>();
        uriList.add(getRoleURI());

        return uriList;
    }

    @Override
    public ClaimCollection retrieveClaimValues(RequestClaimCollection claims,
            ClaimsParameters parameters) {
        String[] attributes = {groupNameAttribute, memberNameAttribute};
        ClaimCollection claimsColl = new ClaimCollection();
        try {
            Principal principal = parameters.getPrincipal();

            String user = AttributeMapLoader.getUser(principal);
            if (user == null) {
                logger.warn("Could not determine user name, possible authentication error. Returning no claims.");
                return new ClaimCollection();
            }

            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectClass", getObjectClass())).and(
                    new EqualsFilter(getMemberNameAttribute(), getUserNameAttribute() + "=" + user
                            + "," + getUserBaseDn()));

            AttributesMapper mapper = new AttributesMapper() {
                public Object mapFromAttributes(Attributes attrs) throws NamingException {
                    Map<String, Attribute> map = new HashMap<String, Attribute>();
                    NamingEnumeration<? extends Attribute> attrEnum = attrs.getAll();
                    while (attrEnum.hasMore()) {
                        Attribute att = attrEnum.next();
                        map.put(att.getID(), att);
                    }
                    return map;
                }
            };

            List<?> results = ldapTemplate.search(groupBaseDn, filter.toString(),
                    SearchControls.SUBTREE_SCOPE, attributes, mapper);

            for (Object result : results) {
                Map<String, Attribute> ldapAttributes = null;
                ldapAttributes = CastUtils.cast((Map<?, ?>) result);

                Attribute attr = ldapAttributes.get(groupNameAttribute);
                if (attr == null) {
                    logger.trace("Claim '{}' is null", roleClaimType);
                } else {
                    Claim c = new Claim();
                    c.setClaimType(getRoleURI());
                    c.setPrincipal(principal);

                    StringBuilder claimValue = new StringBuilder();
                    try {
                        NamingEnumeration<?> list = (NamingEnumeration<?>) attr.getAll();
                        while (list.hasMore()) {
                            Object obj = list.next();
                            if (!(obj instanceof String)) {
                                logger.warn(
                                        "LDAP attribute '{}' has got an unsupported value type",
                                        groupNameAttribute);
                                break;
                            }
                            claimValue.append((String) obj);
                            if (list.hasMore()) {
                                claimValue.append(getDelimiter());
                            }
                        }
                    } catch (NamingException ex) {
                        logger.warn("Failed to read value of LDAP attribute '{}'",
                                groupNameAttribute);
                    }

                    c.setValue(claimValue.toString());
                    // c.setIssuer(issuer);
                    // c.setOriginalIssuer(originalIssuer);
                    // c.setNamespace(namespace);
                    claimsColl.add(c);
                }
            }
        } catch (Exception e) {
            logger.error("Unable to set role claims.", e);
        }
        return claimsColl;
    }

}
