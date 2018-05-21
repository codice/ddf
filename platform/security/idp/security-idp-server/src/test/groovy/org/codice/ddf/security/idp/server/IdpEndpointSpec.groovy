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
package org.codice.ddf.security.idp.server

import com.google.common.collect.ImmutableList
import ddf.security.encryption.EncryptionService
import ddf.security.samlp.LogoutMessage
import ddf.security.samlp.ValidationException
import ddf.security.samlp.impl.RelayStates
import org.apache.wss4j.common.saml.OpenSAMLUtil
import org.codice.ddf.security.common.jaxrs.RestSecurity
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.opensaml.saml.common.SignableSAMLObject
import org.opensaml.saml.saml2.core.*
import org.opensaml.saml.saml2.core.impl.LogoutRequestBuilder
import org.opensaml.saml.saml2.core.impl.LogoutResponseBuilder
import org.opensaml.xmlsec.signature.SignableXMLObject
import org.w3c.dom.Element
import spock.lang.Specification

import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.xml.stream.XMLStreamException
import java.nio.charset.StandardCharsets

class IdpEndpointSpec extends Specification {
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    IdpEndpoint idpEndpoint

    List<SessionIndex> sessionIndexes

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
        SessionIndex sessionIndex = Mock(SessionIndex.class) {
            getSessionIndex() >> "1"
        }
        sessionIndexes = Collections.singletonList(sessionIndex) as List<SessionIndex>
    }

    void copyResourceToFile(File file, String resource) throws IOException {
        file.withOutputStream { fileOutputStream ->
            IdpEndpointSpec.class.getResourceAsStream(resource).
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
                getSessionIndexes() >> sessionIndexes
                getIssuer() >> Mock(Issuer) {
                    getValue() >> sp1entityid
                }
                getNameID() >> Mock(NameID) {
                    getValue() >> nameId
                }
                getID() >> requestId
            }
            buildLogoutResponse(_ as String, _ as String, _ as String) >>
                    {
                        return new LogoutResponseBuilder().buildObject();
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
                getSessionIndexes() >> Collections.emptyList()
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
            buildLogoutRequest(_, _, _) >> new LogoutRequestBuilder().buildObject();

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
        response.status == 303
        (response.location as String).contains(targetSp)
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
                getSessionIndexes() >> Collections.emptyList()
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
            buildLogoutRequest(_ as String, _ as String, _ as List) >> { String name, String target, Collection sessionIndexes ->
                return new LogoutRequestBuilder().buildObject();
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
                getSessionIndexes() >> Collections.emptyList()
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
                        return new LogoutResponseBuilder().buildObject();
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

    def "verify destination is present in LogoutResponse"(){
        setup:
        String samlRequest = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz48c2FtbDJwOkxvZ291dFJlcXVlc3QgeG1sbnM6c2FtbDJwPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6cHJvdG9jb2wiIERlc3RpbmF0aW9uPSJodHRwczovL2xvY2FsaG9zdDo4OTkzL3NlcnZpY2VzL2lkcC9sb2dvdXQiIElEPSJhMmI3ODY3MDA0MTdqYjNqMzdoZTdqNzBqNTI4YTM1IiBJc3N1ZUluc3RhbnQ9IjIwMTgtMDEtMThUMTY6MTQ6MDAuOTU0WiIgVmVyc2lvbj0iMi4wIj48c2FtbDI6SXNzdWVyIHhtbG5zOnNhbWwyPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXNzZXJ0aW9uIj5odHRwOi8vbG9jYWxob3N0OjgwL3NwcmluZy1zZWN1cml0eS1zYW1sMi1zYW1wbGUvc2FtbC9tZXRhZGF0YTwvc2FtbDI6SXNzdWVyPjxkczpTaWduYXR1cmUgeG1sbnM6ZHM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyMiPjxkczpTaWduZWRJbmZvPjxkczpDYW5vbmljYWxpemF0aW9uTWV0aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS8xMC94bWwtZXhjLWMxNG4jIi8+PGRzOlNpZ25hdHVyZU1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNyc2Etc2hhMSIvPjxkczpSZWZlcmVuY2UgVVJJPSIjYTJiNzg2NzAwNDE3amIzajM3aGU3ajcwajUyOGEzNSI+PGRzOlRyYW5zZm9ybXM+PGRzOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNlbnZlbG9wZWQtc2lnbmF0dXJlIi8+PGRzOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIvPjwvZHM6VHJhbnNmb3Jtcz48ZHM6RGlnZXN0TWV0aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnI3NoYTEiLz48ZHM6RGlnZXN0VmFsdWU+WHFxMWQzS3E5L01jZ3NaSlRkY1dNdThLVGlRPTwvZHM6RGlnZXN0VmFsdWU+PC9kczpSZWZlcmVuY2U+PC9kczpTaWduZWRJbmZvPjxkczpTaWduYXR1cmVWYWx1ZT5pbU1xNk9HZHVYUmFnWW1kQVRzNEhzQy96bXljNHhUWjhsSHhxaFJsZWRzanM3R0Y2TDZ4RkhUR0ZCQ3BHODRtazR4cVRWU2thSEs1aVUySlh4VFBxUWpNd1NMbXlyeCtOS3BqbWc1Q2hnL0RUSFMvQS9YYTU5SnIyN1NOTGhTSGw0aWZrUEdvdDlHcE5BcEttUTBhYVRHaHlYUE5KN1JYb1doRTZhZXovWHdnbzBxMFI2SlQ0UWJGRVlFQUVjT0hxL2kxZFNWakNWQnN1TUlId0E1OHJFZHBFejI0MkhpYmpXVTFXL1lnZTU5dmlrK1NMUXp2RERFMDBGNnBweU5WZ0laVkFIWFhFODZndm5xZlQzYmJLK1dVV3NVbklBM2JJQVZtSU42aWhQbXdBb0YxOUVSdVVHbzd2aS9xUjlLSk9rWmRXRE5leUt6OVVCenRmWG5YZ2c9PTwvZHM6U2lnbmF0dXJlVmFsdWU+PGRzOktleUluZm8+PGRzOlg1MDlEYXRhPjxkczpYNTA5Q2VydGlmaWNhdGU+TUlJRFVqQ0NBcnVnQXdJQkFnSUlWQlpoVllBRk1pZ3dEUVlKS29aSWh2Y05BUUVGQlFBd2dZUXhDekFKQmdOVkJBWVRBbFZUTVFzdwpDUVlEVlFRSUV3SkJXakVNTUFvR0ExVUVDaE1EUkVSR01Rd3dDZ1lEVlFRTEV3TkVaWFl4R1RBWEJnTlZCQU1URUVSRVJpQkVaVzF2CklGSnZiM1FnUTBFeE1UQXZCZ2txaGtpRzl3MEJDUUVXSW1WdFlXbHNRV1JrY21WemN6MWtaR1p5YjI5MFkyRkFaWGhoYlhCc1pTNXYKY21jd0hoY05NVGd3TVRBMU1ESXhNekkxV2hjTk1Ua3dNVEExTURJeE16STFXakJLTVFzd0NRWURWUVFHRXdKVlV6RUxNQWtHQTFVRQpDQXdDUVZveEREQUtCZ05WQkFvTUEwUkVSakVNTUFvR0ExVUVDd3dEUkdWMk1SSXdFQVlEVlFRRERBbHNiMk5oYkdodmMzUXdnZ0VpCk1BMEdDU3FHU0liM0RRRUJBUVVBQTRJQkR3QXdnZ0VLQW9JQkFRQ3gyMmJnY1p0L0ZTMXh5S0JBS2lrRTVnTGlpemNFMVlWODBWdmEKaytEVnBPcUx4c2VidW9GL2lXakMyRWRJK1VCZkh6K2ZkTmxtbzJVa09MQ1R5VS90MGFSZFJkWnVQU28vM2ViUXRsRW15cEtKNjQ3dApIZVFzUEVPMS9Fc1lQZlBtRmVTWHZIUU5RZWxUYTlJWlFlMTErK0JoaERyTWNVTkNEMWRuajdrTE5GVkNuSzNwTGhqbjFCTCtweDNFCmt3bXFCZnBoSG0xVGo3V3pCSWY2Q2JCQ3B0WVNUY3FxbGRIelhzcXl5TnZTcVJxZWxkR1p2Vkhsb0xPTnd0OWJ6VFZrOHlibWIxRVAKajJqSU1VeFRPeGEwZFZVS0plSVozenlaNFZLekxmY2pVblhnbU5BcVgyVHl0eWpFODUwOVlFc1p2SExST3c0VkRWS1pVazUxcVZtQgpBZ01CQUFHamdZRXdmekFKQmdOVkhSTUVBakFBTUNjR0NXQ0dTQUdHK0VJQkRRUWFGaGhHVDFJZ1ZFVlRWRWxPUnlCUVZWSlFUMU5GCklFOU9URmt3SFFZRFZSME9CQllFRkV4U0hTZ2Zrbzl6UThyM0VKYXRNMFIyUUlZQ01COEdBMVVkSXdRWU1CYUFGT0ZVeDVmZkNzSy8KcVY5NFhqc0xLK1JJRjczR01Ba0dBMVVkRVFRQ01BQXdEUVlKS29aSWh2Y05BUUVGQlFBRGdZRUFkdzBtZ1BySVVVc1lKelRMSWN3bwo2TjdQY2p2YitOMUQvRjh3VHRtR2VjRWY3UG5zUm9RQkpJenViVDVYc1psWUFTVWhybE51TkFUSXpkSnkzc3hhbVY4OTBXcmxuVWZXClpIcVg3dnZFSWFnY205RXMvdTJiYTViU1h2NGZnd0VEdCtXdUhwQUpZSkxPZjN1aEE4ck9rZ0t1SVVVcDlYeG5waWw2TUNNeHBMcz08L2RzOlg1MDlDZXJ0aWZpY2F0ZT48L2RzOlg1MDlEYXRhPjwvZHM6S2V5SW5mbz48L2RzOlNpZ25hdHVyZT48c2FtbDI6TmFtZUlEIHhtbG5zOnNhbWwyPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXNzZXJ0aW9uIiBGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpuYW1laWQtZm9ybWF0OnBlcnNpc3RlbnQiIE5hbWVRdWFsaWZpZXI9Imh0dHA6Ly9jeGYuYXBhY2hlLm9yZy9zdHMiPmFkbWluPC9zYW1sMjpOYW1lSUQ+PHNhbWwycDpTZXNzaW9uSW5kZXg+Nzk3ODwvc2FtbDJwOlNlc3Npb25JbmRleD48L3NhbWwycDpMb2dvdXRSZXF1ZXN0Pg=="
        String relayState = "relayState"
        def request = Mock(HttpServletRequest) {
            getCookies() >> {
                [Mock(Cookie) {
                    getName() >> Idp.COOKIE
                    getValue() >> "cookieValue"
                }]
            }
        }

        idpEndpoint.logoutMessage = Mock(LogoutMessage) {
            extractSamlLogoutRequest(_ as String) >> Mock(LogoutRequest) {
                getIssuer() >> Mock(Issuer) {
                    getValue() >> sp1entityid
                }
                getNameID() >> Mock(NameID) {
                    getValue() >> "nameId"
                }
                getID() >> "requestId"
            }
            buildLogoutResponse(_ as String, _ as String, _ as String) >>
                    { String issuer, String statusCode, String inResponseTo ->
                        return new LogoutResponseBuilder().buildObject()
                    }
        }

        idpEndpoint.setSpMetadata(ImmutableList.of(sp1metadata, sp2metadata))

        when:
        def response = idpEndpoint.processPostLogout(
                samlRequest,
                null,
                relayState,
                request)

        then:
        //Check destination
        String responseString = response.getEntity().toString()
        int valueStartIndex = responseString.indexOf("value")
        int valueEndIndex = responseString.indexOf("/", valueStartIndex)
        String value = responseString.substring(valueStartIndex, valueEndIndex).replace("value=\"", "")
        String logoutResponse = new String(Base64.getMimeDecoder().decode(value), StandardCharsets.UTF_8)

        logoutResponse.indexOf("Destination") != -1
        logoutResponse.contains(sp1entityid)
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
<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:PAOS" Location="https://sp1:8993/services/saml/sso"/>
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
<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:PAOS" Location="https://sp2:8993/services/saml/sso"/>
</md:SPSSODescriptor>
</md:EntityDescriptor>/$
}
