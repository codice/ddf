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

    @Unroll("null NameIDPolicy Format tests for Format #format")
    def 'null NameIDPolicy Format tests'() {
        setup:
        nameIdMock.setFormat(format)
        nameIdPolicyMock.getFormat() >> policyFormat

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        if (!isError) {
            assert (nameIdMock.value == EXAMPLE_NAME_ID_VALUE)
            return
        }
        thrown(UnsupportedOperationException)

        where:
        policyFormat | format              | isError
        null         | null                | false
        null         | NameID.UNSPECIFIED  | false
        null         | NameID.PERSISTENT   | false
        null         | NameID.TRANSIENT    | false
        null         | NameID.X509_SUBJECT | false
        null         | NameID.EMAIL        | false
    }

    @Unroll("unspecified NameIDPolicy Format tests for Format #format")
    def 'unspecified NameIDPolicy Format tests'() {
        setup:
        nameIdMock.setFormat(format)
        nameIdPolicyMock.getFormat() >> policyFormat

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        if (!isError) {
            assert (nameIdMock.value == EXAMPLE_NAME_ID_VALUE)
            return
        }
        thrown(UnsupportedOperationException)

        where:
        policyFormat       | format              | isError
        NameID.UNSPECIFIED | null                | false
        NameID.UNSPECIFIED | NameID.UNSPECIFIED  | false
        NameID.UNSPECIFIED | NameID.PERSISTENT   | false
        NameID.UNSPECIFIED | NameID.TRANSIENT    | false
        NameID.UNSPECIFIED | NameID.X509_SUBJECT | false
        NameID.UNSPECIFIED | NameID.EMAIL        | false
    }

    @Unroll("persistent NameIDPolicy Format tests for Format #format")
    def 'persistent NameIDPolicy Format tests'() {
        setup:
        nameIdMock.setFormat(format)
        nameIdPolicyMock.getFormat() >> policyFormat

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        if (!isError) {
            assert (nameIdMock.value == EXAMPLE_NAME_ID_VALUE && nameIdMock.format == policyFormat)
            return
        }
        thrown(UnsupportedOperationException)

        where:
        policyFormat      | format              | isError
        NameID.PERSISTENT | null                | false
        NameID.PERSISTENT | NameID.UNSPECIFIED  | false
        NameID.PERSISTENT | NameID.PERSISTENT   | false
        NameID.PERSISTENT | NameID.TRANSIENT    | false
        NameID.PERSISTENT | NameID.X509_SUBJECT | false
        NameID.PERSISTENT | NameID.EMAIL        | false
    }

    @Unroll("transient NameIDPolicy Format tests for Format #format")
    def 'transient NameIDPolicy Format tests'() {
        setup:
        nameIdMock.setFormat(format)
        nameIdPolicyMock.getFormat() >> policyFormat

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        if (!isError) {
            assert (nameIdMock.value == EXAMPLE_NAME_ID_VALUE && nameIdMock.format == policyFormat)
            return
        }
        thrown(UnsupportedOperationException)

        where:
        policyFormat     | format              | isError
        NameID.TRANSIENT | null                | false
        NameID.TRANSIENT | NameID.UNSPECIFIED  | false
        NameID.TRANSIENT | NameID.PERSISTENT   | false
        NameID.TRANSIENT | NameID.TRANSIENT    | false
        NameID.TRANSIENT | NameID.X509_SUBJECT | false
        NameID.TRANSIENT | NameID.EMAIL        | false
    }

    @Unroll("x509 NameIDPolicy Format tests for Format #format")
    def 'x509 NameIDPolicy Format tests'() {
        setup:
        nameIdMock.setFormat(format)
        nameIdPolicyMock.getFormat() >> policyFormat

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        if (!isError) {
            assert (nameIdMock.value == EXAMPLE_NAME_ID_VALUE && nameIdMock.format == policyFormat)
            return
        }
        thrown(UnsupportedOperationException)

        where:
        policyFormat        | format              | isError
        NameID.X509_SUBJECT | null                | true
        NameID.X509_SUBJECT | NameID.UNSPECIFIED  | true
        NameID.X509_SUBJECT | NameID.PERSISTENT   | true
        NameID.X509_SUBJECT | NameID.TRANSIENT    | true
        NameID.X509_SUBJECT | NameID.X509_SUBJECT | false
        NameID.X509_SUBJECT | NameID.EMAIL        | true
    }

    @Unroll("email NameIDPolicy Format tests for Format #format")
    def 'email NameIDPolicy Format tests'() {
        setup:
        nameIdMock.setFormat(format)
        nameIdPolicyMock.getFormat() >> policyFormat

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        if (!isError) {
            assert (nameIdMock.value == EXAMPLE_EMAIL && nameIdMock.format == policyFormat)
            return
        }
        thrown(UnsupportedOperationException)

        where:
        policyFormat | format              | isError
        NameID.EMAIL | null                | false
        NameID.EMAIL | NameID.UNSPECIFIED  | false
        NameID.EMAIL | NameID.PERSISTENT   | false
        NameID.EMAIL | NameID.TRANSIENT    | false
        NameID.EMAIL | NameID.X509_SUBJECT | false
        NameID.EMAIL | NameID.EMAIL        | false
    }

    def 'email NameIDPolicy Format null email attribute'() {
        setup:
        nameIdPolicyMock.getFormat() >> NameID.EMAIL
        responseMock.assertions.first().attributeStatements.first().attributes.clear()

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        thrown(UnsupportedOperationException)
    }

    def 'email NameIDPolicy Format empty email attribute'() {
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

    def 'email NameIDPolicy Format incorrect email attribute name/format'() {
        setup:
        nameIdPolicyMock.getFormat() >> NameID.EMAIL
        emailAttribute.setName(INCORRECT_VALUE)
        emailAttribute.setNameFormat(INCORRECT_VALUE)

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        thrown(UnsupportedOperationException)
    }

    def 'email NameIDPolicy Format correct email attribute name'() {
        setup:
        nameIdPolicyMock.getFormat() >> NameID.EMAIL
        emailAttribute.setNameFormat(INCORRECT_VALUE)

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        assert (nameIdMock.value == EXAMPLE_EMAIL && nameIdMock.format == nameIdPolicyMock.format)
    }

    def 'email NameIDPolicy Format correct email attribute format'() {
        setup:
        nameIdPolicyMock.getFormat() >> NameID.EMAIL
        emailAttribute.setName(INCORRECT_VALUE)

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        assert (nameIdMock.value == EXAMPLE_EMAIL && nameIdMock.format == nameIdPolicyMock.format)
    }

    @Unroll("windows domain qualified NameIDPolicy Format tests for Format #format")
    def 'windows domain qualified NameIDPolicy Format tests'() {
        setup:
        nameIdMock.setFormat(format)
        nameIdPolicyMock.getFormat() >> policyFormat

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        if (!isError) {
            assert (nameIdMock.value == EXAMPLE_NAME_ID_VALUE && nameIdMock.format == policyFormat)
            return
        }
        thrown(UnsupportedOperationException)

        where:
        policyFormat                | format              | isError
        NameID.WIN_DOMAIN_QUALIFIED | null                | true
        NameID.WIN_DOMAIN_QUALIFIED | NameID.UNSPECIFIED  | true
        NameID.WIN_DOMAIN_QUALIFIED | NameID.PERSISTENT   | true
        NameID.WIN_DOMAIN_QUALIFIED | NameID.TRANSIENT    | true
        NameID.WIN_DOMAIN_QUALIFIED | NameID.X509_SUBJECT | true
        NameID.WIN_DOMAIN_QUALIFIED | NameID.EMAIL        | true
    }

    @Unroll("kerberos NameIDPolicy Format tests for Format #format")
    def 'kerberos NameIDPolicy Format tests'() {
        setup:
        nameIdMock.setFormat(format)
        nameIdPolicyMock.getFormat() >> policyFormat

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        if (!isError) {
            assert (nameIdMock.value == EXAMPLE_NAME_ID_VALUE && nameIdMock.format == policyFormat)
            return
        }
        thrown(UnsupportedOperationException)

        where:
        policyFormat    | format              | isError
        NameID.KERBEROS | null                | true
        NameID.KERBEROS | NameID.UNSPECIFIED  | true
        NameID.KERBEROS | NameID.PERSISTENT   | true
        NameID.KERBEROS | NameID.TRANSIENT    | true
        NameID.KERBEROS | NameID.X509_SUBJECT | true
        NameID.KERBEROS | NameID.EMAIL        | true
    }

    @Unroll("entity NameIDPolicy Format tests for Format #format")
    def 'entity NameIDPolicy Format tests'() {
        setup:
        nameIdMock.setFormat(format)
        nameIdPolicyMock.getFormat() >> policyFormat

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        if (!isError) {
            assert (nameIdMock.value == EXAMPLE_NAME_ID_VALUE && nameIdMock.format == policyFormat)
            return
        }
        thrown(UnsupportedOperationException)

        where:
        policyFormat  | format              | isError
        NameID.ENTITY | null                | true
        NameID.ENTITY | NameID.UNSPECIFIED  | true
        NameID.ENTITY | NameID.PERSISTENT   | true
        NameID.ENTITY | NameID.TRANSIENT    | true
        NameID.ENTITY | NameID.X509_SUBJECT | true
        NameID.ENTITY | NameID.EMAIL        | true
    }

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

    def 'null NameIDPolicy'() {
        setup:
        authnRequestMock.getNameIDPolicy() >> null

        when:
        plugin.processPresign(responseMock, authnRequestMock, spMetadataMock, supportedBindingsMock)

        then:
        0 * responseMock.any()
    }

    def 'null/empty NameIDPolicy Format/SpNameQualifier'() {
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
