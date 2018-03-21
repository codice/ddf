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
package ddf.security.samlp.impl

import ddf.security.samlp.SamlProtocol
import ddf.security.samlp.SimpleSign
import org.opensaml.saml.saml2.core.ArtifactResponse
import org.opensaml.saml.saml2.core.LogoutRequest
import org.opensaml.saml.saml2.core.LogoutResponse
import spock.lang.Specification

import java.time.Duration

class SamlValidatorBuilderSpec extends Specification {
    def 'check simpleSign'() {
        setup:
        def simpleSign = Mock(SimpleSign)

        when:
        def builder = new SamlValidator.Builder(simpleSign)

        then:
        builder.simpleSign == simpleSign
    }

    def 'check redirectParams'() {
        setup:
        def builder = new SamlValidator.Builder(null)

        when:
        builder.setRedirectParams(relayState, signature, sigAlgo, samlString, signingCert)

        then:
        builder.relayState == relayState
        builder.signature == signature
        builder.sigAlgo == sigAlgo
        builder.samlString == samlString
        builder.signingCertificate == signingCert

        where:
        relayState | signature | sigAlgo | samlString | signingCert
        null       | null      | null    | null       | null
        null       | 'abc'     | 'def'   | null       | 'xyz'
        'abc'      | 'def'     | 'xxx'   | 'aaa'      | 'zzz'
        'abc'      | null      | null    | 'lkj'      | null
    }

    def 'check requestId'() {
        setup:
        def builder = new SamlValidator.Builder(null)

        when:
        builder.setRequestId('xyz')

        then:
        builder.requestId == 'xyz'

        when:
        builder.setRequestId(null)

        then:
        thrown(IllegalArgumentException)
    }

    def 'check timeout'() {
        setup:
        def builder = new SamlValidator.Builder(null)

        when:
        builder.setTimeout(Duration.ofSeconds(1))

        then:
        builder.timeout.equals(Duration.ofSeconds(1))

        when:
        builder.setTimeout(null)

        then:
        thrown(IllegalArgumentException)
    }

    def 'check jitter'() {
        setup:
        def builder = new SamlValidator.Builder(null)

        when:
        builder.setClockSkew(Duration.ofSeconds(1))

        then:
        builder.clockSkew.equals(Duration.ofSeconds(1))

        when:
        builder.setClockSkew(null)

        then:
        thrown(IllegalArgumentException)
    }

    def 'check not-null endpoint'() {
        setup:
        def builder = new SamlValidator.Builder(null)
        def logoutRequest = Mock(LogoutRequest)

        when:
        builder.build(null, SamlProtocol.Binding.HTTP_POST, logoutRequest)

        then:
        thrown(IllegalArgumentException)

        when:
        builder.build('', SamlProtocol.Binding.HTTP_POST, logoutRequest)

        then:
        thrown(IllegalArgumentException)
    }

    def 'check not-null binding'() {
        setup:
        def builder = new SamlValidator.Builder(null)
        def logoutRequest = Mock(LogoutRequest)

        when:
        builder.build('xxx', null, logoutRequest)

        then:
        thrown(IllegalArgumentException)
    }

    def 'check valid xmlObject types'() {
        setup:
        def builder = new SamlValidator.Builder(null)
        def mock

        when:
        mock = Mock(LogoutRequest)
        builder.build('zzz', SamlProtocol.Binding.HTTP_POST, mock)

        then:
        builder.xmlObject == mock

        when:
        mock = Mock(LogoutResponse)
        builder.build('zzz', SamlProtocol.Binding.HTTP_POST, mock)

        then:
        builder.xmlObject == mock

        when:
        mock = Mock(ArtifactResponse)
        builder.build('zzz', SamlProtocol.Binding.HTTP_POST, mock)

        then:
        thrown(IllegalArgumentException)
    }

    def 'test buildValidator for post-request'() {
        setup:
        def builder = new SamlValidator.Builder(null)
        def xmlObject = Mock(LogoutRequest)

        when:
        def samlValidator = builder.build('xxx', SamlProtocol.Binding.HTTP_POST, xmlObject)

        then:
        builder.isRequest
        builder.xmlObject == xmlObject
        samlValidator instanceof SamlValidator.PostRequest
    }

