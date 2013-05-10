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
 * @author Scott Tustison
 */
public class MatchOneCollectionPermission extends CollectionPermission
{
    public MatchOneCollectionPermission(Collection<Permission> permissions)
    {
        super(permissions);
    }

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
