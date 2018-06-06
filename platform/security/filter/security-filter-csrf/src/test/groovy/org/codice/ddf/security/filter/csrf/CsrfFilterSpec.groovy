/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.filter.csrf

import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class CsrfFilterSpec extends Specification {

    @Unroll
    def 'test allowed'(String requestContext, String originHeader, String refererHeader, boolean hasCsrfHeader) {
        given:
        CsrfFilter csrfFilter = new CsrfFilter()
        csrfFilter.init()
        HttpServletResponse response = Mock(HttpServletResponse)
        FilterChain chain = Mock(FilterChain)

        when:
        String requestUrl = "https://ddf:123" + requestContext
        HttpServletRequest request = Mock(HttpServletRequest)
        request.getRequestURL() >> new StringBuffer(requestUrl)
        request.getRequestURI() >> requestContext
        request.getHeader(CsrfFilter.ORIGIN_HEADER) >> originHeader
        request.getHeader(CsrfFilter.REFERER_HEADER) >> refererHeader
        if (hasCsrfHeader) {
            request.getHeader(CsrfFilter.CSRF_HEADER) >> "XMLHttpRequest"
        } else {
            request.getHeader(CsrfFilter.CSRF_HEADER) >> null
        }

        csrfFilter.doFilter(request, response, chain)

        then:
        0 * response.setStatus(HttpServletResponse.SC_FORBIDDEN)
        0 * response.sendError(HttpServletResponse.SC_FORBIDDEN)
        0 * response.flushBuffer()
        1 * chain.doFilter(_, _)

        where:
        requestContext             | originHeader          | refererHeader         | hasCsrfHeader
        // Cross origin allowed contexts
        "/"                        | null                  | null                  | false
        "/test"                    | null                  | null                  | false
        "/test"                    | "https://example.com" | null                  | false
        "/test"                    | null                  | "https://example.com" | false
        "/test"                    | "https://example.com" | "https://example.com" | false
        "/test"                    | "https://ddf:123/"    | "https://ddf:123/"    | false
        "/test"                    | "https://example.com" | "https://example.com" | true
        "/test"                    | "https://ddf:123/"    | "https://ddf:123/"    | true
        // Cross origin protected contexts
        "/admin/jolokia"           | "https://ddf:123/"    | null                  | true
        "/admin/jolokia"           | null                  | "https://ddf:123/"    | true
        "/admin/jolokia"           | "https://ddf:123/"    | "https://ddf:123/"    | true
        "/search/catalog/internal" | "https://ddf:123/"    | null                  | true
        "/search/catalog/internal" | null                  | "https://ddf:123/"    | true
        "/search/catalog/internal" | "https://ddf:123/"    | "https://ddf:123/"    | true
        // Web sockets endpoint requiring same origin but no CSRF header
        "/search/catalog/ws"       | "https://ddf:123/"    | null                  | true
        "/search/catalog/ws"       | null                  | "https://ddf:123/"    | true
        "/search/catalog/ws"       | "https://ddf:123/"    | "https://ddf:123/"    | true

    }

    @Unroll
    def 'test forbidden'(String requestContext, String originHeader, String refererHeader, boolean hasCsrfHeader) {
        given:
        CsrfFilter csrfFilter = new CsrfFilter()
        csrfFilter.init()
        HttpServletResponse response = Mock(HttpServletResponse)
        FilterChain chain = Mock(FilterChain)

        when:
        String requestUrl = "https://ddf:123" + requestContext
        HttpServletRequest request = Mock(HttpServletRequest)
        request.getRequestURL() >> new StringBuffer(requestUrl)
        request.getRequestURI() >> requestContext
        request.getHeader(CsrfFilter.ORIGIN_HEADER) >> originHeader
        request.getHeader(CsrfFilter.REFERER_HEADER) >> refererHeader
        if (hasCsrfHeader) {
            request.getHeader(CsrfFilter.CSRF_HEADER) >> "XMLHttpRequest"
        } else {
            request.getHeader(CsrfFilter.CSRF_HEADER) >> null
        }

        csrfFilter.doFilter(request, response, chain)

        then:
        1 * response.setStatus(HttpServletResponse.SC_FORBIDDEN)
        1 * response.sendError(HttpServletResponse.SC_FORBIDDEN)
        1 * response.flushBuffer()
        0 * chain.doFilter(_, _)

        where:
        requestContext             | originHeader          | refererHeader         | hasCsrfHeader
        // No CSRF Header - same origin
        "/admin/jolokia"           | "https://ddf:123/"    | null                  | false
        "/admin/jolokia"           | null                  | "https://ddf:123/"    | false
        "/admin/jolokia"           | "https://ddf:123/"    | "https://ddf:123/"    | false
        "/search/catalog/internal" | "https://ddf:123/"    | null                  | false
        "/search/catalog/internal" | null                  | "https://ddf:123/"    | false
        "/search/catalog/internal" | "https://ddf:123/"    | "https://ddf:123/"    | false
        // CSRF header - different origin
        "/admin/jolokia"           | "https://example.com" | null                  | true
        "/admin/jolokia"           | null                  | "https://example.com" | true
        "/admin/jolokia"           | "https://example.com" | "https://example.com" | true
        "/search/catalog/internal" | "https://example.com" | null                  | true
        "/search/catalog/internal" | null                  | "https://example.com" | true
        "/search/catalog/internal" | "https://example.com" | "https://example.com" | true
        // completely cross-site
        "/admin/jolokia"           | null                  | null                  | false
        "/admin/jolokia"           | "https://example.com" | null                  | false
        "/admin/jolokia"           | null                  | "https://example.com" | false
        "/admin/jolokia"           | "https://example.com" | "https://example.com" | false
        "/search/catalog/internal" | null                  | null                  | false
        "/search/catalog/internal" | "https://example.com" | null                  | false
        "/search/catalog/internal" | null                  | "https://example.com" | false
        "/search/catalog/internal" | "https://example.com" | "https://example.com" | false
        // Web sockets endpoint requiring same origin but no CSRF header -
        "/search/catalog/ws"       | "https://example.com" | null                  | true
        "/search/catalog/ws"       | null                  | "https://example.com" | true
        "/search/catalog/ws"       | "https://example.com" | "https://example.com" | true
        // Corrupted origin/referer headers
        "/admin/jolokia"           | "com?ht.p://*example" | null                  | true
        "/admin/jolokia"           | null                  | "com?ht.p://*example" | true
        "/admin/jolokia"           | "com?ht.p://*example" | "com?ht.p://*example" | true
        "/admin/jolokia"           | "!@#\$%^&*(){}:<>?"   | "!@#\$%^&*(){}:<>?"   | true
    }
}