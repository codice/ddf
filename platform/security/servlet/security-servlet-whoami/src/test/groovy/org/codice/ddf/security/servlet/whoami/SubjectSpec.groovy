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

import ddf.security.Subject
import ddf.security.assertion.Attribute
import ddf.security.assertion.AttributeStatement
import ddf.security.assertion.AuthenticationStatement
import ddf.security.assertion.SecurityAssertion
import ddf.security.principal.GuestPrincipal
import java.time.LocalDateTime
import java.time.ZoneId
import org.apache.shiro.subject.PrincipalCollection
import spock.lang.Specification

class SubjectSpec extends Specification {

    protected Date notOnOrAfter = Date.from(LocalDateTime.now()
            .plusDays(6)
            .plusHours(18)
            .plusMinutes(20)
            .plusSeconds(35)
            .atZone(ZoneId.systemDefault()).toInstant())

    protected def mockSubject() {
        def subject = Mock(Subject)
        def pc = Mock(PrincipalCollection)
        def assertion = Mock(SecurityAssertion)
        def principal = new GuestPrincipal("127.0.0.1")
        def attrStatement = Mock(AttributeStatement)
        def authenticationStatement = Mock(AuthenticationStatement)

        def attributes = ['http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress':
                                  ['guest@localhost']]

        def attrs = attributes.collect {
            mockAttribute(it)
        }

        subject.getPrincipals() >> pc

        pc.oneByType(SecurityAssertion) >> assertion
        pc.byType(SecurityAssertion) >> [assertion]

        assertion.getPrincipal() >> principal
        assertion.getAttributeStatements() >> [attrStatement]
        assertion.getIssuer() >> "TestIssuer"
        assertion.getAuthnStatements() >> [authenticationStatement]
        assertion.getNotOnOrAfter() >> notOnOrAfter

        attrStatement.getAttributes() >> attrs

        subject
    }

    protected def mockAttribute(Map.Entry<String, List<String>> attribute) {
        def attr = Mock(Attribute)

        attr.getName() >> attribute.getKey()
        attr.getValues() >> attribute.getValue().collect {
            it
        }

        attr
    }
}
