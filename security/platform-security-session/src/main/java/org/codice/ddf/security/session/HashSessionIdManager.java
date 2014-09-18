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
package org.codice.ddf.security.session;

import org.eclipse.jetty.server.session.AbstractSession;
import org.eclipse.jetty.server.session.AbstractSessionIdManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Custom implementation of the {@link org.eclipse.jetty.server.SessionIdManager} that shares session
 * data between sessions in a cluster.
 */
public class HashSessionIdManager extends AbstractSessionIdManager {

    private final Map<String, Set<WeakReference<HttpSession>>> _sessions = new HashMap<String, Set<WeakReference<HttpSession>>>();

    public HashSessionIdManager() {
    }

    public HashSessionIdManager(Random random) {
        super(random);
    }

    /**
     * @return Collection of String session IDs
     */
    public Collection<String> getSessions() {
        return Collections.unmodifiableCollection(_sessions.keySet());
    }

    /**
     * @return Collection of Sessions for the passed session ID
     */
    public Collection<HttpSession> getSession(String id) {
        ArrayList<HttpSession> sessions = new ArrayList<HttpSession>();
        Set<WeakReference<HttpSession>> refs = _sessions.get(id);
        if (refs != null) {
            for (WeakReference<HttpSession> ref : refs) {
                HttpSession session = ref.get();
                if (session != null) {
                    sessions.add(session);
                }
            }
        }
        return sessions;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        _sessions.clear();
        super.doStop();
    }

    /**
     * @see org.eclipse.jetty.server.SessionIdManager#idInUse(String)
     */
    @Override
    public boolean idInUse(String id) {
        synchronized (this) {
            return _sessions.containsKey(id);
        }
    }

    /**
     * @see org.eclipse.jetty.server.SessionIdManager#addSession(HttpSession)
     */
    @Override
    public void addSession(HttpSession session) {
        String id = getClusterId(session.getId());
        WeakReference<HttpSession> ref = new WeakReference<HttpSession>(session);

        synchronized (this) {
            Set<WeakReference<HttpSession>> sessions = _sessions.get(id);
            if (sessions == null) {
                sessions = new HashSet<WeakReference<HttpSession>>();
                _sessions.put(id, sessions);
            } else {
                Iterator<WeakReference<HttpSession>> iterator = sessions.iterator();
                if (iterator.hasNext()) {
                    WeakReference<HttpSession> weakReference = iterator.next();
                    if (weakReference != null) {
                        HttpSession httpSession = weakReference.get();
                        if (httpSession != null) {
                            Enumeration enumeration = httpSession.getAttributeNames();
                            while (enumeration.hasMoreElements()) {
                                Object obj = enumeration.nextElement();
                                if (obj instanceof String) {
                                    Object value = httpSession.getAttribute((String) obj);
                                    if (value != null) {
                                        session.setAttribute((String) obj, value);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            sessions.add(ref);
        }
    }

    /**
     * @see org.eclipse.jetty.server.SessionIdManager#removeSession(HttpSession)
     */
    @Override
    public void removeSession(HttpSession session) {
        String id = getClusterId(session.getId());

        synchronized (this) {
            Collection<WeakReference<HttpSession>> sessions = _sessions.get(id);
            if (sessions != null) {
                for (Iterator<WeakReference<HttpSession>> iter = sessions.iterator(); iter
                        .hasNext();) {
                    WeakReference<HttpSession> ref = iter.next();
                    HttpSession s = ref.get();
                    if (s == null) {
                        iter.remove();
                        continue;
                    }
                    if (s == session) {
                        iter.remove();
                        break;
                    }
                }
                if (sessions.isEmpty()) {
                    _sessions.remove(id);
                }
            }
        }
    }

    /**
     * @see org.eclipse.jetty.server.SessionIdManager#invalidateAll(String)
     */
    @Override
    public void invalidateAll(String id) {
        Collection<WeakReference<HttpSession>> sessions;
        synchronized (this) {
            sessions = _sessions.remove(id);
        }

        if (sessions != null) {
            for (WeakReference<HttpSession> ref : sessions) {
                AbstractSession session = (AbstractSession) ref.get();
                if (session != null && session.isValid()) {
                    session.invalidate();
                }
            }
            sessions.clear();
        }
    }

    /**
     * Get the session ID without any worker ID.
     *
     * @param nodeId the node id
     * @return sessionId without any worker ID.
     */
    @Override
    public String getClusterId(String nodeId) {
        int dot = nodeId.lastIndexOf('.');
        return (dot > 0) ? nodeId.substring(0, dot) : nodeId;
    }

    /**
     * Get the session ID with any worker ID.
     *
     * @param clusterId
     * @param request
     * @return sessionId plus any worker ID.
     */
    @Override
    public String getNodeId(String clusterId, HttpServletRequest request) {
        // used in Ajp13Parser
        String worker = request == null ?
                null :
                (String) request.getAttribute("org.eclipse.jetty.ajp.JVMRoute");
        if (worker != null) {
            return clusterId + '.' + worker;
        }

        if (_workerName != null) {
            return clusterId + '.' + _workerName;
        }

        return clusterId;
    }
}
