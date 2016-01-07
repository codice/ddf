package org.codice.ddf.security.idp.server

import ddf.security.encryption.EncryptionService
import ddf.security.samlp.LogoutMessage
import ddf.security.samlp.ValidationException
import ddf.security.samlp.impl.LogoutMessageImpl
import ddf.security.samlp.impl.RelayStates
import ddf.security.samlp.impl.SamlValidator
import org.apache.wss4j.common.saml.OpenSAMLUtil
import org.codice.ddf.security.common.jaxrs.RestSecurity
import org.joda.time.DateTime
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.opensaml.saml.common.SAMLVersion
import org.opensaml.saml.common.SignableSAMLObject
import org.opensaml.saml.saml2.core.*
import org.opensaml.xmlsec.signature.SignableXMLObject
import org.w3c.dom.Element
import spock.lang.Specification

import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.xml.stream.XMLStreamException

class IdpEndpointSpecTest extends Specification {
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    IdpEndpoint idpEndpoint;

    final String ENCRYPT_PREFIX = "ENCRYPTED "

    static {
        OpenSAMLUtil.initSamlEngine()
    }

    private static class MockIdpEndpoint extends IdpEndpoint {

        MockIdpEndpoint(String signaturePropertiesPath, String encryptionPropertiesPath, EncryptionService encryptionService) {
            super(signaturePropertiesPath, encryptionPropertiesPath, encryptionService)
        }

        @Override
        void validateRedirect(String relayState, String signatureAlgorithm, String signature,
                              HttpServletRequest request, String samlString, SignableXMLObject logoutRequest,
                              String issuer, String requestId) throws ValidationException {
        }
        @Override
        void validatePost(HttpServletRequest request, SignableSAMLObject samlObject, String requestId)
                throws ValidationException {
        }
    }

    void setup() {
        System.setProperty("org.codice.ddf.system.hostname", "localhost")
        System.setProperty("javax.net.ssl.keyStorePassword", "changeit")
        File jksFile = temporaryFolder.newFile("serverKeystore.jks")
        copyResourceToFile(jksFile, "/serverKeystore.jks")

        File signatureFile = temporaryFolder.newFile("signature.properties")
        copyResourceToFile(signatureFile, "/signature.properties")

        File encryptionFile = temporaryFolder.newFile("encryption.properties")
        copyResourceToFile(encryptionFile, "/encryption.properties")

        EncryptionService encryptionService = Mock(EncryptionService.class) {
            decrypt(_ as String) >> { String data -> data.substring(ENCRYPT_PREFIX.length()) }
            encrypt(_ as String) >> { String data -> "$ENCRYPT_PREFIX$data" }
        }

        System.setProperty("javax.net.ssl.keyStore", jksFile.getAbsolutePath());
        idpEndpoint = new MockIdpEndpoint(signatureFile.absolutePath,
                encryptionFile.absolutePath,
                encryptionService)
        idpEndpoint.strictSignature = true
        idpEndpoint.init()
        idpEndpoint.spMetadata = [sp1metadata, sp2metadata]
        idpEndpoint.logoutStates = new RelayStates<>()
    }

    void copyResourceToFile(File file, String resource) throws IOException {
        file.withOutputStream { fileOutputStream ->
            IdpEndpointSpecTest.class.getResourceAsStream(resource).
                    withStream { resourceStream -> fileOutputStream << resourceStream
                    };
        };
    }

    void cleanup() {
        // This method is for any cleanup that should run after each test
    }

