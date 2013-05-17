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
package ddf.security.permission;

import org.apache.shiro.authz.Permission;

import java.util.Collection;

/**
 * Permission class handling the match one case. Shiro permissions always "match all" attributes. This class
 * extends the CollectionPermission and overrides the implies method to perform the implies for a "match one"
 * condition.
 */
public class MatchOneCollectionPermission extends CollectionPermission
{
    public MatchOneCollectionPermission(Collection<Permission> permissions)
    {
        super(permissions);
    }

    /**
     * Overrides the implies method to handle checking for the existence of one attribute - the "match one"
     * scenario rather than the "match all" behavior of the overridden classes. Specifically, this permission
     * will imply another permission if that permission matches at least one of our permission attributes.
     * @param p the permission to check for behavior/functionality comparison.
     * @return  {@code true} if this current instance <em>implies</em> the specified {@code Permission}
     *          argument, {@code false} otherwise.
     */
    @Override
    public boolean implies(Permission p)
    {
        if (permissionList.isEmpty())
            return false;

        if (p instanceof CollectionPermission)
        {
            for (Permission perm : ((CollectionPermission) p).getPermissionList())
            {
                boolean result = false;
                for (Permission ourPerm : permissionList)
                {
                    //Shiro permissions are always a "match all" condition so we need to flip the implies to make it match one
                    if (perm.implies(ourPerm))
                    {
                        result = true;
                        break;
                    }
                }
                if (!result)
                    return false;
            }
            return true;
        }

        for(Permission permission : permissionList)
        {
            //Shiro permissions are always a "match all" condition so we need to flip the implies to make it match one
            if(p.implies(permission))
            {
                return true;
            }
        }
        return false;
    }
}
