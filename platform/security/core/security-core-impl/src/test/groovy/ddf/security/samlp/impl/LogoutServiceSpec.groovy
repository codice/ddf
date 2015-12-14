package ddf.security.samlp.impl

import ddf.security.encryption.EncryptionService
import ddf.security.samlp.SystemCrypto
import org.apache.commons.io.IOUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.opensaml.saml2.core.impl.IssuerBuilder
import org.opensaml.saml2.core.impl.LogoutRequestBuilder
import org.opensaml.saml2.core.impl.LogoutResponseBuilder
import org.opensaml.saml2.core.impl.NameIDBuilder
import org.opensaml.saml2.core.impl.StatusBuilder
import org.opensaml.saml2.core.impl.StatusCodeBuilder
import org.opensaml.ws.soap.soap11.impl.BodyBuilder
import org.opensaml.ws.soap.soap11.impl.EnvelopeBuilder
import spock.lang.Specification

class LogoutServiceSpec extends Specification {
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    LogoutServiceImpl logoutService;

    final String ENCRYPT_PREFIX = "ENCRYPTED "

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

    def "first test"() {
        setup:
        def foo = { "bar" }

        when:
        println foo()

        then:
        notThrown(Exception)
    }

}
