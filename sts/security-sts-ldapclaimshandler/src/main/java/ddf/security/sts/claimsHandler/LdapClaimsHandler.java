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

import org.apache.cxf.sts.claims.Claim;
import org.apache.cxf.sts.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.RequestClaim;
import org.apache.cxf.sts.claims.RequestClaimCollection;
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

import javax.security.auth.x500.X500Principal;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LdapClaimsHandler extends org.apache.cxf.sts.claims.LdapClaimsHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapClaimsHandler.class);

    private String propertyFileLocation;

    private LdapConnection connection;

    private String userBaseDn;

    public LdapClaimsHandler() {
        super();
    }

    public void setLdapConnection(LdapConnection connection) {
        this.connection = connection;
    }

    public LdapConnection getLdapConnection() {
        return connection;
    }

    public void setUserBaseDn(String userBaseDN) {
        this.userBaseDn = userBaseDN;
    }

    public String getUserBaseDn() {
        return userBaseDn;
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

    @Override
    public ClaimCollection retrieveClaimValues(RequestClaimCollection claims,
            ClaimsParameters parameters) {

        Principal principal = parameters.getPrincipal();

        String user = AttributeMapLoader.getUser(principal);
        if (user == null) {
            LOGGER.warn("Could not determine user name, possible authentication error. Returning no claims.");
            return new ClaimCollection();
        }

        ClaimCollection claimsColl = new ClaimCollection();
        try {

            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectclass", this.getObjectClass())).and(
                    new EqualsFilter(this.getUserNameAttribute(), user));

            List<String> searchAttributeList = new ArrayList<String>();
            for (RequestClaim claim : claims) {
                if (getClaimsLdapAttributeMapping().keySet().contains(claim.getClaimType().toString())) {
                    searchAttributeList.add(getClaimsLdapAttributeMapping().get(
                            claim.getClaimType().toString()));
                } else {
                    LOGGER.debug("Unsupported claim: {}", claim.getClaimType());
                }
            }

            String[] searchAttributes = null;
            searchAttributes = searchAttributeList.toArray(new String[searchAttributeList.size()]);

            LOGGER.trace("Executing ldap search with base dn of {} and filter of {}", this.userBaseDn, filter.toString());
            connection.bind();
            EntryCursor entryCursor = connection.search((this.userBaseDn == null) ? "" : this.userBaseDn,
                    filter.toString(),
                    SearchScope.SUBTREE, searchAttributes);

            Entry entry;
            while(entryCursor.next()) {
                entry = entryCursor.get();
                for (RequestClaim claim : claims) {
                    URI claimType = claim.getClaimType();
                    String ldapAttribute = getClaimsLdapAttributeMapping().get(claimType.toString());
                    Attribute attr = entry.get(ldapAttribute);
                    if (attr == null) {
                        LOGGER.trace("Claim '{}' is null", claim.getClaimType());
                    } else {
                        Claim c = new Claim();
                        c.setClaimType(claimType);
                        c.setPrincipal(principal);

                        Iterator<Value<?>> valueIterator = attr.iterator();
                        while(valueIterator.hasNext()) {
                            Value<?> value = valueIterator.next();

                            Object objValue = value.getValue();
                            if (!(objValue instanceof String)) {
                                LOGGER.warn("LDAP attribute '{}' has got an unsupported value type",
                                        ldapAttribute);
                                break;
                            }
                            String itemValue = (String) objValue;
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
                                }
                            }
                            c.addValue(itemValue);
                        }

                        claimsColl.add(c);
                    }
                }

            }
        } catch (InvalidConnectionException e) {
            LOGGER.warn("Cannot connect to server, therefore unable to set role claims.");
        } catch (Exception e) {
            LOGGER.error("Unable to set role claims.", e);
        } finally {
            try {
                connection.unBind();
            } catch (LdapException ignore) {
            }
        }
        return claimsColl;
    }
}
