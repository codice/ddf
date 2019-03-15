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

import ddf.security.service.SecurityManager
import groovy.json.JsonSlurper
import org.apache.shiro.util.ThreadContext

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class WhoAmIServletSpec extends SubjectSpec {

    WhoAmIServlet whoAmIServlet

    def setup() {
        whoAmIServlet = new WhoAmIServlet()

        def securityManager = Mock(SecurityManager)
        securityManager.getSubject(_) >> mockSubject()

        def subject = mockSubject()
        ThreadContext.bind(subject)
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

        assert json.default.whoAmISubjects.get(0).email == 'guest@localhost'
        assert json.default.whoAmISubjects.size() == 1
        assert json.default.whoAmISubjects.get(0).claims.size() == 1
        assert json.default.whoAmISubjects.get(0).isGuest

        true
    }

}
