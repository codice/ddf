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
package ddf.security.sts;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.sts.claims.Claim;
import org.apache.cxf.sts.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.RequestClaimCollection;
import org.apache.ws.security.WSConstants;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

/**
 * The {@link SecurityAttributesClaimsHandler} is called by the ClaimsManager to handle the
 * securityAttributeClaimType. This class will retrieve the security attributes from LDAP for a
 * specified user. The Claim Type is configurable.
 * 
 */
public class SecurityAttributesClaimsHandler implements ClaimsHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(SecurityAttributesClaimsHandler.class);

    public static final String DEFAULT_SECURITY_CLAIM_TYPE = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/uid";

    private static final String ATTRIBUTE_DELIMITER = ", ";

    private static final String EQUALS_DELIMITER = "=";

    private String securityAttributeClaimType = DEFAULT_SECURITY_CLAIM_TYPE;

    private String attributeMapping;

    private String userBaseDn;

    private Map<String, String> claimsLdapAttributeMapping;

    private String objectClassName;

    private String uidAttribute;

    private LdapTemplate ldapTemplate;

    public LdapTemplate getLdapTemplate() {
        return ldapTemplate;
    }

    public void setLdapTemplate(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    public String getObjectClassName() {
        return objectClassName;
    }

    public void setObjectClassName(String objectClassName) {
        this.objectClassName = objectClassName;
    }

    public String getUidAttribute() {
        return uidAttribute;
    }

    public void setUidAttribute(String uidAttribute) {
        this.uidAttribute = uidAttribute;
    }

    public Map<String, String> getClaimsLdapAttributeMapping() {
        return claimsLdapAttributeMapping;
    }

    public String getUserBaseDn() {
        return userBaseDn;
    }

    public void setUserBaseDn(String userBaseDn) {
        this.userBaseDn = userBaseDn;
    }

    public String getAttributeMapping() {
        return attributeMapping;
    }

    public void setAttributeMapping(String attributesToMap) {
        if (attributesToMap != null && !attributesToMap.equals(this.attributeMapping)) {
            setClaimsLdapAttributeMapping(buildLdapClaimsMap(attributesToMap));
        }
        this.attributeMapping = attributesToMap;
    }

    private Map<String, String> buildLdapClaimsMap(String attributesToMap) {
        // Remove first and last character since they are "[" and "]"
        String cleanedAttributesToMap = attributesToMap.substring(1, attributesToMap.length() - 1);
        String[] attributes = cleanedAttributesToMap.split(ATTRIBUTE_DELIMITER);
        Map<String, String> map = new HashMap<String, String>();
        for (String attribute : attributes) {
            String[] attrSplit = attribute.split(EQUALS_DELIMITER);
            map.put(attrSplit[0], attrSplit[1]);
        }
        return map;
    }

    private void setClaimsLdapAttributeMapping(Map<String, String> mapping) {
        this.claimsLdapAttributeMapping = mapping;

    }

    public String getSecurityAttributeClaimType() {
        return securityAttributeClaimType;
    }

    public void setSecurityAttributeClaimType(String securityAttributeClaimType) {
        this.securityAttributeClaimType = securityAttributeClaimType;
    }

    @Override
    public List<URI> getSupportedClaimTypes() {
        List<URI> uriList = new ArrayList<URI>();
        uriList.add(getSecurityAttributeURI());

        return uriList;
    }

    private URI getSecurityAttributeURI() {
        URI uri = null;
        try {
            uri = new URI(securityAttributeClaimType);
        } catch (URISyntaxException e) {
            LOGGER.warn("Unable to add securityAttributes claim type.", e);
        }
        return uri;
    }

    // Builds the Security Attributes into an array.
    private String[] buildAttributes() {
        // Get the attribute values from the map.
        String[] attributes = new String[claimsLdapAttributeMapping.size()];
        int index = 0;
        for (Entry<String, String> entry : claimsLdapAttributeMapping.entrySet()) {
            attributes[index] = entry.getValue();
            index++;
        }
        return attributes;
    }

    private static class DdfAttributesMapper implements AttributesMapper {

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

    @Override
    public ClaimCollection retrieveClaimValues(RequestClaimCollection claims,
            ClaimsParameters parameters) {
        String[] attributes = buildAttributes();
        ClaimCollection claimsColl = new ClaimCollection();
        try {

            String user = getUserFromClaimsParameters(parameters);

            if (user == null) {
                LOGGER.warn("User must not be null");
                return claimsColl;
            } else {
                LOGGER.trace("Retrieve securityAttributes claims for user {}", user);
            }

            AndFilter filter = buildLdapFilter(user);

            AttributesMapper mapper = new DdfAttributesMapper();

            List<?> results = ldapTemplate.search(userBaseDn, filter.toString(),
                    SearchControls.SUBTREE_SCOPE, attributes, mapper);

            for (Object result : results) {
                Map<String, Attribute> ldapAttributes = null;
                ldapAttributes = CastUtils.cast((Map<?, ?>) result);

                // Get each of the mapped Attributes from the result.
                for (Entry<String, String> claimAttr : claimsLdapAttributeMapping.entrySet()) {
                    Attribute attr = ldapAttributes.get(claimAttr.getValue());
                    if (attr == null) {
                        LOGGER.trace("Claim '{}' is null", claimAttr.getKey());
                    } else {
                        Claim c = buildClaim(parameters, claimAttr, attr);
                        claimsColl.add(c);
                    }
                }
            }
        } catch (URISyntaxException e) {
            LOGGER.error("Unable to set role claims.", e);
        }
        return claimsColl;
    }

    /*
     * Helper method to build a Claim.
     */
    private Claim buildClaim(ClaimsParameters parameters, Entry<String, String> claimAttr,
            Attribute attr) throws URISyntaxException {
        Claim c = new Claim();
        c.setClaimType(new URI(claimAttr.getKey()));
        c.setPrincipal(parameters.getPrincipal());

        StringBuilder claimValue = new StringBuilder();
        try {
            NamingEnumeration<?> list = (NamingEnumeration<?>) attr.getAll();
            while (list.hasMore()) {
                Object obj = list.next();
                if (!(obj instanceof String)) {
                    LOGGER.warn("LDAP attribute '{}' has an unsupported value type", claimAttr.getValue());
                    break;
                }
                claimValue.append((String) obj);
                if (list.hasMore()) {
                    claimValue.append(ATTRIBUTE_DELIMITER);
                }
            }
        } catch (NamingException ex) {
            LOGGER.warn("Failed to read value of LDAP attribute '{}'", claimAttr.getValue());
        }

        c.setValue(claimValue.toString());
        return c;
    }

    /**
     * @param parameters
     * @param claimsColl
     * @param principal
     * @return
     */
    private String getUserFromClaimsParameters(ClaimsParameters parameters) {
        Principal principal = parameters.getPrincipal();
        String user = null;
        if (parameters.getAdditionalProperties() != null
                && parameters.getAdditionalProperties().containsKey(WSConstants.USERNAME_LN)) {
            user = (String) parameters.getAdditionalProperties().get(WSConstants.USERNAME_LN);
        } else {
            if (principal instanceof KerberosPrincipal) {
                KerberosPrincipal kp = (KerberosPrincipal) principal;
                StringTokenizer st = new StringTokenizer(kp.getName(), "@");
                user = st.nextToken();
            } else if (principal instanceof X500Principal) {
                // 1.2.840.113549.1.9.1=#160d69346365406c6d636f2e636f6d,CN=client,OU=I4CE,O=Lockheed
                // Martin,L=Goodyear,ST=Arizona,C=US
                X500Principal xp = (X500Principal) principal;
                StringTokenizer st = new StringTokenizer(xp.getName(), ",");
                @SuppressWarnings("unused")
                String syntaxAndUniqueId = st.nextToken();
                String cn = st.nextToken();
                // String ou = st.nextToken();
                // String o = st.nextToken();
                // String loc = st.nextToken();
                // String state = st.nextToken();
                // String country = st.nextToken();

                StringTokenizer userTokenizer = new StringTokenizer(cn, "=");
                // String cnKey = userTokenizer.nextToken();
                user = userTokenizer.nextToken();
            } else if (principal != null) {
                user = principal.getName();
            } else {
                LOGGER.warn("Principal is null");
            }
        }

        return user;
    }

    /*
     * Method to determine the filter clause for LDAP. If an objectClassName is provided the query
     * will be built as "(&(objectclass=<ocName>)(uid=<username>))". If the objectClassName is not
     * provided the filter will be built as "(&(uid=<username>))". The uidAttribute is also
     * configurable for different LDAP implementations.
     */
    private AndFilter buildLdapFilter(String user) {
        AndFilter filter = new AndFilter();
        if (getObjectClassName() == null || getObjectClassName().isEmpty()) {
            filter.and(new EqualsFilter(getUidAttribute(), user));
        } else {
            filter.and(new EqualsFilter("objectClass", getObjectClassName())).and(
                    new EqualsFilter(getUidAttribute(), user));
        }
        return filter;
    }
}
