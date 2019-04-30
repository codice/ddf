/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.ui.searchui.filter

import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class RedirectServletSpec extends Specification {

    // the default property value for defaultUri
    private static final String DEFAULT_DEFAULTURI = '/search/catalog/'

    def 'test default defaultUri'() {
        given:
        final RedirectServlet redirectServlet = new RedirectServlet(defaultDefaultUri)
        redirectServlet.init()

        final HttpServletResponse mockServletResponse = Mock()

        when:
        redirectServlet.service(Mock(HttpServletRequest), mockServletResponse)

        then:
        1 * mockServletResponse.sendRedirect(defaultDefaultUri)

        where:
        defaultDefaultUri << [DEFAULT_DEFAULTURI, 'https://an/absolute/uri', '/a/relative/uri']
    }

    def 'test invalid default defaultUri'() {
        when:
        new RedirectServlet(defaultDefaultUri)

        then:
        thrown expectedException

        where:
        defaultDefaultUri   || expectedException
        null                || NullPointerException
        ''                  || IllegalArgumentException
        ' '                 || IllegalArgumentException
        ',,,invalid URI,,,' || IllegalArgumentException
    }

    def 'test set defaultUri'() {
        given:
        final RedirectServlet redirectServlet = new RedirectServlet(DEFAULT_DEFAULTURI)
        redirectServlet.init()

        final HttpServletResponse mockServletResponse = Mock()

        redirectServlet.setDefaultUri(newDefaultUri)

        when:
        redirectServlet.service(Mock(HttpServletRequest), mockServletResponse)

        then:
        1 * mockServletResponse.sendRedirect(newDefaultUri)

        where:
        newDefaultUri << [DEFAULT_DEFAULTURI, 'https://an/absolute/uri', '/a/relative/uri']
    }

    def 'test set invalid defaultUri'() {
        given:
        final String defaultDefaultUri = DEFAULT_DEFAULTURI
        final RedirectServlet redirectServlet = new RedirectServlet(defaultDefaultUri)
        redirectServlet.init()

        final HttpServletResponse mockServletResponse = Mock()

        when:
        redirectServlet.setDefaultUri(newDefaultUri)

        then:
        thrown expectedException

        when:
        redirectServlet.service(Mock(HttpServletRequest), mockServletResponse)

        then:
        1 * mockServletResponse.sendRedirect(defaultDefaultUri)

        where:
        newDefaultUri       || expectedException
        null                || NullPointerException
        ''                  || IllegalArgumentException
        ' '                 || IllegalArgumentException
        ',,,invalid URI,,,' || IllegalArgumentException
    }
}
