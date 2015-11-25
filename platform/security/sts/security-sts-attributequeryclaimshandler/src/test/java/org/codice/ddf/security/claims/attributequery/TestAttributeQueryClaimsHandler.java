package org.codice.ddf.security.claims.attributequery;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ddf.security.encryption.EncryptionService;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;

public class TestAttributeQueryClaimsHandler {

    private static final String USERNAME = "admin";

    private static final String EXTERNAL_ATTRIBUTE_STORE_URL = "https://localhost:8993/";

    private static final String ISSUER = "ddf";

    private static final String DESTINATION = "SomeServer";

    private List<String> supportedClaims;

    private EncryptionService encryptionService;

    private SystemCrypto systemCrypto;

    private SimpleSign simpleSign;

    @BeforeClass
    public static void init() {
        OpenSAMLUtil.initSamlEngine();
    }

    @Before
    public void setUp() {
        encryptionService = mock(EncryptionService.class);
        systemCrypto = new SystemCrypto("encryption.properties", "signature.properties",
                encryptionService);
        simpleSign = new SimpleSign(systemCrypto);

        supportedClaims = new ArrayList<>();
        supportedClaims.add("Role");
    }

    @Test
    public void testRetrieveClaimValues() {
        AttributeQueryClaimsHandler attributeQueryClaimsHandler = new AttributeQueryClaimsHandler();
        attributeQueryClaimsHandler.setSimpleSign(simpleSign);
        attributeQueryClaimsHandler.setExternalAttributeStoreUrl(EXTERNAL_ATTRIBUTE_STORE_URL);
        attributeQueryClaimsHandler.setIssuer(ISSUER);
        attributeQueryClaimsHandler.setDestination(DESTINATION);
        attributeQueryClaimsHandler.setSupportedClaims(supportedClaims);
    }
}
