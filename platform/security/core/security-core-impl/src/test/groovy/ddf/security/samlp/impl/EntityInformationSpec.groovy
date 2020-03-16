package ddf.security.samlp.impl


import org.apache.wss4j.common.saml.OpenSAMLUtil
import org.opensaml.saml.saml2.core.AuthnRequest
import org.opensaml.saml.saml2.metadata.EntityDescriptor
import spock.lang.Specification

import java.lang.reflect.Field

import static SamlProtocol.Binding

class EntityInformationSpec extends Specification {
    static final Set<Binding> DEFAULT_BINDINGS =
            [Binding.HTTP_REDIRECT, Binding.HTTP_POST] as Set

    static final String FAKE_SIGN_CERT = "+SIGNCERT+"

    static final String FAKE_ENCRYPT_CERT = "+ENCRYPTCERT+"

    static {
        OpenSAMLUtil.initSamlEngine()
    }

    void setup() {
        // This method is for any setup that should run before each test
    }

    void cleanup() {
        // This method is for any cleanup that should run after each test
    }

    def "check certificates"() {
        setup:
        EntityInformation entityInfo = getFakeEntityInfo()

        when:
        String signCert = entityInfo.signingCertificate
        String encryptCert = entityInfo.encryptionCertificate


        then:
        FAKE_SIGN_CERT == signCert
        FAKE_ENCRYPT_CERT == encryptCert
    }

    def "check logout service"() {
        setup:
        EntityInformation entityInfo = getFakeEntityInfo()

        when:
        def serviceInfo = entityInfo.getLogoutService()

        then: "Default binding should be the preferred binding (as specified in EntityInformation)"
        EntityInformation.PREFERRED_BINDING == serviceInfo.binding

        when:
        serviceInfo = entityInfo.getLogoutService(Binding.HTTP_POST)

        then:
        Binding.HTTP_POST == serviceInfo.binding

        when:
        serviceInfo = entityInfo.getLogoutService(Binding.HTTP_REDIRECT)

        then:
        Binding.HTTP_REDIRECT == serviceInfo.binding

        when: "Binding that we don't support in the FakeSPMetadata"
        serviceInfo = entityInfo.getLogoutService(Binding.HTTP_ARTIFACT)

        then:
        serviceInfo != null
        serviceInfo.binding in DEFAULT_BINDINGS
    }

    def "check logout service of half supported metadata"() {
        setup:
        MetadataConfigurationParser mcp = new MetadataConfigurationParser(
                [fakeSpHalfSupportedMetadata])
        EntityDescriptor ed = mcp.entityDescriptors.find {true}.value
        EntityInformation entityInfo = new EntityInformation.Builder(ed, DEFAULT_BINDINGS).build()

        when: "request any binding"
        def serviceInfo = entityInfo.getLogoutService()

        then: "only supported binding of Union(SP, IDP)"
        serviceInfo.binding != null
        Binding.HTTP_POST == serviceInfo.binding

        when: "preferred an unsupported binding"
        serviceInfo = entityInfo.getLogoutService(Binding.PAOS)

        then:
        Binding.HTTP_POST == serviceInfo.binding

        when: "preferred a binding only supported by sp"
        serviceInfo = entityInfo.getLogoutService(Binding.HTTP_ARTIFACT)

        then:
        Binding.HTTP_POST == serviceInfo.binding
    }

    def "check assertionconsumerservice"() {
        setup:
        EntityInformation entityInfo = getFakeEntityInfo()

        when:
        def serviceInfo = entityInfo.getAssertionConsumerService(null, null, null)

        then:
        Binding.HTTP_POST == serviceInfo.binding

        when: "authnreq has binding and its supported, prefer it"
        AuthnRequest authnRequest = Mock(AuthnRequest) {
            getProtocolBinding() >> Binding.HTTP_REDIRECT.uri
        }
        serviceInfo = entityInfo.getAssertionConsumerService(authnRequest, Binding.HTTP_POST, null)

        then:
        Binding.HTTP_POST == serviceInfo.binding

        when: "authnreq has binding and its supported, prefer it"
        authnRequest = Mock(AuthnRequest) {
            getProtocolBinding() >> Binding.HTTP_POST.uri
        }
        serviceInfo = entityInfo.getAssertionConsumerService(authnRequest, Binding.HTTP_REDIRECT, null)

        then:
        Binding.HTTP_POST == serviceInfo.binding

        when: "authnreq has binding and not supported, use specified preferred binding"
        authnRequest = Mock(AuthnRequest) {
            getProtocolBinding() >> Binding.HTTP_ARTIFACT.uri
        }
        serviceInfo = entityInfo.getAssertionConsumerService(authnRequest, Binding.HTTP_POST, null)

        then:
        Binding.HTTP_POST == serviceInfo.binding
    }

    def "check assertionconsumerservice with half supported metadata"() {
        setup:
        MetadataConfigurationParser mcp = new MetadataConfigurationParser(
                [fakeSpHalfSupportedMetadata])
        EntityDescriptor ed = mcp.entityDescriptors.find {true}.value
        EntityInformation entityInfo = new EntityInformation.Builder(ed, DEFAULT_BINDINGS).build()

        when:
        def serviceInfo = entityInfo.getAssertionConsumerService(null, null, null)

        then: "should be only supported binding (post)"
        Binding.HTTP_POST == serviceInfo.binding
    }

