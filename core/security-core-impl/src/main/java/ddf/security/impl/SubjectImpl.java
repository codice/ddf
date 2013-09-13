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
package ddf.security.impl;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;

import ddf.security.Subject;

public class SubjectImpl extends DelegatingSubject implements Subject {

    private static final long serialVersionUID = 1L;

    public SubjectImpl(PrincipalCollection principals, boolean authenticated, String host,
            Session session, boolean sessionCreationEnabled, SecurityManager securityManager) {
        super(principals, authenticated, host, session, sessionCreationEnabled, securityManager);
    }

    public SubjectImpl(PrincipalCollection principals, boolean authenticated, String host,
            Session session, SecurityManager securityManager) {
        super(principals, authenticated, host, session, securityManager);
    }

    public SubjectImpl(PrincipalCollection principals, boolean authenticated, Session session,
            SecurityManager securityManager) {
        this(principals, authenticated, null, session, securityManager);
    }

}
