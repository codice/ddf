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
package org.codice.admin.modules.installer.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class RedirectFilter implements Filter {

    private List<String> loginIgnoreList;

    protected static final String ADMIN = "/admin";

    protected static final String INSTALLER = "/installer";

    protected static final String JOLOKIA = "/jolokia";

    protected static final String SYSTEM_CONSOLE = "/system/console";

    protected static final String LOGIN_IGNORE_LIST = "loginIgnoreList";

    private static final transient Logger LOGGER = LoggerFactory
            .getLogger(RedirectFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.debug("Starting Redirect filter.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        LOGGER.debug("Request is: {}", httpRequest.getRequestURI());
        boolean allowableRequest = false;
        if (loginIgnoreList == null) {
            Map tempMap = new HashMap<String, Object>();
            String[] tempArray = new String[]{ADMIN, INSTALLER, JOLOKIA, SYSTEM_CONSOLE};
            tempMap.put(LOGIN_IGNORE_LIST, tempArray);
            setLoginIgnoreList(tempMap);
        }
        for (String item : loginIgnoreList) {
            if (httpRequest.getRequestURI().contains(item)) {
                allowableRequest = true;
                LOGGER.debug("No redirect; continuing filter chain...");
                chain.doFilter(request, response);
                break;
            }
        }
        if (!allowableRequest) {
            LOGGER.debug("Redirecting to /admin!");
            httpResponse.sendRedirect("/admin");
            httpResponse.flushBuffer();
            return;
        }
    }

    @Override
    public void destroy() {
        LOGGER.debug("Destroying Redirect filter.");
    }

    public void setLoginIgnoreList(Map<String, Object> properties) {
        Object propArrayObject = properties.get(LOGIN_IGNORE_LIST);
        String[] propArray;

        if (propArrayObject != null && propArrayObject instanceof String[]) {
            propArray = (String[]) properties.get(LOGIN_IGNORE_LIST);
            List<String> propList = new ArrayList<String>();
            for (String item : propArray) {
                if(item.contains(",")) {
                    String[] items = item.split(",");
                    propList.addAll(Arrays.asList(items));
                } else {
                    propList.add(item);
                }
            }
            this.loginIgnoreList = propList;
        } else if (propArrayObject != null) {
            propArray = ((String) propArrayObject).split(",");
            this.loginIgnoreList = Arrays.asList(propArray);
        }
    }

    public List<String> getLoginIgnoreList() {
        return this.loginIgnoreList;
    }
}