    def "process redirect logout initial with only self sp"() {
        setup:
        def relayState = "relayState"
        def samlRequest = RestSecurity.deflateAndBase64Encode("b64deflatedsamlstring")
        def sig = "mysignature"
        def sigAlg = "signaturealgorithm"
        def cookieVal = "cookievalue"
        def nameId = "nameid"
        def requestId = "originalRequestId"

        idpEndpoint.logoutMessage = Mock(LogoutMessage) {
            extractSamlLogoutRequest(_ as String) >> Mock(LogoutRequest) {
                getIssuer() >> Mock(Issuer) {
                    getValue() >> sp1entityid
                }
                getNameID() >> Mock(NameID) {
                    getValue() >> nameId
                }
                getID() >> requestId
            }
            buildLogoutResponse(_ as String, _ as String, _ as String) >>
                    { String issuer, String statusCode, String inResponseTo ->
                        return new LogoutMessageImpl().buildLogoutResponse(issuer,
                                statusCode,
                                inResponseTo)
                    }
        }

        def request = Mock(HttpServletRequest) {
            getRequestURL() >> new StringBuffer("destinationurl")
            getCookies() >> {
                [Mock(Cookie) {
                    getName() >> Idp.COOKIE
                    getValue() >> cookieVal
                }]
            }
        }

        when:
        def response = idpEndpoint.processRedirectLogout(samlRequest,
                null,
                relayState,
                sigAlg,
                sig,
                request)

        then:
        notThrown(Exception)
        /* This is to obtain the hundreds place of status code, looking for 3xx status */
        boolean isRedirect = (response.status / 100) as int == 3
        (isRedirect || (response.entity as String).contains("window.location.replace"))

        when: "a relaystate already exists, shouldn't start over"
        idpEndpoint.cookieCache.cacheSamlAssertion(cookieVal, Mock(Element))
        idpEndpoint.cookieCache.addActiveSp(cookieVal, sp1entityid)
        idpEndpoint.logoutStates.encode(cookieVal, new LogoutState(sp1entityid as Set))

        response = idpEndpoint.processRedirectLogout(samlRequest,
                null,
                relayState,
                sigAlg,
                sig,
                request)

        then:
        (response.entity as String).contains("Logout already in progress")
    }

    def "process redirect logout initial with one other sp"() {
        setup:
        def relayState = "relayState"
        def samlResponse = RestSecurity.deflateAndBase64Encode("b64deflatedsamlstring")
        def sig = "mysignature"
        def sigAlg = "signaturealgorithm"
        def cookieVal = "cookievalue"
        def nameId = "nameid"
        def requestId = "originalRequestId"
        def responseId = "originalResponseId"

        and: "configure idpEndpoint state"
        def targetSp = sp2entityid
        idpEndpoint.cookieCache.cacheSamlAssertion(cookieVal, Mock(Element))
        idpEndpoint.cookieCache.addActiveSp(cookieVal, targetSp)
        def logoutState = new LogoutState([targetSp] as Set)
        logoutState.with {
            it.nameId = nameId
            originalIssuer = sp1entityid
            originalRequestId = requestId
            initialRelayState = relayState
        }
        idpEndpoint.logoutStates.encode(cookieVal, logoutState)

        idpEndpoint.logoutMessage = Mock(LogoutMessage) {
            extractSamlLogoutResponse(_ as String) >> Mock(LogoutResponse) {
                getIssuer() >> Mock(Issuer) {
                    getValue() >> sp1entityid
                }
                getStatus() >> Mock(Status) {
                    getStatusCode() >> Mock(StatusCode) {
                        getValue() >> StatusCode.SUCCESS
                    }
                }
                getID() >> responseId
            }
            buildLogoutRequest(_ as String, _ as String) >> { String name, String target ->
                return new LogoutMessageImpl().buildLogoutRequest(name, targetSp)
            }
        }

        def request = Mock(HttpServletRequest) {
            getRequestURL() >> new StringBuffer("destinationurl")
            getCookies() >> {
                [Mock(Cookie) {
                    getName() >> Idp.COOKIE
                    getValue() >> cookieVal
                }]
            }
        }

        when:
        def response = idpEndpoint.processRedirectLogout(null,
                samlResponse,
                relayState,
                sigAlg,
                sig,
                request)

        then:
        notThrown(Exception)
    }