    def "check assertionconsumerservice with a default"() {
        setup:
        MetadataConfigurationParser mcp = new MetadataConfigurationParser(
                [fakeSpNoPostMetadata])
        EntityDescriptor ed = mcp.entityDescriptors.find {true}.value
        EntityInformation entityInfo = new EntityInformation.Builder(ed, DEFAULT_BINDINGS).build()

        reflectAndSetDefaultAssertionConsumerService(entityInfo)

        when:
        def serviceInfo = entityInfo.getAssertionConsumerService(null, null, null)

        then:
        Binding.HTTP_POST == serviceInfo.binding
        serviceInfo.url.contains("default.com")
    }

    def "check assertionconsumerservice with index"() {
        setup:
        MetadataConfigurationParser mcp = new MetadataConfigurationParser(
                [fakeSpMetadata])
        EntityDescriptor ed = mcp.entityDescriptors.find {true}.value
        EntityInformation entityInfo = new EntityInformation.Builder(ed, DEFAULT_BINDINGS).build()

        reflectAndSetDefaultAssertionConsumerService(entityInfo)

        when:
        def serviceInfo = entityInfo.getAssertionConsumerService(null, null, 0)

        then:
        Binding.HTTP_POST == serviceInfo.binding
    }

    def "parse sample sp metadata"() {
        setup:
        MetadataConfigurationParser mcp = new MetadataConfigurationParser([sampleSPMetadata])
        EntityDescriptor ed = mcp.entityDescriptors.find {true}.value

        when:
        new EntityInformation.Builder(ed, DEFAULT_BINDINGS).build()

        then:
        notThrown(Exception)
    }

    EntityInformation getFakeEntityInfo() {
        MetadataConfigurationParser mcp = new MetadataConfigurationParser([fakeSpMetadata])
        EntityDescriptor ed = mcp.entityDescriptors.find {true}.value
        return new EntityInformation.Builder(ed, DEFAULT_BINDINGS).build()
    }

    /**
     * Given all options to test this code path the best way with reflection, because the
     * variable itself should truly be private and never exposed, but the effort to configure
     * the sp in this precise state is vastly easier with a simple reflection.
     * @param entityInfo
     */
    private static void reflectAndSetDefaultAssertionConsumerService(EntityInformation entityInfo) {
        Field f = entityInfo.class.getDeclaredField("defaultAssertionConsumerService")
        f.setAccessible(true)
        f.set(entityInfo,
                new EntityInformation.ServiceInfo("https://default.com", Binding.HTTP_POST, null))
        f.setAccessible(false)
    }

    String fakeSpHalfSupportedMetadata = $/<md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" entityID="https://localhost:8993/services/saml">
<md:SPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
<md:KeyDescriptor use="signing">
<ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
<ds:X509Data>
<ds:X509Certificate>
$FAKE_SIGN_CERT
</ds:X509Certificate>
</ds:X509Data>
</ds:KeyInfo>
</md:KeyDescriptor>
<md:KeyDescriptor use="encryption">
<ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
<ds:X509Data>
<ds:X509Certificate>
$FAKE_ENCRYPT_CERT
</ds:X509Certificate>
</ds:X509Data>
</ds:KeyInfo>
</md:KeyDescriptor>
<md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact" Location="https://localhost:8993/services/saml/logout"/>
<md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="https://localhost:8993/services/saml/logout"/>
<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact" Location="https://localhost:8993/services/saml/sso" index="0"/>
<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="https://localhost:8993/services/saml/sso" index="1"/>
</md:SPSSODescriptor>
</md:EntityDescriptor>/$

    String fakeSpNoPostMetadata = $/<md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" entityID="https://localhost:8993/services/saml">
<md:SPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
<md:KeyDescriptor use="signing">
<ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
<ds:X509Data>
<ds:X509Certificate>
$FAKE_SIGN_CERT
</ds:X509Certificate>
</ds:X509Data>
</ds:KeyInfo>
</md:KeyDescriptor>
<md:KeyDescriptor use="encryption">
<ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
<ds:X509Data>
<ds:X509Certificate>
$FAKE_ENCRYPT_CERT
</ds:X509Certificate>
</ds:X509Data>
</ds:KeyInfo>
</md:KeyDescriptor>
<md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact" Location="https://localhost:8993/services/saml/logout"/>
<md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://localhost:8993/services/saml/logout"/>
<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact" Location="https://localhost:8993/services/saml/sso" index="0"/>
<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://localhost:8993/services/saml/sso" index="1"/>
</md:SPSSODescriptor>
</md:EntityDescriptor>/$

    String fakeSpMetadata = $/<md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" entityID="https://localhost:8993/services/saml">
<md:SPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
<md:KeyDescriptor use="signing">
<ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
<ds:X509Data>
<ds:X509Certificate>
$FAKE_SIGN_CERT
</ds:X509Certificate>
</ds:X509Data>
</ds:KeyInfo>
</md:KeyDescriptor>
<md:KeyDescriptor use="encryption">
<ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
<ds:X509Data>
<ds:X509Certificate>
$FAKE_ENCRYPT_CERT
</ds:X509Certificate>
</ds:X509Data>
</ds:KeyInfo>
</md:KeyDescriptor>
<md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://localhost:8993/services/saml/logout"/>
<md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="https://localhost:8993/services/saml/logout"/>
<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://localhost:8993/services/saml/sso" index="0"/>
<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="https://localhost:8993/services/saml/sso" index="1"/>
</md:SPSSODescriptor>
</md:EntityDescriptor>/$

    String sampleSPMetadata = $/<md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" entityID="https://localhost:8993/services/saml">
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
<md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://localhost:8993/services/saml/logout"/>
<md:SingleLogoutService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="https://localhost:8993/services/saml/logout"/>
<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://localhost:8993/services/saml/sso" index="0"/>
<md:AssertionConsumerService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="https://localhost:8993/services/saml/sso" index="1"/>
</md:SPSSODescriptor>
</md:EntityDescriptor>/$
}
