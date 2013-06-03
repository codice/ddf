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
package ddf.security.service.impl;


import ddf.security.assertion.SecurityAssertion;
import ddf.security.expansion.Expansion;
import ddf.security.permission.KeyValuePermission;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Abstraction class used to perform authorization for a realm. This class
 * contains generic methods that can be used to parse out the credentials from
 * an incoming security token. It also handles caching tokens for later use.
 */
public abstract class AbstractAuthorizingRealm extends AuthorizingRealm
{

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAuthorizingRealm.class);

    private static final String SAML_ROLE = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

    private Expansion expansionService;

    /**
     * Takes the security attributes about the subject of the incoming security token and builds
     * sets of permissions and roles for use in further checking.
     * @param principalCollection  holds the security assertions for the primary principal of this request
     * @return  a new collection of permissions and roles corresponding to the security assertions
     * @throws AuthorizationException if there are no security assertions associated with this principal collection
     *          or if the token cannot be processed successfully.
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo( PrincipalCollection principalCollection )
    {
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        LOGGER.debug("Retrieving authorizationinfo for {}", principalCollection.getPrimaryPrincipal());
        SecurityAssertion assertion = principalCollection.oneByType(SecurityAssertion.class);
        if (assertion == null)
        {
            String msg = "No assertion found, cannot retrieve authorization info.";
            LOGGER.warn(msg);
            throw new AuthorizationException(msg);
        }
        try
        {
            AssertionWrapper wrapper = new AssertionWrapper(assertion.getSecurityToken().getToken());
            List<AttributeStatement> attributeStatements = wrapper.getSaml2().getAttributeStatements();
            List<Attribute> attributes;
            Set<Permission> permissions = new HashSet<Permission>();
            Set<String> roles = new HashSet<String>();
            Set<String> attributeSet;
            KeyValuePermission curPermission;
            for ( AttributeStatement curStatement : attributeStatements )
            {
                attributes = curStatement.getAttributes();

                for ( Attribute curAttribute : attributes )
                {
                    attributeSet = expandAttributes(curAttribute);
                    curPermission = new KeyValuePermission(curAttribute.getName());
                    if (attributeSet != null)
                    {
                        for (String attr : attributeSet)
                        {
                            curPermission.addValue(attr);
                            if (SAML_ROLE.equals(curAttribute.getName()))
                            {
                                LOGGER.debug("Adding role to authorization info: {}", attr);
                                roles.add(attr);
                            }
                        }
                    }
                    LOGGER.debug("Adding permission: {}", curPermission.toString());
                    permissions.add(curPermission);
                }
            }
            info.setObjectPermissions(permissions);
            info.setRoles(roles);
        }
        catch (WSSecurityException e)
        {
            String msg = "Error Processing Token.";
            LOGGER.warn(msg);
            throw new AuthorizationException(msg, e);
        }

        return info;
    }

    private Set<String> expandAttributes(Attribute attribute)
    {
        Set<String> attributeSet = new HashSet<String>();
        String attributeName = attribute.getName();
        for (XMLObject curValue : attribute.getAttributeValues())
        {
            if (curValue instanceof XSString)
            {
                attributeSet.add(((XSString) curValue).getValue());
            } else
            {
                LOGGER.info("Unexpected attribute type (non-string) for attribute named {} - ignored", attributeName);
            }
        }
        if (expansionService != null)
        {
            LOGGER.debug("Expanding attributes for {} - original values: {}", attributeName, attributeSet);
            attributeSet = expansionService.expand(attributeName, attributeSet);
        }
        LOGGER.debug("Expanded attributes for {} - values: {}", attributeName, attributeSet);
        return attributeSet;
    }

    public void setExpansionService(Expansion expansionService)
    {
        this.expansionService = expansionService;
    }
}
