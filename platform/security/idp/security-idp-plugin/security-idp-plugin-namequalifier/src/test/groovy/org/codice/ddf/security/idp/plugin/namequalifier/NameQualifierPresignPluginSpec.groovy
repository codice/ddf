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

    // general mocks
    AuthnRequest authNReq = Mock(AuthnRequest)
    Issuer issuer = Mock(Issuer)

    List<String> spMetadata = new ArrayList<>()
    Set<SamlProtocol.Binding> bindings = new HashSet<>()

    // set of mocks and concrete objects
    Response response = Mock(Response)

    List<Assertion> assertions = new ArrayList<>()
    Assertion assertion = Mock(Assertion)

    Subject subject = Mock(Subject)
    NameID nameID = Mock(NameID)
    NameID nameIDNeg = Mock(NameID)

    NameIDPolicy nameIdPolicy = Mock(NameIDPolicy)

    def NEGATIVE_TEST = 'negative test'

    def setupSpec() {
        OpenSAMLUtil.initSamlEngine()
    }

    def setup() {

        // set up authNReq
        authNReq.getIssuer() >> issuer

        // set up subject
        assertion.getIssuer() >> issuer

        // set up basic response
        response.assertions >> assertions

        // set up the name id policy
        authNReq.getNameIDPolicy() >> nameIdPolicy;

    }

    def 'test name qualifier when issuer is not null and nameid format is persistent'() {

        setup:
        response.getIssuer() >> issuer
        issuer.getFormat() >> NameQualifierPresignPlugin.PERSISTENT
        assertions.add(assertion)
        assertion.getSubject() >> subject
        subject.getNameID() >> nameID
        nameID.getFormat() >> NameQualifierPresignPlugin.PERSISTENT

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
        nameID.getFormat() >> NameQualifierPresignPlugin.PERSISTENT

        when:
        plugin.processPresign(response, authNReq, spMetadata, bindings)

        then:
        1 * nameID.setNameQualifier(NameQualifierPresignPlugin.NAME_QUALIFIER)
        0 * issuer.setNameQualifier(NameQualifierPresignPlugin.NAME_QUALIFIER)

    }

    def 'test name qualifier when no assertions'() {

        setup:
        response.getIssuer() >> issuer
        issuer.getFormat() >> NameQualifierPresignPlugin.PERSISTENT
        assertion.getSubject() >> subject
        subject.getNameID() >> nameID
        nameID.getFormat() >> NameQualifierPresignPlugin.PERSISTENT

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
        issuer.getFormat() >>  NEGATIVE_TEST
        assertions.add(assertion)

        when:
        plugin.processPresign(response, authNReq, spMetadata, bindings)

        then:
        0 * issuer.setNameQualifier(NameQualifierPresignPlugin.NAME_QUALIFIER)

    }

    def 'test name qualifier when assertion subject is null'() {

        setup:
        response.getIssuer() >> issuer
        issuer.getFormat() >> NameQualifierPresignPlugin.PERSISTENT
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
        issuer.getFormat() >> NameQualifierPresignPlugin.PERSISTENT
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
        issuer.getFormat() >> NameQualifierPresignPlugin.PERSISTENT
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