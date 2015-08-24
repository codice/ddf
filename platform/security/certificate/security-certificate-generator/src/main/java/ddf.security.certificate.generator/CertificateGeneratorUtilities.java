package ddf.security.certificate.generator;

import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;

/**
 * Class to put helper functions that were common to several classes,
 * but where the classes did not form a logical hierarchy. Considered making
 * more wrapper classes-- for example, KeyWrapper that would add methods like
 * "asString" or static method "fromString" to create new instances. But there
 * are already too many classes and too many wrappers.
 */
public class CertificateGeneratorUtilities {

    public static String BC = BouncyCastleProvider.PROVIDER_NAME;

    /**
     * Convert a byte array to a Java String.
     *
     * @param bytes DER encoded bytes
     * @return PEM encoded bytes
     */
    public static String bytesToString(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Convert a Java String to a byte array
     *
     * @param string PEM encoded bytes
     * @return DER encoded bytes
     */
    public static byte[] stringToBytes(String string) {
        return Base64.getDecoder().decode(string);
    }

    /**
     * Get the host name or DNS name that Java thinks is associated with the server running the application. This
     * method is public so client code can easily check the name and decide if it should be used in the generated
     * certificate.
     *
     * @return Hostname of this machine. Hostname should be the same as the machine's DNS name.
     * @throws UnknownHostException
     */
    public static String getHostName() throws UnknownHostException {

        return InetAddress.getLocalHost().getHostName();

    }

    public static String certificateToString(X509Certificate cert ) throws CertificateEncodingException {
        return bytesToString(cert.getEncoded());
    }

    public static X509Certificate stringToCertificate(String certString) throws CertificateException {
        CertificateFactory cf = new CertificateFactory();
        ByteArrayInputStream in = new ByteArrayInputStream(stringToBytes(certString));
        X509Certificate cert = (X509Certificate) cf.engineGenerateCertificate(in);
        return cert;
    }

    /**
     * Convert an instance of Key to a (PEM-encoded) instance of Java String.
     */
    public String keyToString(Key key) {
        return bytesToString(key.getEncoded());
    }

    /**
     * Convert a Java String to an  private key
     *
     * @param keyString encoded RSA private key. Assume PKCS#8 format
     * @return Instance of PrivateKey
     * @throws CertificateGeneratorException Raise exception if conversion was not successful
     */
    public static PrivateKey stringToPrivateKey(String keyString) throws CertificateGeneratorException {
        try {
            return getRsaKeyFactory().generatePrivate(new PKCS8EncodedKeySpec(stringToBytes(keyString)));
        } catch (Exception e) {
            throw new CertificateGeneratorException.InvalidKey("Could not convert String to Private Key", e.getCause());
        }
    }

    //DDF uses RSA asymmetric keys
    protected static KeyFactory getRsaKeyFactory() throws NoSuchProviderException, NoSuchAlgorithmException {
        return KeyFactory.getInstance("RSA", "BC");
    }
}