    def "process redirect logout initial with one other sp partial logout"() {
        setup:
        def relayState = "relayState"
        def samlResponse = RestSecurity.deflateAndBase64Encode("b64deflatedsamlstring")
        def sig = "mysignature"
        def sigAlg = "signaturealgorithm"
        def cookieVal = "cookievalue"
        def nameId = "nameid"
        def requestId = "originalRequestId"
        def responseId = "originalResponseId"

        and: "configure idpEndpoint state"
        def targetSp = sp2entityid
        idpEndpoint.cookieCache.cacheSamlAssertion(cookieVal, Mock(Element))
        idpEndpoint.cookieCache.addActiveSp(cookieVal, targetSp)
        def logoutState = new LogoutState([targetSp] as Set)
        logoutState.with {
            it.nameId = nameId
            originalIssuer = sp1entityid
            originalRequestId = requestId
            initialRelayState = relayState
        }
        idpEndpoint.logoutStates.encode(cookieVal, logoutState)

        idpEndpoint.logoutMessage = Mock(LogoutMessage) {
            extractSamlLogoutResponse(_ as String) >> Mock(LogoutResponse) {
                getIssuer() >> Mock(Issuer) {
                    getValue() >> sp1entityid
                }
                getStatus() >> Mock(Status) {
                    getStatusCode() >> Mock(StatusCode) {
                        getValue() >> StatusCode.REQUEST_DENIED
                    }
                }
                getID() >> responseId
            }
            buildLogoutRequest(_ as String, _ as String) >> { String name, String target ->
                return new LogoutMessageImpl().buildLogoutRequest(name, targetSp)
            }
        }

        def request = Mock(HttpServletRequest) {
            getRequestURL() >> new StringBuffer("destinationurl")
            getCookies() >> {
                [Mock(Cookie) {
                    getName() >> Idp.COOKIE
                    getValue() >> cookieVal
                }]
            }
        }

        when:
        def response = idpEndpoint.processRedirectLogout(null,
                samlResponse,
                relayState,
                sigAlg,
                sig,
                request)

        then:
        notThrown(Exception)
        idpEndpoint.logoutStates.decode(cookieVal).partialLogout
    }

    def "process post logout initial with only self sp"() {
        setup:
        def relayState = "relayState"
        def samlRequest = RestSecurity.deflateAndBase64Encode("b64deflatedsamlstring")

        def cookieVal = "cookievalue"
        def nameId = "nameid"
        def requestId = "originalRequestId"

        idpEndpoint.logoutMessage = Mock(LogoutMessage) {
            extractSamlLogoutRequest(_ as String) >> Mock(LogoutRequest) {
                getIssuer() >> Mock(Issuer) {
                    getValue() >> sp1entityid
                }
                getNameID() >> Mock(NameID) {
                    getValue() >> nameId
                }
                getID() >> requestId
            }
            buildLogoutResponse(_ as String, _ as String, _ as String) >>
                    { String issuer, String statusCode, String inResponseTo ->
                        return new LogoutMessageImpl().buildLogoutResponse(issuer,
                                statusCode,
                                inResponseTo)
                    }
        }

        def request = Mock(HttpServletRequest) {
            getRequestURL() >> new StringBuffer("destinationurl")
            getCookies() >> {
                [Mock(Cookie) {
                    getName() >> Idp.COOKIE
                    getValue() >> cookieVal
                }]
            }
        }

        when:
        def response = idpEndpoint.processPostLogout(samlRequest,
                null,
                relayState,
                request)

        then:
        notThrown(Exception)
        ((response.entity as String).contains("<form"))
    }

