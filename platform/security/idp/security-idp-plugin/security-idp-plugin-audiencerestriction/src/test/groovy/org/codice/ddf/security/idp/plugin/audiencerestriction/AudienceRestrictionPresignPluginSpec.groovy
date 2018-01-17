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
package org.codice.ddf.security.idp.plugin.audiencerestriction

import ddf.security.samlp.SamlProtocol
import org.apache.wss4j.common.saml.OpenSAMLUtil
import org.opensaml.saml.saml2.core.Assertion
import org.opensaml.saml.saml2.core.Audience
import org.opensaml.saml.saml2.core.AudienceRestriction
import org.opensaml.saml.saml2.core.AuthnRequest
import org.opensaml.saml.saml2.core.Conditions
import org.opensaml.saml.saml2.core.Issuer
import org.opensaml.saml.saml2.core.Response
import spock.lang.Specification

class AudienceRestrictionPresignPluginSpec extends Specification {
    def ISSUER_ID = "https://localhost:8993/services/idp/login"

    AudienceRestrictionPresignPlugin plugin = new AudienceRestrictionPresignPlugin()

    // general mocks
    AuthnRequest authNReq = Mock(AuthnRequest)
    Issuer issuer = Mock(Issuer)

    List<String> spMetadata = new ArrayList<>()
    Set<SamlProtocol.Binding> bindings = new HashSet<>()

    // set of mocks and concrete objects
    Response response = Mock(Response)
    List<Assertion> assertions = new ArrayList<>()
    Assertion assertion = Mock(Assertion)
    Conditions conditions = Mock(Conditions)
    List<AudienceRestriction> audienceRestrictions = new ArrayList<>()

    AudienceRestriction audienceRestriction = Mock(AudienceRestriction)
    AudienceRestriction audienceRestriction2 = Mock(AudienceRestriction)
    AudienceRestriction emptyAudienceRestriction = Mock(AudienceRestriction)

    List<Audience> audiences = new ArrayList<>()
    List<Audience> audiences2 = new ArrayList<>()
    List<Audience> emptyAudiences = new ArrayList<>()

    Audience audience = Mock(Audience)
    Audience audience2 = Mock(Audience)

    def setupSpec() {
        OpenSAMLUtil.initSamlEngine()
    }

    def setup() {
        // set up authNReq
        authNReq.getIssuer() >> issuer
        issuer.getValue() >> ISSUER_ID

        // set up basic response
        response.assertions >> assertions
        assertions.add(assertion)
        assertion.conditions >> conditions
        // audienceRestrictions is empty after setup
        conditions.audienceRestrictions >> audienceRestrictions

        // set up audienceURIs
        audience.audienceURI >> "https://localhost:8993/services/SecurityTokenService?wsdl"
        audience2.audienceURI >> "https://localhost:8993/admin"

        // add the audience to the audiences
        audiences.add(audience)
        audiences.add(audience2)
        audiences2.add(audience)

        // stub the audiences to the audienceRestriction
        audienceRestriction.audiences >> audiences
        audienceRestriction2.audiences >> audiences2
        emptyAudienceRestriction.audiences >> emptyAudiences
    }

    def 'add audience to non existent audience restriction'() {
        when:
        plugin.processPresign(response, authNReq, spMetadata, bindings)
        def audienceRestriction = response.assertions[0].conditions.audienceRestrictions[0]

        then:
        audienceRestriction.audiences.size() == 1
        audienceRestriction.audiences[0].audienceURI == ISSUER_ID
    }

    def 'add audience to empty audience restriction'() {
        setup:
        audienceRestrictions.add(emptyAudienceRestriction)

        when:
        plugin.processPresign(response, authNReq, spMetadata, bindings)
        def audienceRestriction = response.assertions[0].conditions.audienceRestrictions[0]

        then:
        audienceRestriction.audiences.size() == 1
        audienceRestriction.audiences[0].audienceURI == ISSUER_ID
    }

    def 'add audience to audience restriction with existing audiences'() {
        setup:
        audienceRestrictions.add(this.audienceRestriction)

        when:
        plugin.processPresign(response, authNReq, spMetadata, bindings)
        def audienceRestriction = response.assertions[0].conditions.audienceRestrictions[0]

        then:
        audienceRestriction.audiences.size() == 3
        audienceRestriction.audiences*.audienceURI.count { it == ISSUER_ID } == 1
    }

    def 'add audience to multiple audience restrictions'() {
        setup:
        audienceRestrictions.add(this.audienceRestriction)
        audienceRestrictions.add(this.audienceRestriction2)
        audienceRestrictions.add(this.emptyAudienceRestriction)

        when:
        plugin.processPresign(response, authNReq, spMetadata, bindings)
        def audienceRestrictions = response.assertions[0].conditions.audienceRestrictions

        then:
        def audienceRestrictionSize3 = audienceRestrictions*.audiences.findAll() { it.size() == 3 }
        audienceRestrictionSize3.size() == 1
        audienceRestrictionSize3[0]*.audienceURI.count { it == ISSUER_ID } == 1

        def audienceRestrictionSize2 = audienceRestrictions*.audiences.findAll() { it.size() == 2 }
        audienceRestrictionSize2.size() == 1
        audienceRestrictionSize2[0]*.audienceURI.count { it == ISSUER_ID } == 1

        def audienceRestrictionSize1 = audienceRestrictions*.audiences.findAll() { it.size() == 1 }
        audienceRestrictionSize3.size() == 1
        audienceRestrictionSize1[0]*.audienceURI.count { it == ISSUER_ID } == 1
    }
}
