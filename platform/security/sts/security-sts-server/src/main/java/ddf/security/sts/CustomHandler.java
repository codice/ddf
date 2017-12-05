package ddf.security.sts;

import org.apache.cxf.sts.token.provider.SamlCustomHandler;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.core.impl.SubjectConfirmationDataBuilder;

/**
 * Custom handler to add/alter SAML properties prior to being signed.
 */
public class CustomHandler implements SamlCustomHandler {

  @Override
  public void handle(
      SamlAssertionWrapper assertionWrapper, TokenProviderParameters tokenParameters) {

    final SubjectConfirmationData scd = new SubjectConfirmationDataBuilder().buildObject();
    scd.setNotOnOrAfter(DateTime.now().plusMinutes(30));
    scd.setRecipient(getAssertionConsumerServiceUrl());

    assertionWrapper
        .getSaml2()
        .getSubject()
        .getSubjectConfirmations()
        .forEach(sc -> sc.setSubjectConfirmationData(scd));
  }

  private String getAssertionConsumerServiceUrl() {
    // TODO RAP 15 Nov 17: Is this correct? And if so, should we extract to a common service?

    String hostname = SystemBaseUrl.getHost();
    String port = SystemBaseUrl.getPort();
    String rootContext = SystemBaseUrl.getRootContext();

    return String.format("https://%s:%s%s/saml/sso", hostname, port, rootContext);
  }
}