    def "xmlStreamException thrown from logout service"() {
        setup:
        def samlObject = RestSecurity.deflateAndBase64Encode("b64deflatedsamlstring")
        idpEndpoint.logoutMessage = Mock(LogoutMessage) {
            extractSamlLogoutRequest(_ as String) >> { throw new XMLStreamException() }
            extractSamlLogoutResponse(_ as String) >> { throw new XMLStreamException() }
        }
        def request = Mock(HttpServletRequest) {
            getRequestURL() >> new StringBuffer("destinationurl")

            getCookies() >> {
                [Mock(Cookie) {
                    getName() >> Idp.COOKIE
                    getValue() >> "bla"
                }]
            }
        }

        when:
        idpEndpoint.processRedirectLogout(samlObject, null, "bla", "bla", "bla", request)

        then:
        def exception = thrown(IdpException)
        exception.cause instanceof XMLStreamException

        when:
        idpEndpoint.processRedirectLogout(null, samlObject, "bla", "bla", "bla", request)

        then:
        exception = thrown(IdpException)
        exception.cause instanceof XMLStreamException

        when:
        idpEndpoint.processPostLogout(samlObject, null, "bla", request)

        then:
        exception = thrown(IdpException)
        exception.cause instanceof XMLStreamException

        when:
        idpEndpoint.processPostLogout(null, samlObject, "bla", request)

        then:
        exception = thrown(IdpException)
        exception.cause instanceof XMLStreamException
    }

    String sp1entityid = "https://sp1:8993/services/saml"

    String sp2entityid = "https://sp2:8993/services/saml"

    String sp1metadata = $/<md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" entityID="$sp1entityid">
<md:SPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
<md:KeyDescriptor use="signing">
<ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
<ds:X509Data>
<ds:X509Certificate>
MIIC8DCCAlmgAwIBAgIJAIzc4FYrIp9pMA0GCSqGSIb3DQEBCwUAMIGEMQswCQYDVQQGEwJVUzELMAkGA1UECBMCQVoxDDAKBgNVBAoTA0RERjEMMAoGA1UECxMDRGV2MRkwFwYDVQQDExBEREYgRGVtbyBSb290IENBMTEwLwYJKoZIhvcNAQkBFiJlbWFpbEFkZHJlc3M9ZGRmcm9vdGNhQGV4YW1wbGUub3JnMCAXDTE1MTIxMTE1NDMyM1oYDzIxMTUxMTE3MTU0MzIzWjBwMQswCQYDVQQGEwJVUzELMAkGA1UECBMCQVoxDDAKBgNVBAoTA0RERjEMMAoGA1UECxMDRGV2MRIwEAYDVQQDEwlsb2NhbGhvc3QxJDAiBgkqhkiG9w0BCQEWFWxvY2FsaG9zdEBleGFtcGxlLm9yZzCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAx4LI1lsJNmmEdB8HmDwWuAGrVFjNXuKRXD+lUaTPyDHeXcD32zxa0DiZEB5vqfS9NH3I0E56Rbidg6IQ6r/9hOL9+sjWTPRBsQfWzZwjmcUG61psPc9gbFRK5qltz4BLv4+SWvRMMjgxHM8+SROnjCU5FD9roJ9Ww2v+ZWAvYJ8CAwEAAaN7MHkwCQYDVR0TBAIwADAsBglghkgBhvhCAQ0EHxYdT3BlblNTTCBHZW5lcmF0ZWQgQ2VydGlmaWNhdGUwHQYDVR0OBBYEFID3lAgzIEAdGx3RHizsLcGt4WuwMB8GA1UdIwQYMBaAFOFUx5ffCsK/qV94XjsLK+RIF73GMA0GCSqGSIb3DQEBCwUAA4GBACWWsi4WusO5/u1O91obGn8ctFnxVlogBQ/tDZ+neQDxy8YB2J28tztELrRHkaGiCPT4CCKdy0hx/bG/jSM1ypJnPKrPVrCkYL3Y68pzxvrFNq5NqAFCcBOCNsDNfvCSZ/XHvFyGHIuso5wNVxJyvTdhQ+vWbnpiX8qr6vTx2Wgw
</ds:X509Certificate>
</ds:X509Data>
</ds:KeyInfo>
</md:KeyDescriptor>
<md:KeyDescriptor use="encryption">
<ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
<ds:X509Data>
<ds:X509Certificate>
MIIC8DCCAlmgAwIBAgIJAIzc4FYrIp9pMA0GCSqGSIb3DQEBCwUAMIGEMQswCQYDVQQGEwJVUzELMAkGA1UECBMCQVoxDDAKBgNVBAoTA0RERjEMMAoGA1UECxMDRGV2MRkwFwYDVQQDExBEREYgRGVtbyBSb290IENBMTEwLwYJKoZIhvcNAQkBFiJlbWFpbEFkZHJlc3M9ZGRmcm9vdGNhQGV4YW1wbGUub3JnMCAXDTE1MTIxMTE1NDMyM1oYDzIxMTUxMTE3MTU0MzIzWjBwMQswCQYDVQQGEwJVUzELMAkGA1UECBMCQVoxDDAKBgNVBAoTA0RERjEMMAoGA1UECxMDRGV2MRIwEAYDVQQDEwlsb2NhbGhvc3QxJDAiBgkqhkiG9w0BCQEWFWxvY2FsaG9zdEBleGFtcGxlLm9yZzCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAx4LI1lsJNmmEdB8HmDwWuAGrVFjNXuKRXD+lUaTPyDHeXcD32zxa0DiZEB5vqfS9NH3I0E56Rbidg6IQ6r/9hOL9+sjWTPRBsQfWzZwjmcUG61psPc9gbFRK5qltz4BLv4+SWvRMMjgxHM8+SROnjCU5FD9roJ9Ww2v+ZWAvYJ8CAwEAAaN7MHkwCQYDVR0TBAIwADAsBglghkgBhvhCAQ0EHxYdT3BlblNTTCBHZW5lcmF0ZWQgQ2VydGlmaWNhdGUwHQYDVR0OBBYEFID3lAgzIEAdGx3RHizsLcGt4WuwMB8GA1UdIwQYMBaAFOFUx5ffCsK/qV94XjsLK+RIF73GMA0GCSqGSIb3DQEBCwUAA4GBACWWsi4WusO5/u1O91obGn8ctFnxVlogBQ/tDZ+neQDxy8YB2J28tztELrRHkaGiCPT4CCKdy0hx/bG/jSM1ypJnPKrPVrCkYL3Y68pzxvrFNq5NqAFCcBOCNsDNfvCSZ/XHvFyGHIuso5wNVxJyvTdhQ+vWbnpiX8qr6vTx2Wgw
</ds:X509Certificate>
</ds:X509Data>
</ds:KeyInfo>
</md:KeyDescriptor>
<md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://sp1:8993/services/saml/logout"/>
<md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="https://sp1:8993/services/saml/logout"/>
<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://sp1:8993/services/saml/sso" index="0"/>
<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="https://sp1:8993/services/saml/sso" index="1"/>
</md:SPSSODescriptor>
</md:EntityDescriptor>/$

