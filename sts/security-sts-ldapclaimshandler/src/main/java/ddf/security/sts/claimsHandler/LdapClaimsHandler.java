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
import javax.security.auth.x500.X500Principal;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.sts.claims.Claim;
import org.apache.cxf.sts.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.RequestClaim;
import org.apache.cxf.sts.claims.RequestClaimCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;

public class LdapClaimsHandler extends org.apache.cxf.sts.claims.LdapClaimsHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapClaimsHandler.class);

    private String propertyFileLocation;

    private LdapTemplate ldap;

    private String userBaseDn;

    public LdapClaimsHandler() {
        super();
    }

    public void setLdapTemplate(LdapTemplate ldapTemplate) {
        this.ldap = ldapTemplate;
    }

    public LdapTemplate getLdapTemplate() {
        return ldap;
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

            LOGGER.trace("Executing ldap search with base dn of {} and filter of {}", this.userBaseDn, filter.toString());
            List<?> result = ldap.search((this.userBaseDn == null) ? "" : this.userBaseDn,
                    filter.toString(), SearchControls.SUBTREE_SCOPE, searchAttributes, mapper);

            Map<String, Attribute> ldapAttributes = null;
            if (result != null && result.size() > 0) {
                ldapAttributes = CastUtils.cast((Map<?, ?>) result.get(0));
            } else {
                LOGGER.debug(
                        "No results returned from LDAP search for user [{}].  Returning empty claims collection.",
                        user);
                return new ClaimCollection();
            }


            for (RequestClaim claim : claims) {
                URI claimType = claim.getClaimType();
                String ldapAttribute = getClaimsLdapAttributeMapping().get(claimType.toString());
                Attribute attr = ldapAttributes.get(ldapAttribute);
                if (attr == null) {
                    LOGGER.trace("Claim '{}' is null", claim.getClaimType());
                } else {
                    Claim c = new Claim();
                    c.setClaimType(claimType);
                    c.setPrincipal(principal);

                    try {
                        NamingEnumeration<?> list = (NamingEnumeration<?>) attr.getAll();
                        while (list.hasMore()) {
                            Object obj = list.next();
                            if (!(obj instanceof String)) {
                                LOGGER.warn("LDAP attribute '{}' has got an unsupported value type",
                                        ldapAttribute);
                                break;
                            }
                            String itemValue = (String) obj;
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
                    } catch (NamingException ex) {
                        LOGGER.warn("Failed to read value of LDAP attribute '{}'", ldapAttribute);
                    }

                    // c.setIssuer(issuer);
                    // c.setOriginalIssuer(originalIssuer);
                    // c.setNamespace(namespace);
                    claimsColl.add(c);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unable to set role claims.", e);
        }
        return claimsColl;
    }
}
