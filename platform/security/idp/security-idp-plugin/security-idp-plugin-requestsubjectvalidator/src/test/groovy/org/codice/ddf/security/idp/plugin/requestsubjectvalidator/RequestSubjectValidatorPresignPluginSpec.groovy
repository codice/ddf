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
package org.codice.ddf.security.idp.plugin.requestsubjectvalidator

import ddf.security.samlp.SamlProtocol
import org.apache.cxf.staxutils.StaxUtils
import org.apache.wss4j.common.saml.OpenSAMLUtil
import org.opensaml.saml.common.SAMLRuntimeException
import org.opensaml.saml.saml2.core.AuthnRequest
import org.opensaml.saml.saml2.core.NameID
import org.opensaml.saml.saml2.core.Response
import org.opensaml.saml.saml2.core.Subject
import org.opensaml.saml.saml2.core.SubjectConfirmation
import org.opensaml.saml.saml2.core.impl.SubjectConfirmationBuilder
import spock.lang.Specification
import spock.lang.Unroll

class RequestSubjectValidatorPresignPluginSpec extends Specification {
    private static final String VALID_NAMEID_UNAME_VALUE = "admin"
    private static final String VALID_NAMEID_EMAIL_VALUE = "admin@localhost"
    private static final String INVALID_NAMEID_VALUE = "invalid NameID value"

    private RequestSubjectValidatorPresignPlugin plugin
    private List<String> spMetadata
    private Set<SamlProtocol.Binding> bindings
    private Response response

    // mocks
    private static NameID invalidNameIdMock
    private static NameID unameNameIdMock
    private static NameID emailNameIdMock
    private static NameID blankNameIdMock
    private static List<SubjectConfirmation> emptySubjectConfirmationsList
    private static List<SubjectConfirmation> populatedSubjectConfirmationsList

    private AuthnRequest authnRequestMock
    private Subject subjectMock

    def setupSpec() {
        OpenSAMLUtil.initSamlEngine()

        // static mocks
        emptySubjectConfirmationsList = []
        populatedSubjectConfirmationsList = [new SubjectConfirmationBuilder().buildObject()]

        unameNameIdMock = Mock()
        emailNameIdMock = Mock()
        invalidNameIdMock = Mock()
        blankNameIdMock = Mock()

        unameNameIdMock.getValue() >> VALID_NAMEID_UNAME_VALUE
        emailNameIdMock.getValue() >> VALID_NAMEID_EMAIL_VALUE
        invalidNameIdMock.getValue() >> INVALID_NAMEID_VALUE
        blankNameIdMock.getValue() >> ""
    }

    def setup() {
        plugin = new RequestSubjectValidatorPresignPlugin()

        def ssoMetadata = getClass().getResource('/ssometadata.xml').text
        spMetadata = [ssoMetadata]
        bindings = [SamlProtocol.Binding.HTTP_POST,
                    SamlProtocol.Binding.HTTP_REDIRECT,
                    SamlProtocol.Binding.SOAP] as Set

        def cannedResponse = getClass().getResource('/SAMLResponse.xml').text
        def responseDoc = StaxUtils.read(new ByteArrayInputStream(cannedResponse.getBytes()))
        def responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement())
        response = (Response) responseXmlObject

        // mocks
        authnRequestMock = Mock()
        subjectMock = Mock()

        authnRequestMock.getSubject() >> subjectMock
    }

    def 'no Subject on AuthnRequest'() {
        setup:
        authnRequestMock = Mock()
        authnRequestMock.getSubject() >> null

        when:
        plugin.processPresign(response, authnRequestMock, spMetadata, bindings)

        then:
        noExceptionThrown()
    }

    def 'no/blank NameID on AuthnRequest'() {
        setup:
        subjectMock.getNameID() >> _nameId
        subjectMock.getSubjectConfirmations() >> emptySubjectConfirmationsList

        when:
        plugin.processPresign(response, authnRequestMock, spMetadata, bindings)

        then:
        noExceptionThrown()

        where:
        _nameId << [null, blankNameIdMock]
    }

    def 'Subject with SubjectConfirmations on AuthnRequest'() {
        setup:
        subjectMock.getNameID() >> null
        subjectMock.getSubjectConfirmations() >> populatedSubjectConfirmationsList

        when:
        plugin.processPresign(response, authnRequestMock, spMetadata, bindings)

        then:
        thrown (SAMLRuntimeException)
    }

    def 'Subject with both NameID and SubjectConfirmations on AuthnRequest'() {
        setup:
        subjectMock.getNameID() >> unameNameIdMock
        subjectMock.getSubjectConfirmations() >> populatedSubjectConfirmationsList

        when:
        plugin.processPresign(response, authnRequestMock, spMetadata, bindings)

        then:
        thrown(SAMLRuntimeException)
    }

    @Unroll
    def 'Subject with no NameID and empty SubjectConfirmations on AuthnRequest'() {
        setup:
        subjectMock.getNameID() >> null
        subjectMock.getSubjectConfirmations() >> emptySubjectConfirmationsList

        when:
        plugin.processPresign(response, authnRequestMock, spMetadata, bindings)

        then:
        noExceptionThrown()
    }

    @Unroll
    def 'Subject with invalid NameID on AuthnRequest'() {
        setup:
        subjectMock.getNameID() >> invalidNameIdMock
        subjectMock.getSubjectConfirmations() >> emptySubjectConfirmationsList

        when:
        plugin.processPresign(response, authnRequestMock, spMetadata, bindings)

        then:
        thrown(SAMLRuntimeException)
    }

    @Unroll
    def 'Subject with NameID of #_nameId.getValue() on AuthnRequest'() {
        setup:
        subjectMock.getNameID() >> _nameId
        subjectMock.getSubjectConfirmations() >> emptySubjectConfirmationsList

        when:
        plugin.processPresign(response, authnRequestMock, spMetadata, bindings)

        then:
        noExceptionThrown()

        where:
        _nameId << [unameNameIdMock, emailNameIdMock]
    }
}
