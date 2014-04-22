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
package org.codice.admin.auth;

import org.jasypt.contrib.org.apache.commons.codec_1_3.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AccountException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * Credit goes to https://github.com/hawtio/hawtio/blob/b4e23e002639c274a2f687ada980118512f06113/hawtio-system/src/main/java/io/hawt/web/AuthenticationFilter.java
 * <p/>
 * Code manipulated for DDF specific environment.
 */
public class AuthenticationFilter implements Filter {

    public enum Authentication {
        AUTHORIZED, NOT_AUTHORIZED, NO_CREDENTIALS
    }

    private static final transient Logger LOGGER = LoggerFactory
            .getLogger(AuthenticationFilter.class);

    public static final String HEADER_AUTHORIZATION = "Authorization";

    public static final String AUTHENTICATION_SCHEME_BASIC = "Basic";

    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

    private final AuthenticationConfiguration configuration = new AuthenticationConfiguration();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        configuration.setRealm("karaf");
        configuration.setRole("admin");
        configuration.setRolePrincipalClasses(
                "java.lang.String,org.apache.karaf.jaas.boot.principal.RolePrincipal");
        configuration.setEnabled(true);

        if (configuration.isEnabled()) {
            LOGGER.info(
                    "Starting authentication filter, JAAS realm: \"{}\" authorized role: \"{}\" role principal classes: \"{}\"",
                    new Object[] {configuration.getRealm(), configuration.getRole(),
                            configuration.getRolePrincipalClasses()}
            );
        } else {
            LOGGER.info("Starting authentication filter, JAAS authentication disabled");
        }
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response,
            final FilterChain chain) throws
            IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getServletPath();
        LOGGER.debug("Handling request for path {}", path);

        if (configuration.getRealm() == null || configuration.getRealm().equals("")
                || !configuration.isEnabled()) {
            LOGGER.debug("No authentication needed for path {}", path);
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            Subject subject = (Subject) session.getAttribute("subject");
            if (subject != null) {
                LOGGER.debug("Session subject {}", subject);
                executeAs(request, response, chain, subject);
                return;
            }
        }

        LOGGER.debug("Doing authentication and authorization for path {}", path);
        Authentication result = authenticate(configuration.getRealm(), configuration.getRole(),
                configuration.getRolePrincipalClasses(),
                httpRequest, new PrivilegedCallback() {
                    public void execute(Subject subject) throws Exception {
                        executeAs(request, response, chain, subject);
                    }
                }
        );
        switch (result) {
        case AUTHORIZED:
            break;
        case NOT_AUTHORIZED:
            doAuthPrompt(configuration.getRealm(), (HttpServletResponse) response);
            break;
        case NO_CREDENTIALS:
            doAuthPrompt(configuration.getRealm(), (HttpServletResponse) response);
            break;
        }
    }

    private void doAuthPrompt(String realm, HttpServletResponse response) {
        try {
            response.setHeader(HEADER_WWW_AUTHENTICATE,
                    AUTHENTICATION_SCHEME_BASIC + " realm=\"" + realm + "\"");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentLength(0);
            response.flushBuffer();
        } catch (IOException ioe) {
            LOGGER.debug("Failed to send auth response: {}", ioe);
        }

    }

    private void extractAuthInfo(String authHeader, ExtractAuthInfoCallback cb) {
        authHeader = authHeader.trim();
        String[] parts = authHeader.split(" ");
        if (parts.length == 2) {
            String authType = parts[0];
            String authInfo = parts[1];

            if (authType.equalsIgnoreCase(AUTHENTICATION_SCHEME_BASIC)) {
                String decoded = new String(Base64.decodeBase64(authInfo.getBytes()));
                parts = decoded.split(":");
                if (parts.length == 2) {
                    String user = parts[0];
                    String password = parts[1];
                    cb.getAuthInfo(user, password);
                }
            }
        }
    }

    private Authentication authenticate(String realm, String role, String rolePrincipalClasses,
            HttpServletRequest request, PrivilegedCallback cb) {

        String authHeader = request.getHeader(HEADER_AUTHORIZATION);

        if (authHeader == null || authHeader.equals("")) {
            return Authentication.NO_CREDENTIALS;
        }

        final String[] username = {null};
        final String[] pass = {null};

        extractAuthInfo(authHeader, new ExtractAuthInfoCallback() {
            @Override
            public void getAuthInfo(String userName, String password) {
                username[0] = userName;
                pass[0] = password;
            }
        });

        if (username[0] == null) {
            return Authentication.NO_CREDENTIALS;
        }

        if (username[0] != null && pass[0] != null) {
            Subject subject = doAuthenticate(realm, role, rolePrincipalClasses, username[0],
                    pass[0]);
            if (subject == null) {
                return Authentication.NOT_AUTHORIZED;
            }

            if (cb != null) {
                try {
                    cb.execute(subject);
                } catch (Exception e) {
                    LOGGER.warn("Failed to execute privileged action: ", e);
                }
            }

            return Authentication.AUTHORIZED;
        }

        return Authentication.NO_CREDENTIALS;
    }

    private Subject doAuthenticate(String realm, String role, String rolePrincipalClasses,
            final String username, final String password) {
        try {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "doAuthenticate[realm={}, role={}, rolePrincipalClasses={}, configuration={}, username={}, password={}]",
                        new Object[] {realm, role, rolePrincipalClasses, configuration, username,
                                "******"});
            }

            Subject subject = new Subject();
            CallbackHandler handler = new AuthenticationCallbackHandler(username, password);

            LoginContext loginContext;
            loginContext = new LoginContext(realm, subject, handler);

            loginContext.login();

            if (role != null && role.length() > 0 && rolePrincipalClasses != null
                    && rolePrincipalClasses.length() > 0) {

                String[] rolePrincipalClazzes = rolePrincipalClasses.split(",");
                boolean found = false;
                for (String clazz : rolePrincipalClazzes) {
                    String name = role;
                    int idx = role.indexOf(':');
                    if (idx > 0) {
                        clazz = role.substring(0, idx);
                        name = role.substring(idx + 1);
                    }
                    for (Principal p : subject.getPrincipals()) {
                        if (p.getClass().getName().equals(clazz.trim())
                                && p.getName().equals(name)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        break;
                    }
                }
                if (!found) {
                    LOGGER.debug("User does not have the required role " + role);
                    return null;
                }
            }

            return subject;

        } catch (AccountException e) {
            LOGGER.warn("Account failure", e);
        } catch (LoginException e) {
            // do not be so verbose at DEBUG level
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Login failed due " + e.getMessage(), e);
            } else {
                LOGGER.debug("Login failed due " + e.getMessage());
            }
        }

        return null;
    }

    private static final class AuthenticationCallbackHandler implements CallbackHandler {

        private final String username;

        private final String password;

        private AuthenticationCallbackHandler(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Callback type {} -> {}", callback.getClass(), callback);
                }
                if (callback instanceof NameCallback) {
                    ((NameCallback) callback).setName(username);
                } else if (callback instanceof PasswordCallback) {
                    ((PasswordCallback) callback).setPassword(password.toCharArray());
                } else {
                    LOGGER.warn(
                            "Unsupported callback class [" + callback.getClass().getName() + "]");
                }
            }
        }
    }

    private static void executeAs(final ServletRequest request, final ServletResponse response,
            final FilterChain chain, Subject subject) {
        try {
            Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    chain.doFilter(request, response);
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            LOGGER.info("Failed to invoke action " + ((HttpServletRequest) request).getPathInfo()
                    + " due to:", e);
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("Destroying authentication filter");
    }

    public interface ExtractAuthInfoCallback {

        public void getAuthInfo(String userName, String password);

    }

    public interface PrivilegedCallback {

        public void execute(Subject subject) throws Exception;

    }
}