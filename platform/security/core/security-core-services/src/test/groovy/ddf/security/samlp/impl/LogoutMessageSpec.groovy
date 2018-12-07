package ddf.security.samlp.impl

import ddf.security.encryption.EncryptionService
import ddf.security.samlp.SystemCrypto
import org.apache.cxf.rs.security.saml.sso.SSOConstants
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.opensaml.saml.common.SAMLVersion
import org.opensaml.saml.saml2.core.LogoutRequest
import org.opensaml.saml.saml2.core.LogoutResponse
import org.opensaml.saml.saml2.core.StatusCode
import spock.lang.Specification

import java.time.Instant

import static java.time.Instant.now
import static org.apache.commons.lang.StringUtils.isNotBlank

class LogoutMessageSpec extends Specification {
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    LogoutMessageImpl logoutMessage;

    final String ENCRYPT_PREFIX = "ENCRYPTED "

    final String NAME_ID = "MyNameId"

    final String ISSUER_ID = "MyIssuerId"

    final String IN_RESPONSE_TO = "InResponseToID"
    final String SESSION_INDEX = "MySessionIndex";

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
        logoutMessage = new LogoutMessageImpl()
        logoutMessage.systemCrypto = new SystemCrypto(encryptionFile.absolutePath,
                signatureFile.absolutePath,
                encryptionService)
        logoutMessage.uuidGenerator = Mock(UuidGenerator.class) {
            generateUuid() >> { UUID.randomUUID().toString() }
        }
    }

    void copyResourceToFile(File file, String resource) throws IOException {
        file.withOutputStream { fileOutputStream ->
            LogoutMessageSpec.class.getResourceAsStream(resource).
                    withStream { resourceStream -> fileOutputStream << resourceStream
                    };
        };
    }

    void cleanup() {
        // This method is for any cleanup that should run after each test
    }

    def "build valid logout request"() {
        when:
        LogoutRequest logoutRequest = logoutMessage.buildLogoutRequest(NAME_ID, ISSUER_ID, Collections.singletonList(SESSION_INDEX))

        then:
        isNotBlank(logoutRequest.ID)
        NAME_ID.equals(logoutRequest.nameID.value)
        ISSUER_ID.equals(logoutRequest.issuer.value)
        SAMLVersion.VERSION_20.equals(logoutRequest.version)
        logoutRequest.sessionIndexes.size() == 1
        SESSION_INDEX.equals(logoutRequest.sessionIndexes.get(0).getSessionIndex());
        now().isAfter(Instant.ofEpochMilli(logoutRequest.issueInstant.millis))
    }

    def "build logout request with invalid info"() {

        when:
        LogoutRequest logoutRequest = logoutMessage.buildLogoutRequest(nameId, issuerId, id,
                sessionIndex == null ? null : Collections.singletonList(sessionIndex))

        then:
        thrown(IllegalArgumentException)

        where: "any param is null"
        nameId | issuerId | id    | sessionIndex
        null   | null     | "xxx" | "xxx"
        "xxx"  | null     | "xxx" | "xxx"
        null   | "xxx"    | "xxx" | "xxx"
        null   | null     | null  | null
        "xxx"  | null     | null  | "xxx"
        null   | "xxx"    | null  | null
        "xxx"  | "xxx"    | null  | null
        "xxx"  | "xxx"    | "xxx" | null
    }

    def "build logout response with valid info and inResponseTo"() {
        when:
        LogoutResponse logoutResponse = logoutMessage.buildLogoutResponse(ISSUER_ID,
                StatusCode.SUCCESS,
                IN_RESPONSE_TO)

        then:
        isNotBlank(logoutResponse.ID)
        ISSUER_ID.equals(logoutResponse.issuer.value)
        StatusCode.SUCCESS.equals(logoutResponse.status.statusCode.value)
        IN_RESPONSE_TO.equals(logoutResponse.inResponseTo)
        SAMLVersion.VERSION_20.equals(logoutResponse.version)
        !now().
                isBefore(Instant.ofEpochMilli(logoutResponse.issueInstant.millis))
    }

    def "build logout response with no inResponseTo"() {
        when:
        LogoutResponse logoutResponse = logoutMessage.buildLogoutResponse("issuer",
                StatusCode.SUCCESS)

        then:
        logoutResponse.inResponseTo == null
    }

    def "verify signed saml request"() {
        setup:
        LogoutRequest logoutRequest = logoutMessage.buildLogoutRequest(NAME_ID, ISSUER_ID, Collections.singletonList(SESSION_INDEX))

        def target = new URI("https://mytarget.com")
        def relayState = "MyRelayStateGuid"

        when:
        URI signedRequest = logoutMessage.signSamlGetRequest(logoutRequest, target, relayState)

        then:
        signedRequest.toString().
                startsWith(target.toString())
        signedRequest.toString().
                contains("${SSOConstants.RELAY_STATE}=")
        signedRequest.toString().
                contains("${SSOConstants.SAML_REQUEST}=")

    }

    def "verify signed saml response"() {
        setup:
        LogoutResponse logoutResponse = logoutMessage.buildLogoutResponse(NAME_ID, ISSUER_ID)

        def target = new URI("https://mytarget.com")
        def relayState = "MyRelayStateGuid"

        when:
        URI signedRequest = logoutMessage.signSamlGetResponse(logoutResponse, target, relayState)

        then:
        signedRequest.toString().
                startsWith(target.toString())
        signedRequest.toString().
                contains("${SSOConstants.RELAY_STATE}=")
        signedRequest.toString().
                contains("${SSOConstants.SAML_RESPONSE}=")
    }
}
