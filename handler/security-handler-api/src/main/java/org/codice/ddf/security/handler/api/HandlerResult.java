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
package org.codice.ddf.security.handler.api;

import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.shiro.authc.AuthenticationToken;

import java.security.Principal;

/**
 * Encapsulates the return status for each handler. Consists of the status of any action taken by
 * the handler (successfully retrieved desired tokens, responded to a client in order to obtain
 * missing tokens, or no action taken), as well as the actual tokens retrieved from the header.
 */
public class HandlerResult implements AuthenticationToken {
    public enum Status {
        // completed - auth tokens retrieved ready to move on
        COMPLETED,

        // no tokens found, no attempt made to obtain any
        NO_ACTION,

        // performing action to obtain auth tokens, stop processing
        REDIRECTED
    }

    private Status status;

    private Object principal;

    private String authCredentials;

    private SecurityToken token;

    private String source;

    public HandlerResult() {
        status = Status.NO_ACTION;
    }

    public HandlerResult(Status fs, Object p, String creds) {
        this.status = fs;
        this.principal = p;
        this.authCredentials = creds;
    }

    public HandlerResult(Status fs, Object p, SecurityToken t) {
        this.status = fs;
        this.principal = p;
        this.token = t;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {

        this.status = status;
    }

    public void setPrincipal(Object obj) {
        this.principal = obj;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    public void setAuthCredentials(String creds) {
        this.authCredentials = creds;
    }

    public void setSecurityToken(SecurityToken t) {
        this.token = t;
    }

    public boolean hasSecurityToken() { return this.token != null; }

    @Override
    public Object getCredentials() {
        return token != null ? token : authCredentials;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String src) {
        this.source = src;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Status: ");
        sb.append(status.toString());
        sb.append("; Principal name: ");
        sb.append(principal == null ? "none" : ((Principal) principal).getName());
        sb.append("; authCredentials: ");
        sb.append(authCredentials);
        sb.append("; token: ");
        sb.append(token);
        sb.append("; source: ");
        sb.append(source);
        return sb.toString();
    }
}
