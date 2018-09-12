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
package org.codice.ddf.security.idp.plugin.subjectconfirmation

import ddf.security.samlp.SamlProtocol
import org.apache.cxf.staxutils.StaxUtils
import org.apache.wss4j.common.saml.OpenSAMLUtil
import org.opensaml.core.xml.XMLObject
import org.opensaml.saml.saml2.core.AuthnRequest
import org.opensaml.saml.saml2.core.Issuer
import org.opensaml.saml.saml2.core.Response
import org.w3c.dom.Document
import spock.lang.Specification

class SubjectConfirmationPresignPluginSpec extends Specification {
    SubjectConfirmationPresignPlugin plugin
    List<String> spMetadata
    Set<SamlProtocol.Binding> bindings
    Response response
    AuthnRequest authNReq
    Issuer issuer

    def setupSpec() {
        OpenSAMLUtil.initSamlEngine()
    }

    def setup() {
        plugin = new SubjectConfirmationPresignPlugin()

        def ssoMetadata = getClass().getResource('/ssometadata.xml').text
        spMetadata = [ssoMetadata]
        bindings = [SamlProtocol.Binding.HTTP_POST,
                    SamlProtocol.Binding.HTTP_REDIRECT,
                    SamlProtocol.Binding.SOAP] as Set

        def cannedResponse = getClass().getResource('/SAMLResponse.xml').text
        Document responseDoc = StaxUtils.read(new ByteArrayInputStream(cannedResponse.getBytes()))
        XMLObject responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement())
        response = (Response) responseXmlObject

        authNReq = Mock(AuthnRequest)
        authNReq.getAssertionConsumerServiceIndex() >> 0
        issuer = Mock(Issuer)
        authNReq.getIssuer() >> issuer
    }

    def 'subjectConfirmationData is updated'() {
        setup:
        issuer.getValue() >> 'https://localhost:8993/services/saml'

        when:
        plugin.processPresign(response, authNReq, spMetadata, bindings)

        then:
        response.assertions.size() == 1
        response.assertions[0].subject.subjectConfirmations.size() == 1
        def scd = response.assertions[0].subject.subjectConfirmations[0].subjectConfirmationData
        scd.inResponseTo == response.getInResponseTo()
        scd.notOnOrAfter == response.assertions[0].conditions.notOnOrAfter
        scd.recipient.endsWith('services/saml/sso')
    }

    def 'subjectConfirmationData is not updated when AcsUrl is not found'() {
        setup:
        issuer.getValue() >> 'foobar'

        when:
        plugin.processPresign(response, authNReq, spMetadata, bindings)

        then:
        thrown(IllegalArgumentException)
    }
}
