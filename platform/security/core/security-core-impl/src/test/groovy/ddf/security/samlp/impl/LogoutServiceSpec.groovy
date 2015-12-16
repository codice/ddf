package ddf.security.samlp.impl

import ddf.security.encryption.EncryptionService
import ddf.security.samlp.SystemCrypto
import org.apache.commons.io.IOUtils
import org.apache.cxf.rs.security.saml.sso.SSOConstants
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.opensaml.common.SAMLVersion
import org.opensaml.saml2.core.LogoutRequest
import org.opensaml.saml2.core.LogoutResponse
import org.opensaml.saml2.core.StatusCode
import org.opensaml.saml2.core.impl.IssuerBuilder
import org.opensaml.saml2.core.impl.LogoutRequestBuilder
import org.opensaml.saml2.core.impl.LogoutResponseBuilder
import org.opensaml.saml2.core.impl.NameIDBuilder
import org.opensaml.saml2.core.impl.StatusBuilder
import org.opensaml.saml2.core.impl.StatusCodeBuilder
import org.opensaml.ws.soap.soap11.impl.BodyBuilder
import org.opensaml.ws.soap.soap11.impl.EnvelopeBuilder
import spock.lang.Specification

import java.time.Instant

import static java.time.Instant.now
import static org.apache.commons.lang.StringUtils.isNotBlank

class LogoutServiceSpec extends Specification {
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    LogoutServiceImpl logoutService;

    final String ENCRYPT_PREFIX = "ENCRYPTED "

    final String NAME_ID = "MyNameId"
    final String ISSUER_ID = "MyIssuerId"
    final String IN_RESPONSE_TO = "InResponseToID"

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
        logoutService = new LogoutServiceImpl()
        injectBuilders(logoutService)
        logoutService.systemCrypto = new SystemCrypto(
                encryptionFile.absolutePath,
                signatureFile.absolutePath,
                encryptionService)
    }

    void copyResourceToFile(File file, String resource) throws IOException {
        file.withOutputStream { fileOutputStream ->
            LogoutServiceSpec.class.getResourceAsStream(resource).withStream { resourceStream ->
                IOUtils.copy(resourceStream, fileOutputStream)
            };
        };
    }

    void injectBuilders(LogoutServiceImpl plugin) {
        plugin.logoutRequestBuilder = new LogoutRequestBuilder()
        plugin.logoutResponseBuilder = new LogoutResponseBuilder()
        plugin.nameIDBuilder = new NameIDBuilder()
        plugin.issuerBuilder = new IssuerBuilder()
        plugin.statusBuilder = new StatusBuilder()
        plugin.envelopeBuilder = new EnvelopeBuilder()
        plugin.bodyBuilder = new BodyBuilder()
        plugin.statusCodeBuilder = new StatusCodeBuilder()
    }

    void cleanup() {

    }

    def "build valid logout request"() {
        when:
        LogoutRequest logoutRequest = logoutService.buildLogoutRequest(NAME_ID, ISSUER_ID)


        then:
        isNotBlank(logoutRequest.ID)
        NAME_ID.equals(logoutRequest.nameID.value)
        ISSUER_ID.equals(logoutRequest.issuer.value)
        SAMLVersion.VERSION_20.equals(logoutRequest.version)
        now().isAfter(Instant.ofEpochMilli(logoutRequest.issueInstant.millis))
    }

    def "build logout request with invalid info"() {
        when:
        LogoutRequest logoutRequest = logoutService.buildLogoutRequest(nameId, issuerId, id)

        then:
        thrown(IllegalArgumentException)

        where: "any param is null"
        nameId | issuerId | id
        null   | null     | "xxx"
        "xxx"  | null     | "xxx"
        null   | "xxx"    | "xxx"
        null   | null     | null
        "xxx"  | null     | null
        null   | "xxx"    | null
        "xxx"  | "xxx"    | null
    }

    def "build logout response with valid info and inResponseTo"() {
        when:
        LogoutResponse logoutResponse = logoutService.buildLogoutResponse(
                ISSUER_ID,
                StatusCode.SUCCESS_URI,
                IN_RESPONSE_TO)

        then:
        isNotBlank(logoutResponse.ID)
        ISSUER_ID.equals(logoutResponse.issuer.value)
        StatusCode.SUCCESS_URI.equals(logoutResponse.status.statusCode.value)
        IN_RESPONSE_TO.equals(logoutResponse.inResponseTo)
        SAMLVersion.VERSION_20.equals(logoutResponse.version)
        now().isAfter(Instant.ofEpochMilli(logoutResponse.issueInstant.millis))
    }

    def "build logout response with no inResponseTo"() {
        when:
        LogoutResponse logoutResponse = logoutService.buildLogoutResponse(
                "issuer",
                StatusCode.SUCCESS_URI)

        then:
        logoutResponse.inResponseTo == null
    }

    def "verify signed saml request"() {
        setup:
        LogoutRequest logoutRequest = logoutService.buildLogoutRequest(NAME_ID, ISSUER_ID)

        def target = new URI("https://mytarget.com")
        def relayState = "MyRelayStateGuid"

        when:
        URI signedRequest = logoutService.signSamlGetRequest(logoutRequest, target, relayState)

        then:
        signedRequest.toString().startsWith(target.toString())
        signedRequest.toString().contains("${SSOConstants.RELAY_STATE}=")
        signedRequest.toString().contains("${SSOConstants.SAML_REQUEST}=")

    }

    def "verify signed saml response"() {
        setup:
        LogoutRequest logoutRequest = logoutService.buildLogoutRequest(NAME_ID, ISSUER_ID)

        def target = new URI("https://mytarget.com")
        def relayState = "MyRelayStateGuid"

        when:
        URI signedRequest = logoutService.signSamlGetResponse(logoutRequest, target, relayState)

        then:
        signedRequest.toString().startsWith(target.toString())
        signedRequest.toString().contains("${SSOConstants.RELAY_STATE}=")
        signedRequest.toString().contains("${SSOConstants.SAML_REQUEST}=")
    }
}
