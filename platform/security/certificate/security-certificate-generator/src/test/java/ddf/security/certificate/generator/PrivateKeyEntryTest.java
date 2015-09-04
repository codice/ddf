package ddf.security.certificate.generator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;


@RunWith(MockitoJUnitRunner.class)
public class PrivateKeyEntryTest {

    @Mock
    X509Certificate subjectCert;
    @Mock
    PrivateKey subjectPrivateKey;
    @Mock
    X509Certificate issuerCert;

    @Test
    public void createAndAccess() throws Exception {
        PrivateKeyEntry pkEntry = new PrivateKeyEntry(subjectCert, subjectPrivateKey, issuerCert);
        assertThat(pkEntry.getIssuerCertificate(), sameInstance(issuerCert));
        assertThat(pkEntry.getSubjectCertificate(), sameInstance(subjectCert));
        assertThat(pkEntry.getSubjectPrivateKey(), sameInstance(subjectPrivateKey));
        X509Certificate[] chain = pkEntry.getCertificateChain();
        assertThat(chain[0], sameInstance(subjectCert));
        assertThat(chain[1], sameInstance(issuerCert));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidSubjectCert() {
        new PrivateKeyEntry(null, subjectPrivateKey, subjectCert);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidIssuerCert() {
        new PrivateKeyEntry(subjectCert, subjectPrivateKey, null);

    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidKeyCert() {
        new PrivateKeyEntry(subjectCert, null, issuerCert);
    }

}