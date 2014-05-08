package ddf.security.filter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
public class FilterUtils
{
    /**
     * Returns a mapping of cookies from the incoming request. Key is the cookie name, while the
     * value is the Cookie object itself.
     * @param req Servlet request for this call
     * @return map of Cookie objects present in the current request - always returns a map
     */
    public static Map<String, Cookie> getCookieMap(HttpServletRequest req) {
        HashMap<String, Cookie> map = new HashMap<String, Cookie>();

        Cookie[] cookies = req.getCookies();
        for (Cookie cookie : cookies) {
            map.put(cookie.getName(), cookie);
        }

        return map;
    }

}
