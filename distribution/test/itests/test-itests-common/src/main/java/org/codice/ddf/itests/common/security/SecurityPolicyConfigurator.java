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
package org.codice.ddf.itests.common.security;

import static com.jayway.restassured.RestAssured.when;
import static org.codice.ddf.itests.common.WaitCondition.expect;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.ServiceManager;
import org.codice.ddf.itests.common.SynchronizedConfiguration;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.codice.ddf.security.policy.context.impl.PolicyManager;
import org.osgi.service.cm.ConfigurationAdmin;

public class SecurityPolicyConfigurator {

  private static final String SYMBOLIC_NAME = "security-policy-context";

  private static final String FACTORY_PID =
      "org.codice.ddf.security.policy.context.impl.PolicyManager";

  public static final String BASIC_AUTH_TYPES = "/=SAML|basic";

  public static final String GUEST_AUTH_TYPES = "/=SAML|GUEST,/admin=SAML|basic,/system=SAML|basic";

  public static final String DEFAULT_WHITELIST =
      "/services/SecurityTokenService,/services/internal/metrics,/services/saml,/proxy,/services/idp,/idp,/services/platform/config/ui,/login";

  private ServiceManager services;

  private ConfigurationAdmin configAdmin;

  public SecurityPolicyConfigurator(ServiceManager services, ConfigurationAdmin configAdmin) {
    this.services = services;
    this.configAdmin = configAdmin;
  }

  public void configureRestForGuest() throws Exception {
    configureRestForGuest(null);
  }

  public void configureRestForGuest(String whitelist) throws Exception {
    configureWebContextPolicy(null, GUEST_AUTH_TYPES, null, createWhitelist(whitelist));
  }

  public void configureRestForBasic() throws Exception {
    configureRestForBasic(null);
  }

  public void configureRestForBasic(String whitelist) throws Exception {
    configureWebContextPolicy(null, BASIC_AUTH_TYPES, null, createWhitelist(whitelist));
  }

  public void waitForBasicAuthReady(String url) {
    expect("Waiting for basic auth")
        .within(AbstractIntegrationTest.GENERIC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .checkEvery(1, TimeUnit.SECONDS)
        .until(() -> when().get(url).then().extract().statusCode() == 401);
  }

  public void waitForGuestAuthReady(String url) {
    expect("Waiting for guest auth")
        .within(AbstractIntegrationTest.GENERIC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .checkEvery(1, TimeUnit.SECONDS)
        .until(() -> when().get(url).then().extract().statusCode() == 200);
  }

  public static String createWhitelist(String whitelist) {
    return DEFAULT_WHITELIST + (StringUtils.isNotBlank(whitelist) ? "," + whitelist : "");
  }

  public void configureWebContextPolicy(
      String realms, String authTypes, String requiredAttributes, String whitelist)
      throws Exception {

    Map<String, Object> policyProperties = null;
    int retries = 0;

    while (policyProperties == null || policyProperties.isEmpty()) {
      policyProperties = services.getMetatypeDefaults(SYMBOLIC_NAME, FACTORY_PID);
      if (retries < 5 && policyProperties.isEmpty()) {
        Thread.sleep(10000);
      } else {
        break;
      }
      retries++;
    }

    if (realms != null) {
      putPolicyValues(policyProperties, "realms", realms);
    }
    if (authTypes != null) {
      putPolicyValues(policyProperties, "authenticationTypes", authTypes);
    }
    if (requiredAttributes != null) {
      putPolicyValues(policyProperties, "requiredAttributes", requiredAttributes);
    }
    if (whitelist != null) {
      putPolicyValues(policyProperties, "whiteListContexts", whitelist);
    }

    new SynchronizedConfiguration(
            FACTORY_PID, null, policyProperties, createChecker(policyProperties), configAdmin)
        .updateConfig();

    services.waitForAllBundles();
  }

  private void putPolicyValues(Map<String, Object> properties, String key, String value) {
    if (StringUtils.isNotBlank(value)) {
      properties.put(key, StringUtils.split(value, ","));
    }
  }

  private Callable<Boolean> createChecker(final Map<String, Object> policyProperties) {

    final ContextPolicyManager ctxPolicyMgr = services.getService(ContextPolicyManager.class);

    final PolicyManager targetPolicies = new PolicyManager();
    targetPolicies.setPolicies(policyProperties);

    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        for (ContextPolicy policy : ctxPolicyMgr.getAllContextPolicies()) {
          ContextPolicy targetPolicy = targetPolicies.getContextPolicy(policy.getContextPath());

          if (targetPolicy == null
              || !targetPolicy.getContextPath().equals(policy.getContextPath())
              || (targetPolicy.getRealm() != null
                  && !targetPolicy.getRealm().equals(policy.getRealm()))
              || !targetPolicy
                  .getAuthenticationMethods()
                  .containsAll(policy.getAuthenticationMethods())
              || !targetPolicy
                  .getAllowedAttributeNames()
                  .containsAll(policy.getAllowedAttributeNames())) {
            return false;
          }
        }

        return true;
      }
    };
  }
}