    String sp2metadata = $/<md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" entityID="$sp2entityid">
<md:SPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
<md:KeyDescriptor use="signing">
<ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
<ds:X509Data>
<ds:X509Certificate>
MIIC8DCCAlmgAwIBAgIJAIzc4FYrIp9pMA0GCSqGSIb3DQEBCwUAMIGEMQswCQYDVQQGEwJVUzELMAkGA1UECBMCQVoxDDAKBgNVBAoTA0RERjEMMAoGA1UECxMDRGV2MRkwFwYDVQQDExBEREYgRGVtbyBSb290IENBMTEwLwYJKoZIhvcNAQkBFiJlbWFpbEFkZHJlc3M9ZGRmcm9vdGNhQGV4YW1wbGUub3JnMCAXDTE1MTIxMTE1NDMyM1oYDzIxMTUxMTE3MTU0MzIzWjBwMQswCQYDVQQGEwJVUzELMAkGA1UECBMCQVoxDDAKBgNVBAoTA0RERjEMMAoGA1UECxMDRGV2MRIwEAYDVQQDEwlsb2NhbGhvc3QxJDAiBgkqhkiG9w0BCQEWFWxvY2FsaG9zdEBleGFtcGxlLm9yZzCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAx4LI1lsJNmmEdB8HmDwWuAGrVFjNXuKRXD+lUaTPyDHeXcD32zxa0DiZEB5vqfS9NH3I0E56Rbidg6IQ6r/9hOL9+sjWTPRBsQfWzZwjmcUG61psPc9gbFRK5qltz4BLv4+SWvRMMjgxHM8+SROnjCU5FD9roJ9Ww2v+ZWAvYJ8CAwEAAaN7MHkwCQYDVR0TBAIwADAsBglghkgBhvhCAQ0EHxYdT3BlblNTTCBHZW5lcmF0ZWQgQ2VydGlmaWNhdGUwHQYDVR0OBBYEFID3lAgzIEAdGx3RHizsLcGt4WuwMB8GA1UdIwQYMBaAFOFUx5ffCsK/qV94XjsLK+RIF73GMA0GCSqGSIb3DQEBCwUAA4GBACWWsi4WusO5/u1O91obGn8ctFnxVlogBQ/tDZ+neQDxy8YB2J28tztELrRHkaGiCPT4CCKdy0hx/bG/jSM1ypJnPKrPVrCkYL3Y68pzxvrFNq5NqAFCcBOCNsDNfvCSZ/XHvFyGHIuso5wNVxJyvTdhQ+vWbnpiX8qr6vTx2Wgw
</ds:X509Certificate>
</ds:X509Data>
</ds:KeyInfo>
</md:KeyDescriptor>
<md:KeyDescriptor use="encryption">
<ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
<ds:X509Data>
<ds:X509Certificate>
MIIC8DCCAlmgAwIBAgIJAIzc4FYrIp9pMA0GCSqGSIb3DQEBCwUAMIGEMQswCQYDVQQGEwJVUzELMAkGA1UECBMCQVoxDDAKBgNVBAoTA0RERjEMMAoGA1UECxMDRGV2MRkwFwYDVQQDExBEREYgRGVtbyBSb290IENBMTEwLwYJKoZIhvcNAQkBFiJlbWFpbEFkZHJlc3M9ZGRmcm9vdGNhQGV4YW1wbGUub3JnMCAXDTE1MTIxMTE1NDMyM1oYDzIxMTUxMTE3MTU0MzIzWjBwMQswCQYDVQQGEwJVUzELMAkGA1UECBMCQVoxDDAKBgNVBAoTA0RERjEMMAoGA1UECxMDRGV2MRIwEAYDVQQDEwlsb2NhbGhvc3QxJDAiBgkqhkiG9w0BCQEWFWxvY2FsaG9zdEBleGFtcGxlLm9yZzCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAx4LI1lsJNmmEdB8HmDwWuAGrVFjNXuKRXD+lUaTPyDHeXcD32zxa0DiZEB5vqfS9NH3I0E56Rbidg6IQ6r/9hOL9+sjWTPRBsQfWzZwjmcUG61psPc9gbFRK5qltz4BLv4+SWvRMMjgxHM8+SROnjCU5FD9roJ9Ww2v+ZWAvYJ8CAwEAAaN7MHkwCQYDVR0TBAIwADAsBglghkgBhvhCAQ0EHxYdT3BlblNTTCBHZW5lcmF0ZWQgQ2VydGlmaWNhdGUwHQYDVR0OBBYEFID3lAgzIEAdGx3RHizsLcGt4WuwMB8GA1UdIwQYMBaAFOFUx5ffCsK/qV94XjsLK+RIF73GMA0GCSqGSIb3DQEBCwUAA4GBACWWsi4WusO5/u1O91obGn8ctFnxVlogBQ/tDZ+neQDxy8YB2J28tztELrRHkaGiCPT4CCKdy0hx/bG/jSM1ypJnPKrPVrCkYL3Y68pzxvrFNq5NqAFCcBOCNsDNfvCSZ/XHvFyGHIuso5wNVxJyvTdhQ+vWbnpiX8qr6vTx2Wgw
</ds:X509Certificate>
</ds:X509Data>
</ds:KeyInfo>
</md:KeyDescriptor>
<md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://sp2:8993/services/saml/logout"/>
<md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="https://sp2:8993/services/saml/logout"/>
<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://sp2:8993/services/saml/sso" index="0"/>
<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="https://sp2:8993/services/saml/sso" index="1"/>
</md:SPSSODescriptor>
</md:EntityDescriptor>/$
}
