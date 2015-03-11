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

import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.exception.InvalidConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RoleClaimsHandler implements ClaimsHandler {

    private final Logger logger = LoggerFactory.getLogger(RoleClaimsHandler.class);

    private Map<String, String> claimsLdapAttributeMapping;

    private LdapConnection connection;

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

    public LdapConnection getLdapConnection() {
        return connection;
    }

    public void setLdapConnection(LdapConnection connection) {
        this.connection = connection;
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
    public ProcessedClaimCollection retrieveClaimValues(ClaimCollection claims,
            ClaimsParameters parameters) {
        String[] attributes = {groupNameAttribute, memberNameAttribute};
        ProcessedClaimCollection claimsColl = new ProcessedClaimCollection();
        try {
            Principal principal = parameters.getPrincipal();

            String user = AttributeMapLoader.getUser(principal);
            if (user == null) {
                logger.warn("Could not determine user name, possible authentication error. Returning no claims.");
                return new ProcessedClaimCollection();
            }

            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectClass", getObjectClass())).and(
                    new EqualsFilter(getMemberNameAttribute(), getUserNameAttribute() + "=" + user
                            + "," + getUserBaseDn()));

            String filterString = filter.toString();
            logger.trace("Executing ldap search with base dn of {} and filter of {}", this.groupBaseDn, filterString);

            connection.bind();
            EntryCursor entryCursor = connection.search(groupBaseDn, filterString,
                    SearchScope.SUBTREE, attributes);

            Entry entry;
            while(entryCursor.next()) {
                entry = entryCursor.get();

                Attribute attr = entry.get(groupNameAttribute);
                if (attr == null) {
                    logger.trace("Claim '{}' is null", roleClaimType);
                } else {
                    ProcessedClaim c = new ProcessedClaim();
                    c.setClaimType(getRoleURI());
                    c.setPrincipal(principal);

                    Iterator<Value<?>> valueIterator = attr.iterator();
                    while(valueIterator.hasNext()) {
                        Value<?> value = valueIterator.next();

                        Object objValue = value.getValue();
                        if (!(objValue instanceof String)) {
                            logger.warn("LDAP attribute '{}' has got an unsupported value type",
                                    groupNameAttribute);
                            break;
                        }
                        String itemValue = (String) objValue;
                        c.addValue(itemValue);
                    }
                    claimsColl.add(c);
                }
            }
        } catch (InvalidConnectionException e) {
            logger.warn("Cannot connect to server, therefore unable to set role claims.");
        } catch (Exception e) {
            logger.error("Unable to set role claims.", e);
        } finally {
            try {
                connection.unBind();
            } catch (LdapException ignore) {
            }
        }
        return claimsColl;
    }

}
