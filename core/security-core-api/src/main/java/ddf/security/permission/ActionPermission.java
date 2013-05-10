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


public class ActionPermission implements Permission
{

    public static final String QUERY_ACTION = "query";
    public static final String CREATE_ACTION = "create";
    public static final String DELETE_ACTION = "delete";
    public static final String UPDATE_ACTION = "update";

    private String action;

    public ActionPermission( String action )
    {
        if (action == null)
        {
            throw new IllegalArgumentException("Incoming action cannot be null, could not create permission.");
        }
        this.action = action;
    }

    public String getAction()
    {
        return action;
    }

    @Override
    public boolean implies( Permission p )
    {
        if (p instanceof ActionPermission)
        {
            return action.equals(((ActionPermission) p).getAction());
        }
        else
        {
            return false;
        }
    }

    public String toString()
    {
        return "Action: " + action;
    }

}
