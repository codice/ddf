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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Scott Tustison
 */
public class CollectionPermission implements Permission
{

    protected List<Permission> permissionList = new ArrayList<Permission>();

    public CollectionPermission()
    {

    }

    public CollectionPermission(Permission... permissions)
    {
        for(Permission permission : permissions)
        {
            permissionList.add(permission);
        }
    }

    public CollectionPermission(Collection<Permission> permissions)
    {
        permissionList.addAll(permissions);
    }

    /**
     * Returns {@code true} if this current instance <em>implies</em> all the functionality and/or resource access
     * described by the specified {@code Permission} argument, {@code false} otherwise.
     * <p/>
     * <p>That is, this current instance must be exactly equal to or a <em>superset</em> of the functionalty
     * and/or resource access described by the given {@code Permission} argument.  Yet another way of saying this
     * would be:
     * <p/>
     * <p>If &quot;permission1 implies permission2&quot;, i.e. <code>permission1.implies(permission2)</code> ,
     * then any Subject granted {@code permission1} would have ability greater than or equal to that defined by
     * {@code permission2}.
     *
     * @param p the permission to check for behavior/functionality comparison.
     * @return {@code true} if this current instance <em>implies</em> all the functionality and/or resource access
     *         described by the specified {@code Permission} argument, {@code false} otherwise.
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
                    if (ourPerm.implies(perm))
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
            if(permission.implies(p))
            {
                return true;
            }
        }
        return false;
    }

    public List<Permission> getPermissionList()
    {
        return Collections.unmodifiableList(permissionList);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (Permission perm : permissionList)
        {
            sb.append('[');
            sb.append(perm.toString());
            sb.append("] ");
        }
        return sb.toString();
    }
}
