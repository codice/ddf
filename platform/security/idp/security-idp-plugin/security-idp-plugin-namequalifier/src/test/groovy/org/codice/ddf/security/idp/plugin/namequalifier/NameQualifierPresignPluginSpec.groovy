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
package org.codice.ddf.security.idp.plugin.namequalifier

import static org.opensaml.saml.saml2.core.NameIDType.PERSISTENT

import ddf.security.samlp.SamlProtocol
import org.apache.wss4j.common.saml.OpenSAMLUtil
import spock.lang.Specification
import org.opensaml.saml.saml2.core.AuthnRequest
import org.opensaml.saml.saml2.core.Issuer
import org.opensaml.saml.saml2.core.Response
import org.opensaml.saml.saml2.core.Subject
import org.opensaml.saml.saml2.core.Assertion
import org.opensaml.saml.saml2.core.NameIDPolicy
import org.opensaml.saml.saml2.core.NameID

class NameQualifierPresignPluginSpec extends Specification {

    NameQualifierPresignPlugin plugin = new NameQualifierPresignPlugin()

    Issuer issuer = Mock(Issuer)
    NameID nameID = Mock(NameID)
    Subject subject = Mock(Subject)
    NameID nameIDNeg = Mock(NameID)
    Response response = Mock(Response)
    Assertion assertion = Mock(Assertion)
    AuthnRequest authNReq = Mock(AuthnRequest)
    NameIDPolicy nameIdPolicy = Mock(NameIDPolicy)

    List<String> spMetadata = new ArrayList<>()
    List<Assertion> assertions = new ArrayList<>()
    Set<SamlProtocol.Binding> bindings = new HashSet<>()

    def NEGATIVE_TEST = 'negative test'

    def setupSpec() {
        OpenSAMLUtil.initSamlEngine()
    }

    def setup() {
        authNReq.getIssuer() >> issuer
        assertion.getIssuer() >> issuer
        response.assertions >> assertions
        authNReq.getNameIDPolicy() >> nameIdPolicy
    }

    def 'test name qualifier when issuer is not null and nameid format is persistent'() {
        setup:
        response.getIssuer() >> issuer
        issuer.getFormat() >> PERSISTENT
        assertions.add(assertion)
        assertion.getSubject() >> subject
        subject.getNameID() >> nameID
        nameID.getFormat() >> PERSISTENT

        when:
        plugin.processPresign(response, authNReq, spMetadata, bindings)

        then:
        1 * nameID.setNameQualifier(NameQualifierPresignPlugin.NAME_QUALIFIER)
        2 * issuer.setNameQualifier(NameQualifierPresignPlugin.NAME_QUALIFIER)
    }

    def 'test name qualifier when issuer is null and nameid format is persistent'() {
        setup:
        response.getIssuer() >> null
        assertions.add(assertion)
        assertion.getSubject() >> subject
        subject.getNameID() >> nameID
        nameID.getFormat() >> PERSISTENT

        when:
        plugin.processPresign(response, authNReq, spMetadata, bindings)

        then:
        1 * nameID.setNameQualifier(NameQualifierPresignPlugin.NAME_QUALIFIER)
        0 * issuer.setNameQualifier(NameQualifierPresignPlugin.NAME_QUALIFIER)
    }

    def 'test name qualifier when no assertions'() {
        setup:
        response.getIssuer() >> issuer
        issuer.getFormat() >> PERSISTENT
        assertion.getSubject() >> subject
        subject.getNameID() >> nameID
        nameID.getFormat() >> PERSISTENT

        when:
        plugin.processPresign(response, authNReq, spMetadata, bindings)

        then:
        0 * nameID.setNameQualifier(NameQualifierPresignPlugin.NAME_QUALIFIER)
        1 * issuer.setNameQualifier(NameQualifierPresignPlugin.NAME_QUALIFIER)
    }

    def 'test name qualifier when issuer format is null'() {
        setup:
        response.getIssuer() >> issuer
        issuer.getFormat() >> null
        assertions.add(assertion)

        when:
        plugin.processPresign(response, authNReq, spMetadata, bindings)

        then:
        0 * issuer.setNameQualifier(NameQualifierPresignPlugin.NAME_QUALIFIER)
    }

    def 'test name qualifier when issuer format is not persistent'() {
        setup:
        response.getIssuer() >> issuer
        issuer.getFormat() >> NEGATIVE_TEST
        assertions.add(assertion)

        when:
        plugin.processPresign(response, authNReq, spMetadata, bindings)

        then:
        0 * issuer.setNameQualifier(NameQualifierPresignPlugin.NAME_QUALIFIER)
    }

    def 'test name qualifier when assertion subject is null'() {
        setup:
        response.getIssuer() >> issuer
        issuer.getFormat() >> PERSISTENT
        assertions.add(assertion)
        assertion.getSubject() >> null

        when:
        plugin.processPresign(response, authNReq, spMetadata, bindings)

        then:
        0 * nameID.setNameQualifier(NameQualifierPresignPlugin.NAME_QUALIFIER)
    }

    def 'test name qualifier when assertion subject is not null and name id is not persistent'() {
        setup:
        response.getIssuer() >> issuer
        issuer.getFormat() >> PERSISTENT
        assertions.add(assertion)
        assertion.getSubject() >> subject
        subject.getNameID() >> nameIDNeg
        nameIDNeg.getFormat() >> NEGATIVE_TEST

        when:
        plugin.processPresign(response, authNReq, spMetadata, bindings)

        then:
        0 * nameID.setNameQualifier(NameQualifierPresignPlugin.NAME_QUALIFIER)
        2 * issuer.setNameQualifier(NameQualifierPresignPlugin.NAME_QUALIFIER)
    }

    def 'test name qualifier when assertion subject is not null and name id is null'() {
        setup:
        response.getIssuer() >> issuer
        issuer.getFormat() >> PERSISTENT
        assertions.add(assertion)
        assertion.getSubject() >> subject
        subject.getNameID() >> null

        when:
        plugin.processPresign(response, authNReq, spMetadata, bindings)

        then:
        0 * nameID.setNameQualifier(NameQualifierPresignPlugin.NAME_QUALIFIER)
        2 * issuer.setNameQualifier(NameQualifierPresignPlugin.NAME_QUALIFIER)
    }
}