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

import com.google.common.collect.ImmutableList
import com.google.common.net.HttpHeaders
import org.codice.ddf.platform.filter.AuthenticationException
import org.eclipse.jetty.http.HttpMethod
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.codice.ddf.platform.filter.FilterChain

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

    static final String MOZILLA_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36"
    static final String CHROME_USER_AGENT = "Chrome/60.0.3112.113 Safari/537.36"
    static final String APACHE_USER_AGENT = "Apache-CXF/3.2.5"
    static final String JAVA_CLIENT_USER_AGENT = "Google-HTTP-Java-Client/1.22.0 (gzip)"
    static final String JAVA_USER_AGENT = "Java/1.8.0_131"

    def setupSpec() {
        System.properties['org.codice.ddf.system.hostname'] = DDF_HOST
        System.properties['org.codice.ddf.system.httpPort'] = DDF_HTTP_PORT
        System.properties['org.codice.ddf.system.httpsPort'] = DDF_HTTPS_PORT

        System.properties['org.codice.ddf.external.hostname'] = PROXY_HOST
        System.properties['org.codice.ddf.external.httpPort'] = PROXY_HTTP_PORT
        System.properties['org.codice.ddf.external.httpsPort'] = PROXY_HTTPS_PORT
    }

    @Unroll
    def "CSRF Browser Protection Allowed: context: #requestContext, origin: #originHeader, referer: #refererHeader, csrfHeader: #hasCsrfHeader, httpVerb: #method"(
            String requestContext, String originHeader, String refererHeader, boolean hasCsrfHeader, String method) {
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
        request.getMethod() >> method
        if (hasCsrfHeader) {
            request.getHeader(CsrfFilter.CSRF_HEADER) >> "XMLHttpRequest"
        } else {
            request.getHeader(CsrfFilter.CSRF_HEADER) >> null
        }
        try {
            csrfFilter.doFilter(request, response, chain)
        } catch (AuthenticationException e) {

        }
        then:
        0 * response.setStatus(HttpServletResponse.SC_FORBIDDEN)
        0 * response.sendError(HttpServletResponse.SC_FORBIDDEN)
        0 * response.flushBuffer()
        1 * chain.doFilter(_, _)

        where:
        [requestContext, originHeader, refererHeader, hasCsrfHeader, method] << [
                // Non-protected contexts
                ["/", "/subdirectory"],
                [null, "", EXTERNAL_SITE, EXTERNAL_UPPER, DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT, DDF_BADPORT, PROXY_BADPORT],
                [null, "", EXTERNAL_SITE, EXTERNAL_UPPER, DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT, DDF_BADPORT, PROXY_BADPORT],
                [true, false],
                [HttpMethod.GET.asString()]
        ].combinations() + [
                // Websockets - same origin OR same referer, with/without CSRF header
                ["/search/catalog/ws", "/search/catalog/ws/subdirectory"],
                [DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT],
                [DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT],
                [true, false],
                [HttpMethod.GET.asString()]
        ].combinations() + [
                ["/search/catalog/ws", "/search/catalog/ws/subdirectory"],
                [null, ""],
                [DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT],
                [true, false],
                [HttpMethod.GET.asString()]
        ].combinations() + [
                ["/search/catalog/ws", "/search/catalog/ws/subdirectory", "/search/catalog/internal/catalog/", "/search/catalog/internal/catalog/sources"],
                [DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT],
                [null, ""],
                [true, false],
                [HttpMethod.GET.asString()]
        ].combinations() + [
                //  Protected Contexts - same origin OR referer, with CSRF header
                ["/admin/jolokia", "/admin/jolokia/subdirectory", "/search/catalog/internal", "/search/catalog/internal/subdirectory"],
                [DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT],
                [DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT],
                [true],
                [HttpMethod.GET.asString()]
        ].combinations() + [
                ["/admin/jolokia", "/admin/jolokia/subdirectory", "/search/catalog/internal", "/search/catalog/internal/subdirectory"],
                [DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT],
                [null, ""],
                [true],
                [HttpMethod.GET.asString()]
        ].combinations() + [
                ["/admin/jolokia", "/admin/jolokia/subdirectory", "/search/catalog/internal", "/search/catalog/internal/subdirectory"],
                [null, ""],
                [DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT],
                [true],
                [HttpMethod.GET.asString()]
        ].combinations()
    }

    @Unroll
    def "CSRF Browser Protection Fobidden: context: #requestContext, origin: #originHeader, referer: #refererHeader, csrfHeader: #hasCsrfHeader, httpVerb: #method"(
            String requestContext, String originHeader, String refererHeader, boolean hasCsrfHeader, String method) {
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
        request.getMethod() >> method
        if (hasCsrfHeader) {
            request.getHeader(CsrfFilter.CSRF_HEADER) >> "XMLHttpRequest"
        } else {
            request.getHeader(CsrfFilter.CSRF_HEADER) >> null
        }

        try {
            csrfFilter.doFilter(request, response, chain)
        } catch (AuthenticationException e) {

        }
        then:
        1 * response.setStatus(HttpServletResponse.SC_FORBIDDEN)
        1 * response.sendError(HttpServletResponse.SC_FORBIDDEN)
        1 * response.flushBuffer()
        0 * chain.doFilter(_, _)

        where:
        [requestContext, originHeader, refererHeader, hasCsrfHeader, method] << [
                // Protected Contexts - no CSRF Header
                ["/admin/jolokia", "/admin/jolokia/subdirectory", "/search/catalog/internal", "/search/catalog/internal/subdirectory"],
                [null, "", EXTERNAL_SITE, EXTERNAL_UPPER, DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT, DDF_BADPORT, PROXY_BADPORT],
                [null, "", EXTERNAL_SITE, EXTERNAL_UPPER, DDF_HTTP, DDF_HTTPS, DDF_UPPER, PROXY_HTTP, PROXY_HTTPS, PROXY_UPPER, PROXY_HTTP_NOPORT, PROXY_HTTPS_NOPORT, DDF_BADPORT, PROXY_BADPORT],
                [false],
                [HttpMethod.GET.asString(), HttpMethod.POST.asString()]
        ].combinations() + [
                // Protected Contexts - different or no origin/referer, with CSRF header
                ["/admin/jolokia", "/admin/jolokia/subdirectory", "/search/catalog/internal", "/search/catalog/internal/subdirectory"],
                [null, "", EXTERNAL_SITE, EXTERNAL_UPPER, DDF_BADPORT, PROXY_BADPORT],
                [null, "", EXTERNAL_SITE, EXTERNAL_UPPER, DDF_BADPORT, PROXY_BADPORT],
                [true],
                [HttpMethod.GET.asString(), HttpMethod.POST.asString()]
        ].combinations() + [
                // Websockets and Catalog context- different or no origin/referer, with/without CSRF header
                ["/search/catalog/ws", "/search/catalog/ws/subdirectory", "/search/catalog/internal/catalog/", "/search/catalog/internal/catalog/sources"],
                [null, "", EXTERNAL_SITE, EXTERNAL_UPPER, DDF_BADPORT, PROXY_BADPORT],
                [null, "", EXTERNAL_SITE, EXTERNAL_UPPER, DDF_BADPORT, PROXY_BADPORT],
                [true, false],
                [HttpMethod.GET.asString(), HttpMethod.POST.asString()]
        ].combinations() + [
                // Corrupted origin/referer headers
                ["/admin/jolokia", "/admin/jolokia/subdirectory", "/search/catalog/internal", "/search/catalog/internal/subdirectory"],
                [null, "", "com?ht.p://*example", "!@#\$%^&*(){}:<>?", "undefined", "0", "true", "\r\n", "\\r\\n"],
                [null, "", "com?ht.p://*example", "!@#\$%^&*(){}:<>?", "undefined", "0", "true", "\r\n", "\\r\\n"],
                [true, false],
                [HttpMethod.GET.asString(), HttpMethod.POST.asString()]
        ].combinations()
    }

    @Unroll
    def "CSRF System Protection Allowed: context: #requestContext, agent: #userAgent, httpVerb: #method, parameter: #param"(
            String requestContext, String userAgent, String method, String param) {
        given:
        CsrfFilter csrfFilter = new CsrfFilter()
        csrfFilter.init()
        HttpServletResponse response = Mock(HttpServletResponse)
        FilterChain chain = Mock(FilterChain)

        when:
        HttpServletRequest request = Mock(HttpServletRequest)
        request.getRequestURI() >> requestContext
        request.getHeader(HttpHeaders.USER_AGENT) >> userAgent
        request.getMethod() >> method
        request.getQueryString() >> param

        csrfFilter.setWhiteListContexts(ImmutableList.of('/services/admin/config[/]?$=GET',
                '/services/content[/]?$=GET',
                '/services/catalog/query[/]?$=GET',
                '/services/catalog/sources.*=GET',
                '/services/idp/login[/]?$=POST',
                '/services/saml/sso[/]?$=POST'))

        try {
            csrfFilter.doFilter(request, response, chain)
        } catch (AuthenticationException e) {

        }
        then:
        0 * response.setStatus(HttpServletResponse.SC_FORBIDDEN)
        0 * response.sendError(HttpServletResponse.SC_FORBIDDEN)
        0 * response.flushBuffer()
        1 * chain.doFilter(_, _)

        where:
        [requestContext, userAgent, method, param] << [
                // Non-protected contexts
                ["/", "/subdirectory"],
                [null, "", CHROME_USER_AGENT, MOZILLA_USER_AGENT, APACHE_USER_AGENT, JAVA_CLIENT_USER_AGENT, JAVA_USER_AGENT],
                [HttpMethod.GET.asString(), HttpMethod.POST.asString()],
                [null]
        ].combinations() + [
                // Whitelisted paths with GET
                ["/services/admin/config", "/services/admin/config/", "/services/content", "/services/catalog/query", "/services/catalog/sources", "/services/catalog/sources/ddf.distribution"],
                [null, "", CHROME_USER_AGENT, MOZILLA_USER_AGENT, APACHE_USER_AGENT, JAVA_CLIENT_USER_AGENT, JAVA_USER_AGENT],
                [HttpMethod.GET.asString()],
                [null]
        ].combinations() + [
                // Whitelisted paths with POST
                ["/services/idp/login", "/services/idp/login/", "/services/saml/sso"],
                [null, "", CHROME_USER_AGENT, MOZILLA_USER_AGENT, APACHE_USER_AGENT, JAVA_CLIENT_USER_AGENT, JAVA_USER_AGENT],
                [HttpMethod.POST.asString()],
                [null]
        ].combinations() + [
                // GETs to wsdl URLs
                ["services/csw/", "/services/csw"],
                [null, "", CHROME_USER_AGENT, MOZILLA_USER_AGENT, APACHE_USER_AGENT, JAVA_CLIENT_USER_AGENT, JAVA_USER_AGENT],
                [HttpMethod.GET.asString()],
                ["wsdl", "WSDL"]
        ].combinations() + [
                // Not whitelisted path with not blacklisted user-agents
                ["/services/catalog/", "/services/catalog/subdirectory"],
                [null, "", APACHE_USER_AGENT, JAVA_CLIENT_USER_AGENT, JAVA_USER_AGENT],
                [HttpMethod.GET.asString(), HttpMethod.POST.asString()],
                [null]
        ].combinations()
    }

    @Unroll
    def "CSRF System Protection Forbidden: context: #requestContext, agent: #userAgent, httpVerb: #method, parameter: #param"(
            String requestContext, String userAgent, String method, String param) {
        given:
        CsrfFilter csrfFilter = new CsrfFilter()
        csrfFilter.init()
        HttpServletResponse response = Mock(HttpServletResponse)
        FilterChain chain = Mock(FilterChain)

        when:
        HttpServletRequest request = Mock(HttpServletRequest)
        request.getRequestURI() >> requestContext
        request.getHeader(HttpHeaders.USER_AGENT) >> userAgent
        request.getMethod() >> method
        request.getQueryString() >> param

        csrfFilter.setWhiteListContexts(ImmutableList.of('/services/admin/config[/]?$=GET',
                '/services/content[/]?$=GET',
                '/services/catalog/query[/]?$=GET',
                '/services/catalog/sources.*=GET',
                '/services/idp/login[/]?$=POST',
                '/services/saml/sso[/]?$=POST'))

        try {
            csrfFilter.doFilter(request, response, chain)
        } catch (AuthenticationException e) {

        }
        then:
        1 * response.setStatus(HttpServletResponse.SC_FORBIDDEN)
        1 * response.sendError(HttpServletResponse.SC_FORBIDDEN)
        1 * response.flushBuffer()
        0 * chain.doFilter(_, _)

        where:
        [requestContext, userAgent, method, param] << [
                // Whitelisted paths with incorrect method
                ["/services/admin/config", "/services/content", "/services/catalog/query", "/services/catalog/sources", "/services/catalog/sources/ddf.distribution"],
                [CHROME_USER_AGENT, MOZILLA_USER_AGENT],
                [HttpMethod.POST.asString()],
                [null]
        ].combinations() + [
                // Whitelisted paths with incorrect method
                ["/services/idp/login", "/services/saml/sso"],
                [CHROME_USER_AGENT, MOZILLA_USER_AGENT],
                [HttpMethod.GET.asString()],
                [null]
        ].combinations() + [
                // Not whitelisted path with blacklisted user-agents
                ["/services/catalog/", "/services/catalog/subdirectory"],
                [CHROME_USER_AGENT, MOZILLA_USER_AGENT],
                [HttpMethod.GET.asString(), HttpMethod.POST.asString()],
                [null]
        ].combinations()
    }
}
