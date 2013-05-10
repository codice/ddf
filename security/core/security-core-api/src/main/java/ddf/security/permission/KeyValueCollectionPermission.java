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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Scott Tustison
 */
public class KeyValueCollectionPermission extends CollectionPermission
{

    public KeyValueCollectionPermission()
    {

    }

    public KeyValueCollectionPermission(KeyValuePermission... permissions)
    {
        for(KeyValuePermission permission : permissions)
        {
            permissionList.add(permission);
        }
    }

    public KeyValueCollectionPermission(Map<String, List<String>> map)
    {
        Set<String> keys = map.keySet();
        for(String key : keys)
        {
            permissionList.add(new KeyValuePermission(key, map.get(key)));
        }
    }

    public KeyValueCollectionPermission(Collection<KeyValuePermission> permissions)
    {
        permissionList.addAll(permissions);
    }

    public <T> List<T> getKeyValuePermissionList()
    {
        return (List<T>) Collections.unmodifiableList(permissionList);
    }
}
