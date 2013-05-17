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

/**
 * Permission class containing actions and the code to imply one action permission from another.
 */
public class ActionPermission implements Permission
{
    /**
     * The string corresponding to a query action - used to create equivalent ActionPermissions.
     */
    public static final String QUERY_ACTION = "query";
    /**
     * The string corresponding to a create action - used to create equivalent ActionPermissions.
     */
    public static final String CREATE_ACTION = "create";
    /**
     * The string corresponding to a delete action - used to create equivalent ActionPermissions.
     */
    public static final String DELETE_ACTION = "delete";
    /**
     * The string corresponding to a update action - used to create equivalent ActionPermissions.
     */
    public static final String UPDATE_ACTION = "update";

    private String action;

    /**
     * Creates a new ActionPermission with the provided action. Action can be any string. The action string
     * is used during the implies operation to determine if the actions are equivalent.
     * @param action  represents the action this permission allows
     */
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

    /**
     * Determines if this permission implies the provided permission. If the provided permission is an
     * ActionPermission and if the actions are equivalent, this method will return true. Otherwise, false
     * is returned.
     * @param p  Permission to be checked against this action to determine if it should be implied
     * @return  true if this action implies the provided permission, false otherwise
     */
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

    /**
     * Returns a string representation of this ActionPermission.
     * @return  string representation of this ActionPermission
     */
    @Override
    public String toString()
    {
        return "Action: " + action;
    }

}
