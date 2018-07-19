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
 **/
package org.codice.ddf.security.idp.plugin.nameidpolicy

import org.apache.cxf.staxutils.StaxUtils
import org.apache.wss4j.common.saml.OpenSAMLUtil
import org.opensaml.core.xml.XMLObject
import org.opensaml.saml.saml2.core.Attribute
import org.opensaml.saml.saml2.core.AuthnRequest
import org.opensaml.saml.saml2.core.NameID
import org.opensaml.saml.saml2.core.NameIDPolicy
import org.opensaml.saml.saml2.core.Response
import org.w3c.dom.Document
import spock.lang.Specification
import spock.lang.Unroll

class NameIdPolicyPresignPluginSpec extends Specification {
    def static INCORRECT_VALUE = "incorrect"
    def static EXAMPLE_SP_NAME_QUALIFIER = "https://example-ddf-sp.com"
    def static EXAMPLE_NAME_ID_VALUE = "admin"
    def static EXAMPLE_EMAIL = "admin@localhost"

    def static NAME_IDENTIFIER_URI =
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"
    def static EMAIL_URI = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"

    def plugin = new NameIdPolicyPresignPlugin()

    Response responseMock
    NameID nameIdMock
    Attribute emailAttribute

    def authnRequestMock = Mock(AuthnRequest)
    def nameIdPolicyMock = Mock(NameIDPolicy)
    def spMetadataMock = new ArrayList<>()
    def supportedBindingsMock = new HashSet<>()

    def setupSpec() {
        OpenSAMLUtil.initSamlEngine()
    }

    def setup() {
        def cannedResponse = this.getClass().getResource('/sampleResponse.xml').text
        Document responseDoc = StaxUtils.read(new ByteArrayInputStream(cannedResponse.getBytes()))
        XMLObject responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement())
        responseMock = (Response) responseXmlObject
        nameIdMock = responseMock.assertions.first().subject.nameID
        nameIdMock.setValue(EXAMPLE_NAME_ID_VALUE)
        nameIdMock.setFormat(NameID.UNSPECIFIED)

        emailAttribute =
                responseMock.assertions.first().attributeStatements.first().attributes.first()

