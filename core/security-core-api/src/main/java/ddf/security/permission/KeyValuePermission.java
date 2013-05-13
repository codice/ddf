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

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class KeyValuePermission implements Permission
{

    private String key;
    private List<String> values;

    public KeyValuePermission( String key )
    {
        this(key, new ArrayList<String>());
    }
    
    public KeyValuePermission( String key, List<String> values)
    {
        if (key == null)
        {
            throw new IllegalArgumentException("Incoming key cannot be null, could not create permission.");
        }
        this.key = key;
        if (values == null)
        {
            this.values = new ArrayList<String>();
        }
        else
        {
            this.values = values;
        }
    }

    public String getKey()
    {
        return key;
    }

    public List<String> getValues()
    {
        return Collections.unmodifiableList(values);
    }
    
    public void addValue(String value)
    {
        values.add(value);
    }

    @Override
    public boolean implies( Permission p )
    {
        if (p instanceof KeyValuePermission)
        {
            if (getKey().equals(((KeyValuePermission) p).getKey()))
            {
                WildcardPermission thisWildCard = buildWildcardFromKeyValue(this);
                WildcardPermission implied = buildWildcardFromKeyValue((KeyValuePermission) p);
                return thisWildCard.implies(implied);
            }
        }
        else if(p instanceof KeyValueCollectionPermission)
        {
            WildcardPermission thisWildCard = buildWildcardFromKeyValue(this);
            List<KeyValuePermission> permissionList = ((KeyValueCollectionPermission) p).getKeyValuePermissionList();
            for(KeyValuePermission keyValuePermission : permissionList)
            {
                if(getKey().equals(keyValuePermission.getKey()))
                {
                    WildcardPermission implied = buildWildcardFromKeyValue(keyValuePermission);
                    return thisWildCard.implies(implied);
                }
            }
        }
        else if(p instanceof WildcardPermission)
        {
            WildcardPermission thisWildCard = buildWildcardFromKeyValue(this);
            return thisWildCard.implies(p);
        }
        return false;
    }

    /**
     * Returns a {@link org.apache.shiro.authz.permission.WildcardPermission} representing a {@link KeyValuePermission}
     * @param perm  the permission to convert.
     * @return new equivalent permission
     */
    private WildcardPermission buildWildcardFromKeyValue(KeyValuePermission perm)
    {
        StringBuilder wildcardString = new StringBuilder();
        for(String value : perm.getValues())
        {
            wildcardString.append(value);
            wildcardString.append(",");
        }
        WildcardPermission wildcardPermission = new WildcardPermission(wildcardString.toString().substring(0, wildcardString.length()-1));
        return wildcardPermission;
    }
    
    @Override
    public String toString()
    {
        return key + " : " + StringUtils.join(values, ",");
    }

}
