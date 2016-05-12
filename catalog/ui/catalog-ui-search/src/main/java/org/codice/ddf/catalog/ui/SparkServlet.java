/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * <p>
 * Copyright 2011- Per Wendel
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Copyright 2011- Per Wendel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codice.ddf.catalog.ui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Optional;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.globalstate.ServletFlag;
import spark.http.matching.MatcherFilter;
import spark.route.ServletRoutes;
import spark.servlet.SparkApplication;
import spark.staticfiles.StaticFilesConfiguration;

// Based on https://github.com/perwendel/spark/issues/193
public class SparkServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkServlet.class);

    private static final String SLASH_WILDCARD = "/*";

    private static final String SLASH = "/";

    private static final String FILTER_MAPPING_PARAM = "filterMappingUrlPattern";

    private static SparkApplication sparkApplication;

    private static MatcherFilter matcherFilter;

    public SparkServlet(SparkApplication application) {
        sparkApplication = application;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletFlag.runFromServlet();

        sparkApplication.init();

        matcherFilter = new MatcherFilter(ServletRoutes.get(),
                StaticFilesConfiguration.servletInstance,
                true,
                false);
    }

    @Override
    public void destroy() {
        if (sparkApplication != null) {
            sparkApplication.destroy();
        }
    }

    private String getFilterPath() throws ServletException {
        Optional<String> mapping = this.getServletContext()
                .getServletRegistration(this.getServletName())
                .getMappings()
                .stream()
                .findFirst();
        if (mapping.isPresent()) {
            String result = mapping.get();
            if (result == null || result.equals(SLASH_WILDCARD)) {
                return "";
            } else if (!result.startsWith(SLASH) || !result.endsWith(SLASH_WILDCARD)) {
                throw new RuntimeException("The " + FILTER_MAPPING_PARAM
                        + " must start with \"/\" and end with \"/*\". It's: " + result);
            }
            return result.substring(1, result.length() - 1);
        } else {
            throw new ServletException("Unable to read servlet mapping");
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        final String requestPath = getFilterPath();
        final String relativePath = getRelativePath(req, requestPath);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(relativePath);
        }

        HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(req) {
            @Override
            public String getPathInfo() {
                return relativePath;
            }

            @Override
            public String getRequestURI() {
                return relativePath;
            }
        };

        matcherFilter.doFilter(requestWrapper, resp, null);
    }

    static String getRelativePath(HttpServletRequest request, String filterPath) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();

        path = path.substring(contextPath.length());

        if (path.length() > 0) {
            path = path.substring(1);
        }

        if (!path.startsWith(filterPath) && filterPath.equals(path + SLASH)) {
            path += SLASH;
        }
        if (path.startsWith(filterPath)) {
            path = path.substring(filterPath.length());
        }

        if (!path.startsWith(SLASH)) {
            path = SLASH + path;
        }

        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // this can't really ever happen
        }
        return path;
    }

}
