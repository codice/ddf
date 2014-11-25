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
package ddf.security.pdp.realm;

import ddf.security.common.audit.SecurityLogger;
import ddf.security.permission.ActionPermission;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.permission.MatchOneCollectionPermission;
import ddf.security.service.impl.AbstractAuthorizingRealm;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.PermissionResolver;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This simple Authz {@link ddf.security.service.impl.AbstractAuthorizingRealm} provides the ability
 * to check permissions without making calls out to an external PDP. {@link Permission} objects are
 * checked against each other to ensure that the subject permissions imply the resource permissions.
 * 
 * @author tustisos
 */
public class SimpleAuthzRealm extends AbstractAuthorizingRealm {
    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(SimpleAuthzRealm.class));

    private static final String ACCESS_DENIED_MSG = "User not authorized";

    private static final String PERMISSION_FINISH_1_MSG = "Finished permission check for user [";

    private static final String PERMISSION_FINISH_2_MSG = "]. Result is that permission [";

    /**
     * Identifies the key used to retrieve a List of Strings that represent the mapping between
     * metacard and user attributes. The mappings defined in this List of Strings are used by the
     * "match all" evaluation to determine if this user should be authorized to access this data.
     * <p>
     * Each string is of the format: <code>metacardAttribute=userAttribute</code> where
     * metacardAttribute is the name of an attribute in the metacard and userAttribute is the name
     * of the corresponding attribute in the user credentials. It is the value of each of these
     * attributes that will be evaluated against each other when determining if authorization should
     * be allowed.
     */
    public static final String MATCH_ALL_MAPPINGS = "matchAllMappings";

    /**
     * Identifies the key used to retrieve a List of Strings that represent the mapping between
     * metacard and user attributes. The mappings defined in this List of Strings are used by the
     * "match one" evaluation to determine if this user should be authorized to access this data.
     * <p>
     * Each string is of the format: <code>metacardAttribute=userAttribute</code> where
     * metacardAttribute is the name of an attribute in the metacard and userAttribute is the name
     * of the corresponding attribute in the user credentials. It is the value of each of these
     * attributes that will be evaluated against each other when determining if authorization should
     * be allowed.
     */
    public static final String MATCH_ONE_MAPPINGS = "matchOneMappings";

    /**
     * Identifies the key used to retrieve a List of Strings that represent roles that will be
     * allowed to perform restricted actions. Each string defines the role name.
     */
    public static final String ACCESS_ROLE_LIST = "accessRoleList";

    /**
     * Identifies the key used to retrieve a List of Strings that represent actions that are open
     * for any role to access. Each string is the action corresponding to the SOAP action presented
     * in the SOAP request. If the specified string is contained in the SOAP action string, then
     * access is automatically allowed for this operation.
     */
    public static final String OPEN_ACCESS_ACTION_LIST = "openAccessActionList";

    private List<String> accessRoleList;

    private List<String> openAccessActionList;

    private HashMap<String, String> matchAllMap = new HashMap<String, String>();

    private HashMap<String, String> matchOneMap = new HashMap<String, String>();

    // this realm is for authorization only
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
        throws AuthenticationException {
        return null;
    }

    // This method is for testing purposes only, Mockito was not able to mock the
    // getAuthorizationInfo method
    AuthorizationInfo info = null;

    public void setAuthorizationInfo(AuthorizationInfo info) {
        this.info = info;
    }

    /**
     * Returns an account's authorization-specific information for the specified {@code principals},
     * or {@code null} if no account could be found. The resulting {@code AuthorizationInfo} object
     * is used by the other method implementations in this class to automatically perform access
     * control checks for the corresponding {@code Subject}.
     * <p/>
     * This implementation obtains the actual {@code AuthorizationInfo} object from the subclass's
     * implementation of
     * {@link #doGetAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection)
     * doGetAuthorizationInfo}, and then caches it for efficient reuse if caching is enabled (see
     * below).
     * <p/>
     * Invocations of this method should be thought of as completely orthogonal to acquiring
     * {@link #getAuthenticationInfo(org.apache.shiro.authc.AuthenticationToken) authenticationInfo}
     * , since either could occur in any order.
     * <p/>
     * For example, in &quot;Remember Me&quot; scenarios, the user identity is remembered (and
     * assumed) for their current session and an authentication attempt during that session might
     * never occur. But because their identity would be remembered, that is sufficient enough
     * information to call this method to execute any necessary authorization checks. For this
     * reason, authentication and authorization should be loosely coupled and not depend on each
     * other.
     * <h3>Caching</h3>
     * The {@code AuthorizationInfo} values returned from this method are cached for efficient reuse
     * if caching is enabled. Caching is enabled automatically when an
     * {@link #setAuthorizationCache authorizationCache} instance has been explicitly configured, or
     * if a {@link #setCacheManager cacheManager} has been configured, which will be used to lazily
     * create the {@code authorizationCache} as needed.
     * <p/>
     * If caching is enabled, the authorization cache will be checked first and if found, will
     * return the cached {@code AuthorizationInfo} immediately. If caching is disabled, or there is
     * a cache miss, the authorization info will be looked up from the underlying data store via the
     * {@link #doGetAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection)} method, which
     * must be implemented by subclasses.
     * <h4>Changed Data</h4>
     * If caching is enabled and if any authorization data for an account is changed at runtime,
     * such as adding or removing roles and/or permissions, the subclass implementation should clear
     * the cached AuthorizationInfo for that account via the
     * {@link #clearCachedAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection)
     * clearCachedAuthorizationInfo} method. This ensures that the next call to
     * {@code getAuthorizationInfo(PrincipalCollection)} will acquire the account's fresh
     * authorization data, where it will then be cached for efficient reuse. This ensures that stale
     * authorization data will not be reused.
     * 
     * @param principals
     *            the corresponding Subject's identifying principals with which to look up the
     *            Subject's {@code AuthorizationInfo}.
     * @return the authorization information for the account associated with the specified
     *         {@code principals}, or {@code null} if no account could be found.
     */
    @Override
    public AuthorizationInfo getAuthorizationInfo(PrincipalCollection principals) {
        if (info != null) {
            return info;
        }
        return super.getAuthorizationInfo(principals);
    }

    /**
     * Returns <tt>true</tt> if the corresponding subject/user is permitted to perform an action or
     * access a resource summarized by the specified permission.
     * 
     * <p>
     * More specifically, this method determines if any <tt>Permission</tt>s associated with the
     * subject {@link Permission#implies(Permission) imply} the specified permission.
     * 
     * @param subjectPrincipal
     *            the application-specific subject/user identifier.
     * @param permission
     *            the permission that is being checked.
     * @return true if the corresponding Subject/user is permitted, false otherwise.
     */
    @Override
    public boolean isPermitted(PrincipalCollection subjectPrincipal, Permission permission) {
        return isPermitted(subjectPrincipal, Collections.singletonList(permission))[0];
    }

    /**
     * Checks if the corresponding Subject/user implies the given Permissions and returns a boolean
     * array indicating which permissions are implied.
     * 
     * <p>
     * More specifically, this method should determine if each <tt>Permission</tt> in the array is
     * {@link Permission#implies(Permission) implied} by permissions already associated with the
     * subject.
     * 
     * <p>
     * This is primarily a performance-enhancing method to help reduce the number of
     * {@link #isPermitted} invocations over the wire in client/server systems.
     * 
     * @param subjectPrincipal
     *            the application-specific subject/user identifier.
     * @param permissions
     *            the permissions that are being checked.
     * @return an array of booleans whose indices correspond to the index of the permissions in the
     *         given list. A true value at an index indicates the user is permitted for for the
     *         associated <tt>Permission</tt> object in the list. A false value at an index
     *         indicates otherwise.
     */
    @Override
    public boolean[] isPermitted(PrincipalCollection subjectPrincipal, List<Permission> permissions) {    
        boolean[] results = new boolean[permissions.size()];
        AuthorizationInfo info = getAuthorizationInfo(subjectPrincipal);
        int i = 0;
        for (Permission permission : permissions) {
            results[i++] = isPermitted(subjectPrincipal, permission, info);
        }

        return results;
    }

    /**
     * Checks if the corresponding Subject/user contained within the AuthorizationInfo object
     * implies the given Permission.
     * 
     * @param permission
     *            the permission being checked.
     * @param info
     *            the application-specific subject/user identifier.
     * @return true if the user is permitted
     */
    private boolean isPermitted(PrincipalCollection subjectPrincipal, Permission permission,
            AuthorizationInfo info) {
        Collection<Permission> perms = getPermissions(info);
        String curUser = "<user>";
        if (subjectPrincipal != null && subjectPrincipal.getPrimaryPrincipal() != null) {
            curUser = subjectPrincipal.getPrimaryPrincipal().toString();
        }
        SecurityLogger.logInfo("Starting permissions check for user [" + curUser + "]");
        if (perms != null && !perms.isEmpty()) {
            if (permission instanceof KeyValuePermission) {
                permission = new KeyValueCollectionPermission((KeyValuePermission) permission);
            }
            if (permission != null && permission instanceof KeyValueCollectionPermission) {
                List<KeyValuePermission> metacardPermissions = ((KeyValueCollectionPermission) permission)
                        .getKeyValuePermissionList();
                List<KeyValuePermission> matchOnePermissions = new ArrayList<KeyValuePermission>();
                List<KeyValuePermission> matchAllPermissions = new ArrayList<KeyValuePermission>();

                for (KeyValuePermission metacardPermission : metacardPermissions) {
                    String metacardKey = metacardPermission.getKey();
                    // user specificied this metacard key in the match all list - remap key
                    if (matchAllMap.containsKey(metacardKey)) {
                        SecurityLogger.logInfo("Mapping metacard key " + metacardKey + " to "
                                + matchAllMap.get(metacardKey));
                        KeyValuePermission kvp = new KeyValuePermission(
                                matchAllMap.get(metacardKey), metacardPermission.getValues());
                        matchAllPermissions.add(kvp);
                    }
                    // user specified this metacard key in the match one list - remap key
                    else if (matchOneMap.containsKey(metacardKey)) {
                        SecurityLogger.logInfo("Mapping metacard key " + metacardKey + " to "
                                + matchOneMap.get(metacardKey));
                        KeyValuePermission kvp = new KeyValuePermission(
                                matchOneMap.get(metacardKey), metacardPermission.getValues());
                        matchOnePermissions.add(kvp);
                    }
                    // this metacard key was not specified in either - default to match all with the
                    // same key value
                    else {
                        SecurityLogger.logInfo("Using original metacard key of " + metacardKey);
                        matchAllPermissions.add(metacardPermission);
                    }
                }

                KeyValueCollectionPermission matchAllCollection = new KeyValueCollectionPermission(
                        matchAllPermissions);
                KeyValueCollectionPermission matchOneCollection = new KeyValueCollectionPermission(
                        matchOnePermissions);
                CollectionPermission subjectAllCollection = new CollectionPermission(perms);
                MatchOneCollectionPermission subjectOneCollection = new MatchOneCollectionPermission(
                        perms);

                boolean matchAll = subjectAllCollection.implies(matchAllCollection);
                boolean matchOne = subjectOneCollection.implies(matchOneCollection);
                if (matchAll && matchOne) {
                    SecurityLogger.logInfo(PERMISSION_FINISH_1_MSG + curUser
                            + PERMISSION_FINISH_2_MSG + permission + "] is implied.");
                } else {
                    SecurityLogger.logInfo(PERMISSION_FINISH_1_MSG + curUser
                            + PERMISSION_FINISH_2_MSG + permission + "] is not implied.");
                }
                return (matchAll && matchOne);
            }

            for (Permission perm : perms) {
                if (permission instanceof ActionPermission
                        && isPermitted((ActionPermission) permission, info)) {
                    SecurityLogger.logInfo(PERMISSION_FINISH_1_MSG + curUser
                            + PERMISSION_FINISH_2_MSG + permission + "] is implied.");
                    return true;
                } else if (perm.implies(permission)) {
                    SecurityLogger.logInfo(PERMISSION_FINISH_1_MSG + curUser
                            + PERMISSION_FINISH_2_MSG + permission + "] is implied.");
                    return true;
                }
            }
        } else if (permission instanceof ActionPermission
                && isPermitted((ActionPermission) permission, info)) {
            SecurityLogger.logInfo(PERMISSION_FINISH_1_MSG + curUser + PERMISSION_FINISH_2_MSG
                    + permission + "] is implied.");
            return true;
        }
        SecurityLogger.logInfo(PERMISSION_FINISH_1_MSG + curUser + PERMISSION_FINISH_2_MSG
                + permission + "] is not implied.");
        return false;
    }

    private boolean isPermitted(ActionPermission actionPermission, AuthorizationInfo info) {
        String action = actionPermission.getAction();
        if (StringUtils.isNotEmpty(action)) {
            // check to see if the action they are trying to perform is an action anyone can do
            if (openAccessActionList != null) {
                for (String openAction : openAccessActionList) {
                    if (action.indexOf(openAction) != -1) {
                        SecurityLogger.logInfo("Action permission [" + actionPermission
                                + "] implied as an open action.");
                        return true;
                    }
                }
            }

            // it must be a restricted action, so check if the user has the correct role
            if (accessRoleList != null) {
                for (String accessRole : accessRoleList) {
                    
                    if (info.getRoles().contains(accessRole)) {
                        SecurityLogger.logInfo("User has access role " + accessRole);
                        return true;
                    }
                }
            }
        }
        SecurityLogger.logInfo("Action permission [" + actionPermission + "] not implied.");
        return false;
    }

    /**
     * Returns a {@link WildcardPermission} representing a {@link KeyValuePermission}
     * 
     * @param perm
     *            the permission to convert.
     * @return new equivalent permission
     */
    private WildcardPermission buildWildcardFromKeyValue(KeyValuePermission perm) {
        StringBuilder wildcardString = new StringBuilder();
        for (String value : perm.getValues()) {
            wildcardString.append(value);
            wildcardString.append(",");
        }
        WildcardPermission wildcardPermission = new WildcardPermission(wildcardString.toString()
                .substring(0, wildcardString.length() - 1));
        return wildcardPermission;
    }

    /**
     * Returns a collection of {@link Permission} objects that the {@link AuthorizationInfo} object
     * of a {@link ddf.security.Subject} is asserting.
     * 
     * @param info
     *            the application-specific subject/user identifier.
     * @return collection of Permissions.
     */
    private Collection<Permission> getPermissions(AuthorizationInfo info) {
        Set<Permission> permissions = new HashSet<Permission>();

        if (info != null) {
            Collection<Permission> perms = info.getObjectPermissions();
            if (!CollectionUtils.isEmpty(perms)) {
                permissions.addAll(perms);
            }
            perms = resolvePermissions(info.getStringPermissions());
            if (!CollectionUtils.isEmpty(perms)) {
                permissions.addAll(perms);
            }

            perms = resolveRolePermissions(info.getRoles());
            if (!CollectionUtils.isEmpty(perms)) {
                permissions.addAll(perms);
            }
        }

        return Collections.unmodifiableSet(permissions);
    }

    /**
     * Returns a collection of {@link Permission} objects that are built from the associated
     * collection of Strings.
     * 
     * @param stringPerms
     *            collection of Strings that represent permissions.
     * @return collection of Permissions
     */
    private Collection<Permission> resolvePermissions(Collection<String> stringPerms) {
        Collection<Permission> perms = Collections.emptySet();
        PermissionResolver resolver = getPermissionResolver();
        if (resolver != null && !CollectionUtils.isEmpty(stringPerms)) {
            perms = new LinkedHashSet<Permission>(stringPerms.size());
            for (String strPermission : stringPerms) {
                Permission permission = getPermissionResolver().resolvePermission(strPermission);
                perms.add(permission);
            }
        }
        return perms;
    }

    /**
     * Returns a collection of {@link Permission} objects that are built from the associated
     * collection of Strings that represent the roles that a user possesses.
     * 
     * @param roleNames
     *            user roles.
     * @return collection of Permissions
     */
    private Collection<Permission> resolveRolePermissions(Collection<String> roleNames) {
        Collection<Permission> perms = Collections.emptySet();
        RolePermissionResolver resolver = getRolePermissionResolver();
        if (resolver != null && !CollectionUtils.isEmpty(roleNames)) {
            perms = new LinkedHashSet<Permission>(roleNames.size());
            for (String roleName : roleNames) {
                Collection<Permission> resolved = resolver.resolvePermissionsInRole(roleName);
                if (!CollectionUtils.isEmpty(resolved)) {
                    perms.addAll(resolved);
                }
            }
        }
        return perms;
    }

    /**
     * Sets the list of roles that are allowed to execute privileged operations. Each string in the
     * list defines a specific role that users may be assigned.
     * 
     * @param accessRoleList
     *            List of String values that correspond to roles that are allowed to execute
     *            privileged operations.
     */
    public void setAccessRoleList(List<String> accessRoleList) {
        this.accessRoleList = accessRoleList;
    }

    /**
     * Takes in a comma-delimited string of privileged access roles.
     * 
     * @see SimpleAuthzRealm#setAccessRoleList(List)
     * @param commaStr
     */

    public void setAccessRoleList(String commaStr) {
        setAccessRoleList(convertToList(commaStr));
    }

    /**
     * Returns a comma-delimited string of privileged access roles.
     * @return comma delimited String of access roles
     */
    public String getAccessRoleList() {
        String accessRoleCsv = null;
        if (accessRoleList != null) {
            accessRoleCsv = StringUtils.join(accessRoleList, ",");
        }
        return accessRoleCsv;
    }
    
    /**
     * Sets the list of SOAP actions that are open for users in any role to access. Each string is
     * the action corresponding to the SOAP action presented in the SOAP request. If the specified
     * string is contained in the SOAP action string, then access is automatically allowed for this
     * operation.
     * 
     * @param openAccessActionList
     *            List of SOAP action strings that may be accessed by users in any role.
     */
    public void setOpenAccessActionList(List<String> openAccessActionList) {
        this.openAccessActionList = openAccessActionList;
    }

    /**
     * Takes in a comma-delimited string of open access roles.
     * 
     * @see SimpleAuthzRealm#setOpenAccessActionList(List)
     * @param Comma
     *            -delimited string of open access roles.
     */
    public void setOpenAccessActionList(String commaStr) {
        setOpenAccessActionList(convertToList(commaStr));
    }

    /**
     * Sets the mappings used by the "match all" evaluation to determine if this user should be
     * authorized to access requested data.
     * <p>
     * Each string is of the format: <code>metacardAttribute=userAttribute</code><br/>
     * where <code>metacardAttribute</code> is the name of an attribute in the metacard and
     * <code>userAttribute</code> is the name of the corresponding attribute in the user
     * credentials.<br/>
     * It is the values corresponding to each of these attributes that will be evaluated against
     * each other when determining if authorization should be allowed.
     * 
     * @param list
     *            List of Strings that define mappings between metadata attributes and user
     *            attributes
     */
    public void setMatchAllMappings(List<String> list) {
        String[] values;
        matchAllMap.clear();
        if (list != null) {
            for (String mapping : list) {
                values = mapping.split("=");
                if (values.length == 2) {
                    LOGGER.debug("Adding mapping: {} = {} to matchAllMap.", values[1].trim(),
                            values[0].trim());
                    matchAllMap.put(values[1].trim(), values[0].trim());
                } else {
                    LOGGER.warn(
                            "Match all mapping ignored: {} doesn't match expected format of metacardAttribute=userAttribute",
                            mapping);
                }
            }
        }
    }

    /**
     * Takes in a comma-delimited string of match all mappings.
     * 
     * @see SimpleAuthzRealm#setMatchAllMappings(List)
     * @param commaStr
     */
    public void setMatchAllMappings(String commaStr) {
        setMatchAllMappings(convertToList(commaStr));
    }

    /**
     * Sets the mappings used by the "match one" evaluation to determine if this user should be
     * authorized to access requested data.
     * <p>
     * Each string is of the format: <code>metacardAttribute=userAttribute</code><br/>
     * where <code>metacardAttribute</code> is the name of an attribute in the metacard and
     * <code>userAttribute</code> is the name of the corresponding attribute in the user
     * credentials.<br/>
     * It is the values corresponding to each of these attributes that will be evaluated against
     * each other when determining if authorization should be allowed.
     * 
     * @param list
     *            List of Strings that define mappings between metadata attributes and user
     *            attributes
     */
    public void setMatchOneMappings(List<String> list) {
        String[] values;
        matchOneMap.clear();
        if (list != null) {
            for (String mapping : list) {
                values = mapping.split("=");
                if (values.length == 2) {
                    LOGGER.debug("Adding mapping: {} = {} to matchOneMap.", values[1].trim(),
                            values[0].trim());
                    matchOneMap.put(values[1].trim(), values[0].trim());
                } else {
                    LOGGER.warn(
                            "Match one mapping ignored: {} doesn't match expected format of metacardAttribute=userAttribute",
                            mapping);
                }
            }
        }
    }

    /**
     * Takes in a comma-delimited string of match one mappings.
     * 
     * @see SimpleAuthzRealm#setMatchOneMappings(List)
     * @param commaStr
     */
    public void setMatchOneMappings(String commaStr) {
        setMatchOneMappings(convertToList(commaStr));
    }

    private List<String> convertToList(String commaStr) {
        List<String> list = new ArrayList<String>();
        if (commaStr != null) {
            for (String curValue : commaStr.split(",")) {
                curValue = curValue.trim();
                list.add(curValue);
            }
        }
        return list;
    }
}
