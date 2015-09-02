package ddf.security.certificate.generator;

import org.bouncycastle.asn1.x500.X500Name;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Created by aaronhoffer on 9/1/15.
 */

@RunWith(MockitoJUnitRunner.class)


public class PkiToolsTest {

PkiTools tools;

    @Before
            public void setup()
    {
        tools = new PkiTools();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nameIsNull() throws CertificateEncodingException {

        tools.makeDistinguishedName(null);
    }

    @Test
    public void nameIsEmptyString() throws CertificateEncodingException {

        X500Name name = tools.makeDistinguishedName("");
        assertThat(name.toString(), equalTo("cn="));
    }


    @Test
    public void convertCertificate() throws CertificateException {
        String originalCert = CertificateAuthority.pemDemoCaCertificate;
        X509Certificate cert = tools.pemToCertificate(originalCert);
        String certString = tools.certificateToPem(cert);
        assertThat(certString, equalTo(originalCert));
    }

}