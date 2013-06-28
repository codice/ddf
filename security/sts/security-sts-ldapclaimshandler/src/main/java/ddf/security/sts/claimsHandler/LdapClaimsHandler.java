/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
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
import java.util.StringTokenizer;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.sts.claims.Claim;
import org.apache.cxf.sts.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.RequestClaim;
import org.apache.cxf.sts.claims.RequestClaimCollection;
import org.apache.log4j.Logger;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;

public class LdapClaimsHandler extends org.apache.cxf.sts.claims.LdapClaimsHandler
{
    private static final Logger LOGGER = Logger.getLogger(LdapClaimsHandler.class);

    private String attributeMapping;

    private String propertyFileLocation;
    
    private LdapTemplate ldap;
    
    private String userBaseDn;
    
    public LdapClaimsHandler()
    {
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
    
    public String getPropertyFileLocation()
    {
        return propertyFileLocation;
    }
    
    public void setPropertyFileLocation(String propertyFileLocation)
    {
        if(propertyFileLocation != null && !propertyFileLocation.isEmpty() && !propertyFileLocation.equals(this.propertyFileLocation))
        {
            setClaimsLdapAttributeMapping(AttributeMapLoader.buildClaimsMapFile(propertyFileLocation));
        }
        this.propertyFileLocation = propertyFileLocation;
    }

    public String getAttributeMapping() {
        return attributeMapping;
    }

    public void setAttributeMapping(String attributesToMap) {
        if (attributesToMap != null
                && !attributesToMap.isEmpty() && !attributesToMap.equals(this.attributeMapping)) {
            setClaimsLdapAttributeMapping(AttributeMapLoader.buildClaimsMap(attributesToMap));
        }
        this.attributeMapping = attributesToMap;
    }
    
	@Override
	public ClaimCollection retrieveClaimValues(RequestClaimCollection claims,
			ClaimsParameters parameters) {

		Principal principal = parameters.getPrincipal();

		String user = null;
		if (principal instanceof KerberosPrincipal) {
			KerberosPrincipal kp = (KerberosPrincipal) principal;
			StringTokenizer st = new StringTokenizer(kp.getName(), "@");
			user = st.nextToken();
		} else if (principal instanceof X500Principal) {
			X500Principal x500p = (X500Principal) principal;
            StringTokenizer st = new StringTokenizer(x500p.getName(), ",");
            while(st.hasMoreElements())
            {
                //token is in the format:
                //syntaxAndUniqueId
                //cn
                //ou
                //o
                //loc
                //state
                //country
                String[] strArr = st.nextToken().split("=");
                if(strArr.length > 1 && strArr[0].equalsIgnoreCase("cn"))
                {
                    user = strArr[1];
                    break;
                }
            }
		} else if (principal != null) {
			user = principal.getName();
		} else {
			LOGGER.info("Principal is null");
			return new ClaimCollection();
		}

		if (user == null) {
			LOGGER.warn("User must not be null");
			return new ClaimCollection();
		} else {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Retrieve claims for user " + user);
			}
		}

		AndFilter filter = new AndFilter();
		filter.and(new EqualsFilter("objectclass", this.getObjectClass())).and(
				new EqualsFilter(this.getUserNameAttribute(), user));

		List<String> searchAttributeList = new ArrayList<String>();
		for (RequestClaim claim : claims) {
			if (getClaimsLdapAttributeMapping().keySet().contains(
					claim.getClaimType().toString())) {
				searchAttributeList.add(getClaimsLdapAttributeMapping().get(
						claim.getClaimType().toString()));
			} else {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Unsupported claim: " + claim.getClaimType());
				}
			}
		}

		String[] searchAttributes = null;
		searchAttributes = searchAttributeList
				.toArray(new String[searchAttributeList.size()]);

		AttributesMapper mapper = new AttributesMapper() {
			public Object mapFromAttributes(Attributes attrs)
					throws NamingException {
				Map<String, Attribute> map = new HashMap<String, Attribute>();
				NamingEnumeration<? extends Attribute> attrEnum = attrs
						.getAll();
				while (attrEnum.hasMore()) {
					Attribute att = attrEnum.next();
					map.put(att.getID(), att);
				}
				return map;
			}
		};

		List<?> result = ldap.search((this.userBaseDn == null) ? ""
				: this.userBaseDn, filter.toString(),
				SearchControls.SUBTREE_SCOPE, searchAttributes, mapper);

		Map<String, Attribute> ldapAttributes = null;
		if (result != null && result.size() > 0) {
			ldapAttributes = CastUtils.cast((Map<?, ?>) result.get(0));
		} else {
		    LOGGER.debug("No results returned from LDAP search for user [" + user + "].  Returning empty claims collection.");
		    return new ClaimCollection();
		}

		ClaimCollection claimsColl = new ClaimCollection();

		for (RequestClaim claim : claims) {
			URI claimType = claim.getClaimType();
			String ldapAttribute = getClaimsLdapAttributeMapping().get(
					claimType.toString());
			Attribute attr = ldapAttributes.get(ldapAttribute);
			if (attr == null) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Claim '" + claim.getClaimType() + "' is null");
				}
			} else {
				Claim c = new Claim();
				c.setClaimType(claimType);
				c.setPrincipal(principal);

				try {
					NamingEnumeration<?> list = (NamingEnumeration<?>) attr
							.getAll();
					while (list.hasMore()) {
						Object obj = list.next();
						if (!(obj instanceof String)) {
							LOGGER.warn("LDAP attribute '" + ldapAttribute
									+ "' has got an unsupported value type");
							break;
						}
						String itemValue = (String) obj;
						if (this.isX500FilterEnabled()) {
							try {
								X500Principal x500p = new X500Principal(
										itemValue);
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
					LOGGER.warn("Failed to read value of LDAP attribute '"
							+ ldapAttribute + "'");
				}

				// c.setIssuer(issuer);
				// c.setOriginalIssuer(originalIssuer);
				// c.setNamespace(namespace);
				claimsColl.add(c);
			}
		}

		return claimsColl;
	}
}
