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
import spock.util.environment.RestoreSystemProperties

import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestoreSystemProperties
class CsrfFilterSpec extends Specification {

    static final String DDF_HOST = 'ddf'
    static final String DDF_HTTP_PORT = '8993'
    static final String DDF_HTTPS_PORT = '8181'
    static final String PROXY_HOST = 'proxy'
    static final String PROXY_HTTP_PORT = '80'
    static final String PROXY_HTTPS_PORT = '443'

    static final String DDF_HTTP = "http://" + DDF_HOST + ":" + DDF_HTTP_PORT
    static final String DDF_HTTPS = "https://" + DDF_HOST + ":" + DDF_HTTPS_PORT
    static final String PROXY_HTTP = "http://" + PROXY_HOST + ":" + PROXY_HTTP_PORT
    static final String PROXY_HTTP_NOPORT = "http://" + PROXY_HOST
    static final String PROXY_HTTPS = "https://" + PROXY_HOST + ":" + PROXY_HTTPS_PORT
    static final String PROXY_HTTPS_NOPORT = "https://" + PROXY_HOST
    static final String EXTERNAL_SITE = "https://example.com"

    static final String DDF_UPPER = "https://" + DDF_HOST.toUpperCase() + ":" + DDF_HTTPS_PORT
    static final String PROXY_UPPER = "https://" + PROXY_HOST.toUpperCase() + ":" + PROXY_HTTPS_PORT
    static final String EXTERNAL_UPPER = "https://EXAMPLE.COM"

    static final String DDF_BADPORT = "https://" + DDF_HOST + ":9999"
    static final String PROXY_BADPORT = "https://" + PROXY_HOST + ":" + "9999"


    def setupSpec() {
        System.properties['org.codice.ddf.system.hostname'] = DDF_HOST
        System.properties['org.codice.ddf.system.httpPort'] = DDF_HTTP_PORT
        System.properties['org.codice.ddf.system.httpsPort'] = DDF_HTTPS_PORT

        System.properties['org.codice.ddf.external.hostname'] = PROXY_HOST
        System.properties['org.codice.ddf.external.httpPort'] = PROXY_HTTP_PORT
        System.properties['org.codice.ddf.external.httpsPort'] = PROXY_HTTPS_PORT
    }

    @Unroll
    def 'CSRF Allowed: context: #requestContext, origin: #originHeader, referer: #refererHeader, csrfHeader: #hasCsrfHeader'(
            String requestContext, String originHeader, String refererHeader, boolean hasCsrfHeader) {
        given:
        CsrfFilter csrfFilter = new CsrfFilter()
        csrfFilter.init()
        HttpServletResponse response = Mock(HttpServletResponse)
        FilterChain chain = Mock(FilterChain)

        when:
        HttpServletRequest request = Mock(HttpServletRequest)
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
        [requestContext, originHeader, refererHeader, hasCsrfHeader] << [
                // Non-protected contexts
                ["/", "/subdirectory"],
                [null, "", EXTERNAL_SITE, EXTERNAL_UPPER, DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT, DDF_BADPORT, PROXY_BADPORT],
                [null, "", EXTERNAL_SITE, EXTERNAL_UPPER, DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT, DDF_BADPORT, PROXY_BADPORT],
                [true, false]
        ].combinations() + [
                // Websockets - same origin OR same referer, with/without CSRF header
                ["/search/catalog/ws", "/search/catalog/ws/subdirectory"],
                [DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT],
                [DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT],
                [true, false]
        ].combinations() + [
                ["/search/catalog/ws", "/search/catalog/ws/subdirectory"],
                [null, ""],
                [DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT],
                [true, false]
        ].combinations() + [
                ["/search/catalog/ws", "/search/catalog/ws/subdirectory"],
                [DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT],
                [null, ""],
                [true, false]
        ].combinations() + [
                //  Protected Contexts - same origin OR referer, with CSRF header
                ["/admin/jolokia", "/admin/jolokia/subdirectory", "/search/catalog/internal", "/search/catalog/internal/subdirectory"],
                [DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT],
                [DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT],
                [true]
        ].combinations() + [
                ["/admin/jolokia", "/admin/jolokia/subdirectory", "/search/catalog/internal", "/search/catalog/internal/subdirectory"],
                [DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT],
                [null, ""],
                [true]
        ].combinations() + [
                ["/admin/jolokia", "/admin/jolokia/subdirectory", "/search/catalog/internal", "/search/catalog/internal/subdirectory"],
                [null, ""],
                [DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT],
                [true]
        ].combinations()
    }

    @Unroll
    def 'CSRF Fobidden: context: #requestContext, origin: #originHeader, referer: #refererHeader, csrfHeader: #hasCsrfHeader'(
            String requestContext, String originHeader, String refererHeader, boolean hasCsrfHeader) {
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
        [requestContext, originHeader, refererHeader, hasCsrfHeader] << [
                // Protected Contexts - no CSRF Header
                ["/admin/jolokia", "/admin/jolokia/subdirectory", "/search/catalog/internal", "/search/catalog/internal/subdirectory"],
                [null, "", EXTERNAL_SITE, EXTERNAL_UPPER, DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT, DDF_BADPORT, PROXY_BADPORT],
                [null, "", EXTERNAL_SITE, EXTERNAL_UPPER, DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT, DDF_BADPORT, PROXY_BADPORT],
                [false]
        ].combinations() + [
                // Protected Contexts - different or no origin/referer, with CSRF header
                ["/admin/jolokia", "/admin/jolokia/subdirectory", "/search/catalog/internal", "/search/catalog/internal/subdirectory"],
                [null, "", EXTERNAL_SITE, EXTERNAL_UPPER, DDF_BADPORT, PROXY_BADPORT],
                [null, "", EXTERNAL_SITE, EXTERNAL_UPPER, DDF_BADPORT, PROXY_BADPORT],
                [true]
        ].combinations() + [
                // Websockets - different or no origin/referer, with/without CSRF header
                ["/search/catalog/ws", "/search/catalog/ws/subdirectory"],
                [null, "", EXTERNAL_SITE, EXTERNAL_UPPER, DDF_BADPORT, PROXY_BADPORT],
                [null, "", EXTERNAL_SITE, EXTERNAL_UPPER, DDF_BADPORT, PROXY_BADPORT],
                [true, false]
        ].combinations() + [
                // Corrupted origin/referer headers
                ["/admin/jolokia", "/admin/jolokia/subdirectory", "/search/catalog/internal", "/search/catalog/internal/subdirectory"],
                [null, "", "com?ht.p://*example", "!@#\$%^&*(){}:<>?", "undefined", "0", "true", "\r\n", "\\r\\n"],
                [null, "", "com?ht.p://*example", "!@#\$%^&*(){}:<>?", "undefined", "0", "true", "\r\n", "\\r\\n"],
                [true, false]
        ].combinations()
    }
}
