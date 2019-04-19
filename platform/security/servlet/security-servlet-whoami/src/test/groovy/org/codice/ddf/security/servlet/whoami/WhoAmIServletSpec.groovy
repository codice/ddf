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
 */
package org.codice.ddf.security.servlet.whoami

import ddf.security.SecurityConstants
import ddf.security.common.SecurityTokenHolder
import ddf.security.http.SessionFactory
import ddf.security.service.SecurityManager
import groovy.json.JsonSlurper
import org.apache.cxf.ws.security.tokenstore.SecurityToken

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

import static org.mockito.Mockito.mock

class WhoAmIServletSpec extends SubjectSpec {

    WhoAmIServlet whoAmIServlet

    def setup() {
        whoAmIServlet = new WhoAmIServlet()

        def sessionFactory = Mock(SessionFactory)
        def httpSession = Mock(HttpSession)
        def securityTokenHolder = Mock(SecurityTokenHolder)

        sessionFactory.getOrCreateSession(_ as HttpServletRequest) >> httpSession
        httpSession.getAttribute(SecurityConstants.SAML_ASSERTION) >> securityTokenHolder
        securityTokenHolder.getSecurityToken() >> mock(SecurityToken)

        whoAmIServlet.setHttpSessionFactory(sessionFactory)

        def securityManager = Mock(SecurityManager)
        securityManager.getSubject(_) >> mockSubject()

        whoAmIServlet.setSecurityManager(securityManager)
    }

    def 'Prints valid json response'() {
        setup:
        def resp = Mock(HttpServletResponse)
        def writer = Mock(PrintWriter)
        resp.getWriter() >> writer

        when:
        whoAmIServlet.doGet(Mock(HttpServletRequest), resp)

        then:
        1 * writer.print({ verifyJsonReponse(it) })
    }

    private def verifyJsonReponse(String body) {
        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parseText(body)

        assert json.email == 'guest@localhost'
        assert json.claims.size() == 1
        assert json.isGuest

        true
    }

}
