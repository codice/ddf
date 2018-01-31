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
import ddf.security.assertion.SecurityAssertion
import ddf.security.principal.GuestPrincipal
import org.apache.shiro.subject.PrincipalCollection
import org.opensaml.core.xml.schema.XSString
import org.opensaml.saml.saml2.core.Attribute
import org.opensaml.saml.saml2.core.AttributeStatement
import org.opensaml.saml.saml2.core.AuthnStatement
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.stream.Collectors

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
        def authnStatement = Mock(AuthnStatement)

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
        assertion.getAuthnStatements() >> [authnStatement]
        assertion.getNotOnOrAfter() >> notOnOrAfter

        attrStatement.getAttributes() >> attrs

        subject
    }

    protected def mockAttribute(Map.Entry<String, List<String>> attribute) {
        def attr = Mock(Attribute)

        attr.getName() >> attribute.getKey()
        attr.getAttributeValues() >> attribute.value.collect {
            mockXSString(it)
        }

        attr
    }

    protected def mockXSString(String str) {
        def xstr = Mock(XSString)

        xstr.getValue() >> str

        xstr
    }
}
