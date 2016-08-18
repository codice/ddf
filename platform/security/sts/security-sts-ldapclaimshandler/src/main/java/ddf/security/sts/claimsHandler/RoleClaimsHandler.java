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
package ddf.security.sts.claimsHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.responses.BindResult;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;

public class RoleClaimsHandler implements ClaimsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleClaimsHandler.class);

    private Map<String, String> claimsLdapAttributeMapping;

    private LDAPConnectionFactory connectionFactory;

    private String delimiter = ";";

    private String objectClass = "groupOfNames";

    private String memberNameAttribute = "member";

    private String userNameAttribute = "uid";

    private String groupNameAttribute = "cn";

    private String userBaseDn;

    private String groupBaseDn;

    private String roleClaimType = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

    private String propertyFileLocation;

    private String bindUserCredentials;

    private String bindUserDN;

    public URI getRoleURI() {
        URI uri = null;
        try {
            uri = new URI(roleClaimType);
        } catch (URISyntaxException e) {
            LOGGER.info("Unable to add role claim type. Set log level for \"ddf.security.sts.claimsHandler\" to DEBUG for more information.");
            LOGGER.debug("Unable to add role claim type.", e);
        }
        return uri;
    }

    public String getPropertyFileLocation() {
        return propertyFileLocation;
    }

    public void setPropertyFileLocation(String propertyFileLocation) {
        if (propertyFileLocation != null && !propertyFileLocation.isEmpty()
                && !propertyFileLocation.equals(this.propertyFileLocation)) {
            setClaimsLdapAttributeMapping(AttributeMapLoader.buildClaimsMapFile(propertyFileLocation));
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

    public LDAPConnectionFactory getLdapConnectionFactory() {
        return connectionFactory;
    }

    public void setLdapConnectionFactory(LDAPConnectionFactory connection) {
        this.connectionFactory = connection;
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

    public Map<String, String> getClaimsLdapAttributeMapping() {
        return claimsLdapAttributeMapping;
    }

    public void setClaimsLdapAttributeMapping(Map<String, String> ldapClaimMapping) {
        this.claimsLdapAttributeMapping = ldapClaimMapping;
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
        Connection connection = null;
        try {
            Principal principal = parameters.getPrincipal();

            String user = AttributeMapLoader.getUser(principal);
            if (user == null) {
                LOGGER.info(
                        "Could not determine user name, possible authentication error. Returning no claims.");
                return new ProcessedClaimCollection();
            }

            AndFilter filter = new AndFilter();
            String userBaseDN = AttributeMapLoader.getBaseDN(principal, getUserBaseDn());
            filter.and(new EqualsFilter("objectClass", getObjectClass()))
                    .and(new EqualsFilter(getMemberNameAttribute(),
                            getUserNameAttribute() + "=" + user + "," + userBaseDN));

            String filterString = filter.toString();

            connection = connectionFactory.getConnection();
            if (connection != null) {
                BindResult bindResult = connection.bind(bindUserDN, bindUserCredentials.toCharArray());

                if (bindResult.isSuccess()) {
                    LOGGER.trace("Executing ldap search with base dn of {} and filter of {}",
                            groupBaseDn,
                            filterString);

                    ConnectionEntryReader entryReader = connection.search(groupBaseDn,
                            SearchScope.WHOLE_SUBTREE,
                            filter.toString(),
                            attributes);

                    SearchResultEntry entry;
                    while (entryReader.hasNext()) {
                        entry = entryReader.readEntry();

                        Attribute attr = entry.getAttribute(groupNameAttribute);
                        if (attr == null) {
                            LOGGER.trace("Claim '{}' is null", roleClaimType);
                        } else {
                            ProcessedClaim c = new ProcessedClaim();
                            c.setClaimType(getRoleURI());
                            c.setPrincipal(principal);

                            for (ByteString value : attr) {
                                String itemValue = value.toString();
                                c.addValue(itemValue);
                            }
                            claimsColl.add(c);
                        }
                    }
                } else {
                    LOGGER.info("LDAP Connection failed.");
                }
            }
        } catch (LdapException e) {
            LOGGER.info("Cannot connect to server, therefore unable to set role claims. Set log level for \"ddf.security.sts.claimsHandler\" to DEBUG for more information.");
            LOGGER.debug("Cannot connect to server, therefore unable to set role claims.", e);
        } catch (SearchResultReferenceIOException e) {
            LOGGER.info("Unable to set role claims. Set log level for \"ddf.security.sts.claimsHandler\" to DEBUG for more information.");
            LOGGER.debug("Unable to set role claims.", e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
        return claimsColl;
    }

    public void disconnect() {
        connectionFactory.close();
    }

    public void setBindUserDN(String bindUserDN) {
        this.bindUserDN = bindUserDN;
    }

    public void setBindUserCredentials(String bindUserCredentials) {
        this.bindUserCredentials = bindUserCredentials;
    }
}
