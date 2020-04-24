/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.pdp.realm;

import ddf.security.audit.SecurityLogger;
import ddf.security.pdp.realm.xacml.XacmlPdp;
import ddf.security.pdp.realm.xacml.processor.PdpException;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.permission.impl.CollectionPermissionImpl;
import ddf.security.permission.impl.KeyValueCollectionPermissionImpl;
import ddf.security.permission.impl.KeyValuePermissionImpl;
import ddf.security.permission.impl.MatchOneCollectionPermission;
import ddf.security.policy.extension.PolicyExtension;
import ddf.security.service.impl.AbstractAuthorizingRealm;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.PermissionResolver;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.CollectionUtils;
import org.codice.ddf.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This simple Authz {@link ddf.security.service.impl.AbstractAuthorizingRealm} provides the ability
 * to check permissions without making calls out to an external PDP. {@link Permission} objects are
 * checked against each other to ensure that the subject permissions imply the resource permissions.
 *
 * @author tustisos
 */
public class AuthzRealm extends AbstractAuthorizingRealm {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthzRealm.class);

  private static final String PERMISSION_FINISH_1_MSG = "Finished permission check for user [";

  private static final String PERMISSION_FINISH_2_MSG = "]. Result is that permission [";

  private static final String POLICY_EXTENSION_WARNING_MSG =
      "Policy Extension plugin did not complete correctly. This could allow access to a resource.";

  private final String dirPath;

  private final Parser parser;

  private List<PolicyExtension> policyExtensions = new ArrayList<>();

  private HashMap<String, String> matchAllMap = new HashMap<>();

  private HashMap<String, String> matchOneMap = new HashMap<>();

  private List<String> environmentAttributes = new ArrayList<>();

  private XacmlPdp xacmlPdp;

  private SecurityLogger securityLogger;

  public AuthzRealm(String dirPath, Parser parser) throws PdpException {
    super();

    this.dirPath = dirPath;
    this.parser = parser;
  }

  // this realm is for authorization only
  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
      throws AuthenticationException {
    return null;
  }

  /**
   * Returns an account's authorization-specific information for the specified {@code principals},
   * or {@code null} if no account could be found. The resulting {@code AuthorizationInfo} object is
   * used by the other method implementations in this class to automatically perform access control
   * checks for the corresponding {@code Subject}.
   *
   * <p>This implementation obtains the actual {@code AuthorizationInfo} object from the subclass's
   * implementation of {@link #doGetAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection)
   * doGetAuthorizationInfo}, and then caches it for efficient reuse if caching is enabled (see
   * below).
   *
   * <p>Invocations of this method should be thought of as completely orthogonal to acquiring {@link
   * #getAuthenticationInfo(org.apache.shiro.authc.AuthenticationToken) authenticationInfo} , since
   * either could occur in any order.
   *
   * <p>For example, in &quot;Remember Me&quot; scenarios, the user identity is remembered (and
   * assumed) for their current session and an authentication attempt during that session might
   * never occur. But because their identity would be remembered, that is sufficient enough
   * information to call this method to execute any necessary authorization checks. For this reason,
   * authentication and authorization should be loosely coupled and not depend on each other.
   *
   * <h3>Caching</h3>
   *
   * The {@code AuthorizationInfo} values returned from this method are cached for efficient reuse
   * if caching is enabled. Caching is enabled automatically when an {@link #setAuthorizationCache
   * authorizationCache} instance has been explicitly configured, or if a {@link #setCacheManager
   * cacheManager} has been configured, which will be used to lazily create the {@code
   * authorizationCache} as needed.
   *
   * <p>If caching is enabled, the authorization cache will be checked first and if found, will
   * return the cached {@code AuthorizationInfo} immediately. If caching is disabled, or there is a
   * cache miss, the authorization info will be looked up from the underlying data store via the
   * {@link #doGetAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection)} method, which
   * must be implemented by subclasses.
   *
   * <h4>Changed Data</h4>
   *
   * If caching is enabled and if any authorization data for an account is changed at runtime, such
   * as adding or removing roles and/or permissions, the subclass implementation should clear the
   * cached AuthorizationInfo for that account via the {@link
   * #clearCachedAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection)
   * clearCachedAuthorizationInfo} method. This ensures that the next call to {@code
   * getAuthorizationInfo(PrincipalCollection)} will acquire the account's fresh authorization data,
   * where it will then be cached for efficient reuse. This ensures that stale authorization data
   * will not be reused.
   *
   * @param principals the corresponding Subject's identifying principals with which to look up the
   *     Subject's {@code AuthorizationInfo}.
   * @return the authorization information for the account associated with the specified {@code
   *     principals}, or {@code null} if no account could be found.
   */
  @Override
  public AuthorizationInfo getAuthorizationInfo(PrincipalCollection principals) {
    return super.getAuthorizationInfo(principals);
  }

  /**
   * Returns <tt>true</tt> if the corresponding subject/user is permitted to perform an action or
   * access a resource summarized by the specified permission.
   *
   * <p>
   *
   * <p>More specifically, this method determines if any <tt>Permission</tt>s associated with the
   * subject {@link Permission#implies(Permission) imply} the specified permission.
   *
   * @param subjectPrincipal the application-specific subject/user identifier.
   * @param permission the permission that is being checked.
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
   *
   * <p>More specifically, this method should determine if each <tt>Permission</tt> in the array is
   * {@link Permission#implies(Permission) implied} by permissions already associated with the
   * subject.
   *
   * <p>
   *
   * <p>This is primarily a performance-enhancing method to help reduce the number of {@link
   * #isPermitted} invocations over the wire in client/server systems.
   *
   * @param subjectPrincipal the application-specific subject/user identifier.
   * @param permissions the permissions that are being checked.
   * @return an array of booleans whose indices correspond to the index of the permissions in the
   *     given list. A true value at an index indicates the user is permitted for for the associated
   *     <tt>Permission</tt> object in the list. A false value at an index indicates otherwise.
   */
  @Override
  public boolean[] isPermitted(PrincipalCollection subjectPrincipal, List<Permission> permissions) {
    boolean[] results = new boolean[permissions.size()];
    AuthorizationInfo authorizationInfo = getAuthorizationInfo(subjectPrincipal);
    List<Permission> expandedPermissions = expandPermissions(permissions);
    int i = 0;
    for (Permission permission : expandedPermissions) {
      results[i++] = isPermitted(subjectPrincipal, permission, authorizationInfo);
    }

    return results;
  }

  /**
   * Checks if the corresponding Subject/user contained within the AuthorizationInfo object implies
   * the given Permission.
   *
   * @param permission the permission being checked.
   * @param authorizationInfo the application-specific subject/user identifier.
   * @return true if the user is permitted
   */
  private boolean isPermitted(
      PrincipalCollection subjectPrincipal,
      Permission permission,
      AuthorizationInfo authorizationInfo) {
    Collection<Permission> perms = getPermissions(authorizationInfo);
    String curUser = "<user>";
    if (subjectPrincipal != null && subjectPrincipal.getPrimaryPrincipal() != null) {
      curUser = subjectPrincipal.getPrimaryPrincipal().toString();
    }
    if (!CollectionUtils.isEmpty(perms)) {
      if (permission instanceof KeyValuePermission) {
        permission =
            new KeyValueCollectionPermissionImpl(
                CollectionPermission.UNKNOWN_ACTION, (KeyValuePermission) permission);
        LOGGER.debug(
            "Should not execute subject.isPermitted with KeyValuePermission. Instead create a KeyValueCollectionPermission with an action.");
      }
      if (permission != null && permission instanceof KeyValueCollectionPermission) {
        KeyValueCollectionPermission kvcp = (KeyValueCollectionPermission) permission;
        List<KeyValuePermission> keyValuePermissions = kvcp.getKeyValuePermissionList();
        List<KeyValuePermission> matchOnePermissions = new ArrayList<>();
        List<KeyValuePermission> matchAllPermissions = new ArrayList<>();

        List<KeyValuePermission> matchAllPreXacmlPermissions = new ArrayList<>();

        for (KeyValuePermission keyValuePermission : keyValuePermissions) {
          String metacardKey = keyValuePermission.getKey();
          // user specified this key in the match all list - remap key
          if (matchAllMap.containsKey(metacardKey)) {
            KeyValuePermission kvp =
                new KeyValuePermissionImpl(
                    matchAllMap.get(metacardKey), keyValuePermission.getValues());
            matchAllPermissions.add(kvp);
            // user specified this key in the match one list - remap key
          } else if (matchOneMap.containsKey(metacardKey)) {
            KeyValuePermission kvp =
                new KeyValuePermissionImpl(
                    matchOneMap.get(metacardKey), keyValuePermission.getValues());
            matchOnePermissions.add(kvp);
            // this key was not specified in either - default to match all with the
            // same key value
          } else {
            // creating a KeyValuePermission list to try to quick match all of these permissions
            // if that fails, then XACML will try to match them
            // this covers the case where attributes on the user match up perfectly with the
            // permissions being implied
            // this also allows the xacml permissions to run through the policy extensions
            matchAllPreXacmlPermissions.add(keyValuePermission);
          }
        }

        CollectionPermission subjectAllCollection =
            new CollectionPermissionImpl(CollectionPermission.UNKNOWN_ACTION, perms);
        KeyValueCollectionPermission matchAllCollection =
            new KeyValueCollectionPermissionImpl(kvcp.getAction(), matchAllPermissions);
        KeyValueCollectionPermission matchAllPreXacmlCollection =
            new KeyValueCollectionPermissionImpl(kvcp.getAction(), matchAllPreXacmlPermissions);
        KeyValueCollectionPermission matchOneCollection =
            new KeyValueCollectionPermissionImpl(kvcp.getAction(), matchOnePermissions);

        matchAllCollection =
            isPermittedByExtensionAll(subjectAllCollection, matchAllCollection, kvcp);
        matchAllPreXacmlCollection =
            isPermittedByExtensionAll(subjectAllCollection, matchAllPreXacmlCollection, kvcp);
        matchOneCollection =
            isPermittedByExtensionOne(subjectAllCollection, matchOneCollection, kvcp);
        MatchOneCollectionPermission subjectOneCollection = new MatchOneCollectionPermission(perms);

        boolean matchAll = subjectAllCollection.implies(matchAllCollection);
        boolean matchAllXacml = subjectAllCollection.implies(matchAllPreXacmlCollection);
        boolean matchOne = subjectOneCollection.implies(matchOneCollection);
        if (!matchAll || !matchOne) {
          securityLogger.audit(
              PERMISSION_FINISH_1_MSG
                  + curUser
                  + PERMISSION_FINISH_2_MSG
                  + permission
                  + "] is not implied.");
        }

        // if we weren't able to automatically imply these permissions, call out to XACML
        if (!matchAllXacml) {
          KeyValueCollectionPermission xacmlPermissions =
              new KeyValueCollectionPermissionImpl(kvcp.getAction(), matchAllPreXacmlPermissions);
          configureXacmlPdp();
          matchAllXacml = xacmlPdp.isPermitted(curUser, authorizationInfo, xacmlPermissions);
          if (!matchAllXacml) {
            securityLogger.audit(
                PERMISSION_FINISH_1_MSG
                    + curUser
                    + PERMISSION_FINISH_2_MSG
                    + permission
                    + "] is not implied via XACML.");
          }
        }
        return matchAll && matchOne && matchAllXacml;
      }

      for (Permission perm : perms) {
        if (permission != null && perm.implies(permission)) {
          return true;
        }
      }
    }

    securityLogger.audit(
        PERMISSION_FINISH_1_MSG
            + curUser
            + PERMISSION_FINISH_2_MSG
            + permission
            + "] is not implied.");
    return false;
  }

  private void configureXacmlPdp() {
    if (xacmlPdp == null) {
      try {
        xacmlPdp = new XacmlPdp(dirPath, parser, environmentAttributes, securityLogger);
      } catch (PdpException e) {
        LOGGER.warn("Unable to create XACML PDP.", e);
      }
    }
  }

  private KeyValueCollectionPermission isPermittedByExtensionAll(
      CollectionPermission subjectAllCollection,
      KeyValueCollectionPermission matchAllCollection,
      KeyValueCollectionPermission allPermissionsCollection) {
    if (!CollectionUtils.isEmpty(policyExtensions)) {
      KeyValueCollectionPermission resultCollection = new KeyValueCollectionPermissionImpl();
      resultCollection.addAll(matchAllCollection.getPermissionList());
      resultCollection.setAction(matchAllCollection.getAction());
      for (PolicyExtension policyExtension : policyExtensions) {
        try {
          resultCollection =
              policyExtension.isPermittedMatchAll(
                  subjectAllCollection, resultCollection, allPermissionsCollection);
        } catch (Exception e) {
          securityLogger.auditWarn(POLICY_EXTENSION_WARNING_MSG, e);
          LOGGER.warn(POLICY_EXTENSION_WARNING_MSG, e);
        }
      }
      return resultCollection;
    }
    return matchAllCollection;
  }

  private KeyValueCollectionPermission isPermittedByExtensionOne(
      CollectionPermission subjectAllCollection,
      KeyValueCollectionPermission matchOneCollection,
      KeyValueCollectionPermission allPermissionsCollection) {
    if (!CollectionUtils.isEmpty(policyExtensions)) {
      KeyValueCollectionPermission resultCollection = new KeyValueCollectionPermissionImpl();
      resultCollection.addAll(matchOneCollection.getPermissionList());
      resultCollection.setAction(matchOneCollection.getAction());
      for (PolicyExtension policyExtension : policyExtensions) {
        try {
          resultCollection =
              policyExtension.isPermittedMatchOne(
                  subjectAllCollection, resultCollection, allPermissionsCollection);
        } catch (Exception e) {
          securityLogger.auditWarn(POLICY_EXTENSION_WARNING_MSG, e);
          LOGGER.warn(POLICY_EXTENSION_WARNING_MSG, e);
        }
      }
      return resultCollection;
    }
    return matchOneCollection;
  }

  /**
   * Returns a collection of {@link Permission} objects that the {@link AuthorizationInfo} object of
   * a {@link ddf.security.Subject} is asserting.
   *
   * @param authorizationInfo the application-specific subject/user identifier.
   * @return collection of Permissions.
   */
  @Override
  protected Collection<Permission> getPermissions(AuthorizationInfo authorizationInfo) {
    Set<Permission> permissions = new HashSet<>();

    if (authorizationInfo != null) {
      Collection<Permission> perms = authorizationInfo.getObjectPermissions();
      if (!CollectionUtils.isEmpty(perms)) {
        permissions.addAll(perms);
      }
      perms = resolvePermissions(authorizationInfo.getStringPermissions());
      if (!CollectionUtils.isEmpty(perms)) {
        permissions.addAll(perms);
      }

      perms = resolveRolePermissions(authorizationInfo.getRoles());
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
   * @param stringPerms collection of Strings that represent permissions.
   * @return collection of Permissions
   */
  private Collection<Permission> resolvePermissions(Collection<String> stringPerms) {
    Collection<Permission> perms = Collections.emptySet();
    PermissionResolver resolver = getPermissionResolver();
    if (resolver != null && !CollectionUtils.isEmpty(stringPerms)) {
      perms = new HashSet<>(stringPerms.size());
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
   * @param roleNames user roles.
   * @return collection of Permissions
   */
  private Collection<Permission> resolveRolePermissions(Collection<String> roleNames) {
    Collection<Permission> perms = Collections.emptySet();
    RolePermissionResolver resolver = getRolePermissionResolver();
    if (resolver != null && !CollectionUtils.isEmpty(roleNames)) {
      perms = new HashSet<>(roleNames.size());
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
   * Sets list of policy extension objects
   *
   * @param policyExtensions
   */
  public void setPolicyExtensions(List<PolicyExtension> policyExtensions) {
    this.policyExtensions = policyExtensions;
  }

  public void addPolicyExtension(PolicyExtension policyExtension) {
    if (policyExtensions != null) {
      policyExtensions.add(policyExtension);
    }
  }

  public void removePolicyExtension(PolicyExtension policyExtension) {
    if (policyExtensions != null) {
      policyExtensions.remove(policyExtension);
    }
  }

  /**
   * Sets the mappings used by the "match all" evaluation to determine if this user should be
   * authorized to access requested data.
   *
   * <p>Each string is of the format: <code>subjectAttrName=metacardAttrName</code><br>
   * where <code>metacardAttrName</code> is the name of an attribute in the metacard and <code>
   * subjectAttrName</code> is the name of the corresponding attribute in the user credentials.<br>
   * It is the values corresponding to each of these attributes that will be evaluated against each
   * other when determining if authorization should be allowed.
   *
   * @param list List of Strings that define mappings between metadata attributes and user
   *     attributes
   */
  public void setMatchAllMappings(List<String> list) {
    String[] values;
    matchAllMap.clear();
    if (list != null) {
      for (String mapping : list) {
        values = mapping.split("=");
        if (values.length == 2) {
          securityLogger.audit(
              "Adding mapping: {} = {} to matchAllMap.", values[1].trim(), values[0].trim());
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
   * Sets the mappings used by the "match one" evaluation to determine if this user should be
   * authorized to access requested data.
   *
   * <p>Each string is of the format: <code>subjectAttrName=metacardAttrName</code><br>
   * where <code>metacardAttrName</code> is the name of an attribute in the metacard and <code>
   * subjectAttrName</code> is the name of the corresponding attribute in the user credentials.<br>
   * It is the values corresponding to each of these attributes that will be evaluated against each
   * other when determining if authorization should be allowed.
   *
   * @param list List of Strings that define mappings between metadata attributes and user
   *     attributes
   */
  public void setMatchOneMappings(List<String> list) {
    String[] values;
    matchOneMap.clear();
    if (list != null) {
      for (String mapping : list) {
        values = mapping.split("=");
        if (values.length == 2) {
          securityLogger.audit(
              "Adding mapping: {} = {} to matchOneMap.", values[1].trim(), values[0].trim());
          matchOneMap.put(values[1].trim(), values[0].trim());
        } else {
          LOGGER.warn(
              "Match one mapping ignored: {} doesn't match expected format of metacardAttribute=userAttribute",
              mapping);
        }
      }
    }
  }

  public void setEnvironmentAttributes(List<String> environmentAttributes) {
    this.environmentAttributes.clear();
    this.environmentAttributes.addAll(environmentAttributes);
  }

  public void setSecurityLogger(SecurityLogger securityLogger) {
    this.securityLogger = securityLogger;
  }
}
