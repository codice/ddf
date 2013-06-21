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
package ddf.ldap.ldaplogin;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class GroupImpl implements Group
{
    private String name;
    private Set<Principal> members;
    
    public GroupImpl(String name)
    {
        init(name, new HashSet<Principal>());
    }
    
    public GroupImpl(String name, Principal member)
    {
        Set<Principal> members = new HashSet<Principal>();
        members.add(member);
        init(name, members);
    }
    
    public GroupImpl(String name, Set<Principal> members)
    {
        init(name, members);
    }
    
    public void init(String name, Set<Principal> members)
    {
        this.name = name;
        this.members = members;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean addMember( Principal member )
    {
        return members.add(member);
    }

    @Override
    public boolean isMember( Principal member )
    {
        return members.contains(member);
    }

    @Override
    public Enumeration<? extends Principal> members()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean removeMember( Principal member )
    {
        return members.remove(member);
    }

}