    def 'test buildValidator for post-response'() {
        setup:
        def builder = new SamlValidator.Builder(null)
        def xmlObject = Mock(LogoutResponse)

        when:
        def samlValidator = builder.build('xxx', SamlProtocol.Binding.HTTP_POST, xmlObject)

        then:
        !builder.isRequest
        builder.xmlObject == xmlObject
        samlValidator instanceof SamlValidator.PostResponse
    }

    def 'test buildValidator with unsupported xmlObject'() {
        setup:
        def builder = new SamlValidator.Builder(null)
        def xmlObject = Mock(ArtifactResponse)

        when:
        builder.build('xxx', SamlProtocol.Binding.HTTP_POST, xmlObject)

        then:
        thrown(IllegalArgumentException)
    }

    def 'test buildValidator for redirect with missing params'() {
        setup:
        def builder = new SamlValidator.Builder(null)
        def xmlObject = Mock(LogoutRequest)

        when:
        builder.build('xxx', SamlProtocol.Binding.HTTP_REDIRECT, xmlObject)

        then:
        thrown(UnsupportedOperationException)

        when:
        builder.setRedirectParams('xxx', null, 'xxx', 'xxx', 'xxx')
        builder.build('xxx', SamlProtocol.Binding.HTTP_REDIRECT, xmlObject)

        then:
        thrown(UnsupportedOperationException)

        when:
        builder.setRedirectParams('xxx', 'xxx', null, 'xxx', 'xxx')
        builder.build('xxx', SamlProtocol.Binding.HTTP_REDIRECT, xmlObject)

        then:
        thrown(UnsupportedOperationException)

        when:
        builder.setRedirectParams('xxx', 'xxx', 'xxx', null, 'xxx')
        builder.build('xxx', SamlProtocol.Binding.HTTP_REDIRECT, xmlObject)

        then:
        thrown(UnsupportedOperationException)

        when:
        builder.setRedirectParams('xxx', 'xxx', 'xxx', 'xxx', null)
        builder.build('xxx', SamlProtocol.Binding.HTTP_REDIRECT, xmlObject)

        then:
        thrown(UnsupportedOperationException)

        when: 'relayState is allowed to be null'
        builder.setRedirectParams(null, 'xxx', 'xxx', 'xxx', 'xxx')
        def samlValidator = builder.build('xxx', SamlProtocol.Binding.HTTP_REDIRECT, xmlObject)

        then:
        builder.isRequest
        builder.xmlObject == xmlObject
        samlValidator instanceof SamlValidator.RedirectRequest
    }

    def 'test buildValidator for redirect-request'() {
        setup:
        def builder = new SamlValidator.Builder(null)
        def xmlObject = Mock(LogoutRequest)

        when:
        builder.build('xxx', SamlProtocol.Binding.HTTP_REDIRECT, xmlObject)

        then:
        thrown(UnsupportedOperationException)

        when:
        builder.setRedirectParams('xxx', 'xxx', 'xxx', 'xxx', 'xxx')
        def samlValidator = builder.build('xxx', SamlProtocol.Binding.HTTP_REDIRECT, xmlObject)

        then:
        builder.isRequest
        builder.xmlObject == xmlObject
        samlValidator instanceof SamlValidator.RedirectRequest
    }

    def 'test buildValidator for redirect-response'() {
        setup:
        def builder = new SamlValidator.Builder(null)
        def xmlObject = Mock(LogoutResponse)

        when:
        builder.build('xxx', SamlProtocol.Binding.HTTP_REDIRECT, xmlObject)

        then:
        thrown(UnsupportedOperationException)

        when:
        builder.setRedirectParams('xxx', 'xxx', 'xxx', 'xxx', 'xxx')
        def samlValidator = builder.build('xxx', SamlProtocol.Binding.HTTP_REDIRECT, xmlObject)

        then:
        !builder.isRequest
        builder.xmlObject == xmlObject
        samlValidator instanceof SamlValidator.RedirectResponse
    }

    def 'test buildValidator with unsupported binding'() {
        setup:
        def builder = new SamlValidator.Builder(null)
        def xmlObject = Mock(LogoutRequest)

        when:
        builder.build('xxx', SamlProtocol.Binding.HTTP_ARTIFACT, xmlObject)

        then:
        thrown(UnsupportedOperationException)
    }
}