        authnRequestMock.getNameIDPolicy() >> nameIdPolicyMock
    }

    @Unroll("test a null NameIDPolicy Format against an original NameID Format of #format")
    def 'null NameIDPolicy Format tests'() {
        setup:
        nameIdMock.setFormat(format)
        nameIdPolicyMock.getFormat() >> null

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        assert (nameIdMock.value == EXAMPLE_NAME_ID_VALUE && nameIdMock.format == format)

        where:
        format << [null,
                   NameID.UNSPECIFIED,
                   NameID.PERSISTENT,
                   NameID.X509_SUBJECT]
    }

    @Unroll("test an unspecified NameIDPolicy Format against an original NameID Format of #format")
    def 'unspecified NameIDPolicy Format tests'() {
        setup:
        nameIdMock.setFormat(format)
        nameIdPolicyMock.getFormat() >> NameID.UNSPECIFIED

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        assert (nameIdMock.value == EXAMPLE_NAME_ID_VALUE && nameIdMock.format == format)

        where:
        format << [null,
                   NameID.UNSPECIFIED,
                   NameID.PERSISTENT,
                   NameID.X509_SUBJECT,
                   NameID.EMAIL]
    }

    @Unroll("test a persistent NameIDPolicy Format against an original NameID Format of #format")
    def 'persistent NameIDPolicy Format tests'() {
        setup:
        nameIdMock.setFormat(format)
        nameIdPolicyMock.getFormat() >> NameID.PERSISTENT

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        assert (nameIdMock.value == EXAMPLE_NAME_ID_VALUE && nameIdMock.format == NameID.PERSISTENT)

        where:
        format << [null,
                   NameID.UNSPECIFIED,
                   NameID.PERSISTENT,
                   NameID.X509_SUBJECT,
                   NameID.EMAIL]
    }

    @Unroll("test a null NameIDPolicy Format against an original NameID Format of #format")
    def 'x509 NameIDPolicy Format tests'() {
        setup:
        nameIdMock.setFormat(format)
        nameIdPolicyMock.getFormat() >> NameID.X509_SUBJECT

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        if (!isError) {
            assert (nameIdMock.value == EXAMPLE_NAME_ID_VALUE
                    && nameIdMock.format == NameID.X509_SUBJECT)
            return
        }
        thrown(UnsupportedOperationException)

        where:
        format              | isError
        null                | true
        NameID.UNSPECIFIED  | true
        NameID.PERSISTENT   | true
        NameID.X509_SUBJECT | false
        NameID.EMAIL        | true
    }

    @Unroll("test an email NameIDPolicy Format against an original NameID Format of #format")
    def 'email NameIDPolicy Format tests'() {
        setup:
        nameIdMock.setFormat(format)
        nameIdPolicyMock.getFormat() >> NameID.EMAIL

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        assert (nameIdMock.value == EXAMPLE_EMAIL && nameIdMock.format == NameID.EMAIL)

        where:
        format << [null,
                   NameID.UNSPECIFIED,
                   NameID.PERSISTENT,
                   NameID.X509_SUBJECT,
                   NameID.EMAIL]
    }

    def 'test an email NameIDPolicy Format against an assertion with no attribute statements'() {
        setup:
        nameIdPolicyMock.getFormat() >> NameID.EMAIL
        responseMock.assertions.first().attributeStatements.first().attributes.clear()

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        thrown(UnsupportedOperationException)
    }

    def 'test an email NameIDPolicy Format against an assertion with no email attribute statement'() {
        setup:
        nameIdPolicyMock.getFormat() >> NameID.EMAIL
        emailAttribute.getAttributeValues().clear()
        emailAttribute.name = null
        emailAttribute.nameFormat = null

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        thrown(UnsupportedOperationException)
    }

    def 'test an email NameIDPolicy Format against an assertion with a correctly named email attribute statement'() {
        setup:
        nameIdPolicyMock.getFormat() >> NameID.EMAIL
        emailAttribute.setNameFormat(INCORRECT_VALUE)

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        assert (nameIdMock.value == EXAMPLE_EMAIL && nameIdMock.format == nameIdPolicyMock.format)
    }

    def 'test an email NameIDPolicy Format against an assertion with a correctly name formatted email attribute statement'() {
        setup:
        nameIdPolicyMock.getFormat() >> NameID.EMAIL
        emailAttribute.setName(INCORRECT_VALUE)

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        assert (nameIdMock.value == EXAMPLE_EMAIL && nameIdMock.format == nameIdPolicyMock.format)
    }

    @Unroll('test a #policySpNameQualifier NameIDPolicy SPNameQualifier against an original #spNameQualifier NameID SPNameQualifier')
    def 'SPNameQualifier tests'() {
        setup:
        nameIdMock.setSPNameQualifier(spNameQualifier)
        nameIdPolicyMock.getSPNameQualifier() >> policySpNameQualifier

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        if (policySpNameQualifier == null) {
            nameIdMock.getSPNameQualifier() == spNameQualifier
        } else {
            nameIdMock.getSPNameQualifier() == policySpNameQualifier
        }

        where:
        policySpNameQualifier     | spNameQualifier
        null                      | null
        null                      | EXAMPLE_SP_NAME_QUALIFIER
        EXAMPLE_SP_NAME_QUALIFIER | null
        EXAMPLE_SP_NAME_QUALIFIER | EXAMPLE_SP_NAME_QUALIFIER
    }

    def 'null NameIDPolicy test'() {
        setup:
        authnRequestMock.getNameIDPolicy() >> null

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        0 * responseMock.any()
    }

    def 'null/empty NameIDPolicy Format/SpNameQualifier test'() {
        setup:
        nameIdPolicyMock.getFormat() >> policyFormat
        nameIdPolicyMock.getSPNameQualifier() >> policySpNameQualifier

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        0 * responseMock.any()

        where:
        policyFormat | policySpNameQualifier
        null         | null
        ""           | ""
        null         | ""
        ""           | null
    }
}
