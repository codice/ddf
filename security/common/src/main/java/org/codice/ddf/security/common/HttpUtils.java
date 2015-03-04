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
package org.codice.ddf.security.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

public class HttpUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);
    /**
     * Sends the given response code back to the caller.
     *
     * @param code     HTTP response code for this request
     * @param response the servlet response object
     */
    public static void returnSimpleResponse(int code, HttpServletResponse response) {
        try {
            LOGGER.debug("Sending response code {}", code);
            response.setStatus(code);
            response.setContentLength(0);
            response.flushBuffer();
        } catch (IOException ioe) {
            LOGGER.debug("Failed to send auth response", ioe);
        }
    }

    /**
     * Returns a mapping of cookies from the incoming request. Key is the cookie name, while the
     * value is the Cookie object itself.
     *
     * @param req Servlet request for this call
     * @return map of Cookie objects present in the current request - always returns a map
     */
    public static Map<String, Cookie> getCookieMap(HttpServletRequest req) {
        HashMap<String, Cookie> map = new HashMap<String, Cookie>();

        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie != null) {
                    map.put(cookie.getName(), cookie);
                }
            }
        }

        return map;
    }

    public static void deleteCookie(String cookieName, HttpServletRequest request, HttpServletResponse response) {
        //remove session cookie
        try {
            LOGGER.debug("Removing cookie {}", cookieName);
            response.setContentType("text/html");
            Cookie cookie = new Cookie(cookieName, "");
            URL url = null;
            url = new URL(request.getRequestURL().toString());
            cookie.setDomain(url.getHost());
            cookie.setMaxAge(0);
            cookie.setPath("/");
            cookie.setComment("EXPIRING COOKIE at " + System.currentTimeMillis());
            response.addCookie(cookie);
        } catch (MalformedURLException e) {
            LOGGER.warn("Unable to delete cookie {}", cookieName, e);
        }
    }

}
