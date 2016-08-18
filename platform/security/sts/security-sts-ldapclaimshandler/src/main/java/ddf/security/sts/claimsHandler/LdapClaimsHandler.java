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
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
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

public class LdapClaimsHandler extends org.apache.cxf.sts.claims.LdapClaimsHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapClaimsHandler.class);

    private String propertyFileLocation;

    private LDAPConnectionFactory connectionFactory;

    private String bindUserCredentials;

    private String bindUserDN;

    public LdapClaimsHandler() {
        super();
    }

    public LDAPConnectionFactory getLdapConnectionFactory() {
        return connectionFactory;
    }

    public void setLdapConnectionFactory(LDAPConnectionFactory connection) {
        this.connectionFactory = connection;
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

    @Override
    public ProcessedClaimCollection retrieveClaimValues(ClaimCollection claims,
            ClaimsParameters parameters) {

        Principal principal = parameters.getPrincipal();

        String user = AttributeMapLoader.getUser(principal);
        if (user == null) {
            LOGGER.info(
                    "Could not determine user name, possible authentication error. Returning no claims.");
            return new ProcessedClaimCollection();
        }

        ProcessedClaimCollection claimsColl = new ProcessedClaimCollection();
        Connection connection = null;
        try {
            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectclass", this.getObjectClass()))
                    .and(new EqualsFilter(this.getUserNameAttribute(), user));

            List<String> searchAttributeList = new ArrayList<String>();
            for (Claim claim : claims) {
                if (getClaimsLdapAttributeMapping().keySet()
                        .contains(claim.getClaimType()
                                .toString())) {
                    searchAttributeList.add(getClaimsLdapAttributeMapping().get(claim.getClaimType()
                            .toString()));
                } else {
                    LOGGER.debug("Unsupported claim: {}", claim.getClaimType());
                }
            }

            String[] searchAttributes = null;
            searchAttributes = searchAttributeList.toArray(new String[searchAttributeList.size()]);

            connection = connectionFactory.getConnection();
            if (connection != null) {
                BindResult bindResult = connection.bind(bindUserDN,
                        bindUserCredentials.toCharArray());
                if (bindResult.isSuccess()) {
                    String baseDN = AttributeMapLoader.getBaseDN(principal, getUserBaseDN());
                    LOGGER.trace("Executing ldap search with base dn of {} and filter of {}",
                            baseDN,
                            filter.toString());

                    ConnectionEntryReader entryReader = connection.search(baseDN,
                            SearchScope.WHOLE_SUBTREE,
                            filter.toString(),
                            searchAttributes);

                    SearchResultEntry entry;
                    while (entryReader.hasNext()) {
                        entry = entryReader.readEntry();
                        for (Claim claim : claims) {
                            URI claimType = claim.getClaimType();
                            String ldapAttribute =
                                    getClaimsLdapAttributeMapping().get(claimType.toString());
                            Attribute attr = entry.getAttribute(ldapAttribute);
                            if (attr == null) {
                                LOGGER.trace("Claim '{}' is null", claim.getClaimType());
                            } else {
                                ProcessedClaim c = new ProcessedClaim();
                                c.setClaimType(claimType);
                                c.setPrincipal(principal);

                                for (ByteString value : attr) {
                                    String itemValue = value.toString();
                                    if (this.isX500FilterEnabled()) {
                                        try {
                                            X500Principal x500p = new X500Principal(itemValue);
                                            itemValue = x500p.getName();
                                            int index = itemValue.indexOf('=');
                                            itemValue = itemValue.substring(index + 1,
                                                    itemValue.indexOf(',', index));
                                        } catch (Exception ex) {
                                            // Ignore, not X500 compliant thus use the whole
                                            // string as the value
                                            LOGGER.debug("Not X500 compliant", ex);
                                        }
                                    }
                                    c.addValue(itemValue);
                                }

                                claimsColl.add(c);
                            }
                        }

                    }
                } else {
                    LOGGER.info("LDAP Connection failed.");
                }

            }
        } catch (LdapException e) {
            LOGGER.info(
                    "Cannot connect to server, therefore unable to set user attributes. Set log level for \"ddf.security.sts.claimsHandler\" to DEBUG for more information");
            LOGGER.debug("Cannot connect to server, therefore unable to set user attributes.", e);
        } catch (SearchResultReferenceIOException e) {
            LOGGER.info("Unable to set user attributes. Set log level for \"ddf.security.sts.claimsHandler\" to DEBUG for more information");
            LOGGER.debug("Unable to set user attributes.", e);
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
