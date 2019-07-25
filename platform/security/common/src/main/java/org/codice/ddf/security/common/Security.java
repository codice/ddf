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
package org.codice.ddf.security.common;

import static org.apache.commons.lang.Validate.notNull;

import com.google.common.annotations.VisibleForTesting;
import ddf.security.Subject;
import ddf.security.SubjectUtils;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.security.auth.AuthPermission;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.subject.ExecutionException;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.GuestAuthenticationToken;
import org.codice.ddf.security.handler.api.STSAuthenticationTokenFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton class that provides common security related utility functions. To use, get the instance
 * of the class using {@link #getInstance()} and call the desired method, or inject as a dependency.
 *
 * <p>IMPORTANT: New methods added to this class should be non-static.
 */
public class Security {

  private static final Security INSTANCE = new Security();

  private static final Logger LOGGER = LoggerFactory.getLogger(Security.class);

  private static final String INSUFFICIENT_PERMISSIONS_ERROR =
      "Current user doesn't have sufficient privileges to run this command";

  private static final String KARAF_LOCAL_ROLE = "karaf.local.roles";

  private static final AuthPermission GET_SYSTEM_SUBJECT_PERMISSION =
      new AuthPermission("getSystemSubject");

  private Subject cachedSystemSubject;

  private static final javax.security.auth.Subject JAVA_ADMIN_SUBJECT = getAdminJavaSubject();

  private Security() {
    // Singleton
  }

  /** @return unique instance of this class. Never {@code null}. */
  public static Security getInstance() {
    return INSTANCE;
  }

  /**
   * Gets the {@link Subject} given a user name and password.
   *
   * @param username username
   * @param password password
   * @return {@link Subject} associated with the user name and password provided
   */
  public Subject getSubject(String username, String password, String ip) {
    STSAuthenticationTokenFactory tokenFactory = createBasicTokenFactory();
    BaseAuthenticationToken token = tokenFactory.fromUsernamePassword(username, password, ip);
    SecurityManager securityManager = getSecurityManager();

    if (securityManager != null) {
      try {
        // TODO - Change when class is a service
        token.setAllowGuest(true);
        return securityManager.getSubject(token);
      } catch (SecurityServiceException | RuntimeException e) {
        LOGGER.info("Unable to request subject for {} user.", username, e);
      }
    }
    return null;
  }

  /**
   * Determines if the current Java {@link Subject} has the admin role.
   *
   * @return {@code true} if the Java {@link Subject} exists and has the admin role, {@code false}
   *     otherwise
   * @throws SecurityException if a security manager exists and the {@link
   *     javax.security.auth.AuthPermission AuthPermission("getSubject")} permission is not
   *     authorized
   */
  public final boolean javaSubjectHasAdminRole() {
    javax.security.auth.Subject subject =
        javax.security.auth.Subject.getSubject(AccessController.getContext());
    if (subject != null) {
      String localRoles =
          AccessController.doPrivileged(
              (PrivilegedAction<String>) () -> System.getProperty(KARAF_LOCAL_ROLE, ""));
      Collection<RolePrincipal> principals = new ArrayList<>();
      for (String role : localRoles.split(",")) {
        principals.add(new RolePrincipal(role));
      }
      return subject.getPrincipals().containsAll(principals);
    }
    return false;
  }

  /**
   * Runs the {@link Callable} in the current thread as the current security framework's {@link
   * Subject}. If the security framework's {@link Subject} is not currently set and the Java Subject
   * contains the admin role, elevates and runs the {@link Callable} as the system {@link Subject}.
   *
   * @param codeToRun code to run
   * @param <T> type of the returned value
   * @return value returned by the {@link Callable}
   * @throws SecurityServiceException if the current subject didn' have enough permissions to run
   *     the code
   * @throws SecurityException if a security manager exists and the {@link
   *     javax.security.auth.AuthPermission AuthPermission("getSystemSubject")} or {@link
   *     javax.security.auth.AuthPermission AuthPermission("getSubject")} permissions are not
   *     authorized
   * @throws InvocationTargetException wraps any exception thrown by {@link Callable#call()}. {@link
   *     Callable} exception can be retrieved using the {@link
   *     InvocationTargetException#getCause()}.
   */
  public <T> T runWithSubjectOrElevate(Callable<T> codeToRun)
      throws SecurityServiceException, InvocationTargetException {
    notNull(codeToRun, "Callable cannot be null");

    try {
      final org.apache.shiro.subject.Subject shiroSubject = getShiroSubject();

      if (shiroSubject != null) {
        return shiroSubject.execute(codeToRun);
      } else {
        LOGGER.debug("No shiro subject available for running command, trying with Java Subject");
      }
      Subject subject = getSystemSubject();

      if (subject == null) {
        auditInsufficientPermissions();
        throw new SecurityServiceException(INSUFFICIENT_PERMISSIONS_ERROR);
      }

      auditSystemSubjectElevation();
      return subject.execute(codeToRun);
    } catch (ExecutionException e) {
      auditFailedCodeExecutionForSystemSubject(e);
      throw new InvocationTargetException(e.getCause());
    }
  }

  /**
   * Gets the {@link Subject} associated with this system. Uses a cached subject since the subject
   * will not change between calls.
   *
   * @return system's {@link Subject} or {@code null} if unable to get the system's {@link Subject}
   * @throws SecurityException if a security manager exists and the {@link
   *     javax.security.auth.AuthPermission AuthPermission("getSystemSubject")} or {@link
   *     javax.security.auth.AuthPermission AuthPermission("getSubject")} permissions are not
   *     authorized
   */
  @Nullable
  public final synchronized Subject getSystemSubject() {
    auditSystemSubjectAccess();
    final java.lang.SecurityManager security = System.getSecurityManager();

    if (security != null) {
      security.checkPermission(Security.GET_SYSTEM_SUBJECT_PERMISSION);
    }
    if (!javaSubjectHasAdminRole()) {
      SecurityLogger.audit("Unable to retrieve system subject.");
      return null;
    }

    if (!tokenAboutToExpire(cachedSystemSubject)) {
      return cachedSystemSubject;
    }

    KeyStore keyStore =
        AccessController.doPrivileged((PrivilegedAction<KeyStore>) this::getSystemKeyStore);
    String alias = null;
    Certificate cert = null;
    try {
      if (keyStore != null) {
        if (keyStore.size() == 1) {
          alias = keyStore.aliases().nextElement();
        } else if (keyStore.size() > 1) {
          alias = getCertificateAlias();
        }
        cert = keyStore.getCertificate(alias);
      }
    } catch (KeyStoreException e) {
      LOGGER.warn("Unable to get certificate for alias [{}]", alias, e);
      return null;
    }

    if (cert == null) {
      LOGGER.warn("Unable to get certificate for alias [{}]", alias);
      return null;
    }

    STSAuthenticationTokenFactory tokenFactory = createBasicTokenFactory();
    BaseAuthenticationToken token =
        tokenFactory.fromCertificates(new X509Certificate[] {(X509Certificate) cert}, "127.0.0.1");
    if (token != null) {
      token.setAllowGuest(true);
      SecurityManager securityManager = getSecurityManager();
      if (securityManager != null) {
        try {
          cachedSystemSubject = securityManager.getSubject(token);
        } catch (SecurityServiceException sse) {
          LOGGER.warn("Unable to request subject for system user.", sse);
        }
      }
    }
    return cachedSystemSubject;
  }

  /**
   * Gets the guest {@link Subject} associated with the specified IP. Uses a cached subject when
   * possible since the subject will not change between calls.
   *
   * @return system's {@link Subject}
   */
  public Subject getGuestSubject(String ipAddress) {
    Subject subject = null;
    GuestAuthenticationToken token = new GuestAuthenticationToken(ipAddress);
    LOGGER.debug("Getting new Guest user token for {}", ipAddress);
    try {
      SecurityManager securityManager = getSecurityManager();
      if (securityManager != null) {
        subject = securityManager.getSubject(token);
      }
    } catch (SecurityServiceException sse) {
      LOGGER.info("Unable to request subject for guest user.", sse);
    }

    return subject;
  }

  /**
   * Determines whether a {@link Subject}'s token is about to expire or not.
   *
   * @param subject subject whose token needs to be checked
   * @return {@code true} only if the {@link Subject}'s token will expire soon
   */
  public boolean tokenAboutToExpire(Subject subject) {
    return !((null != subject)
        && (null != subject.getPrincipals())
        && (!subject.getPrincipals().byType(SecurityAssertion.class).isEmpty())
        && (!areAnyAboutToExpire(
            subject
                .getPrincipals()
                .byType(SecurityAssertion.class)
                .stream()
                .map(SecurityAssertion::getNotOnOrAfter)
                .map(Date::toInstant)
                .collect(Collectors.toList()),
            TimeUnit.MINUTES.toSeconds(1))));
  }

  public boolean areAnyAboutToExpire(List<Instant> expireList, long secondsToExpiry) {
    for (Instant expire : expireList) {
      if (isAboutToExpire(expire, secondsToExpiry)) {
        return true;
      }
    }
    return false;
  }

  public boolean isAboutToExpire(Instant expires, long secondsToExpiry) {
    if (expires != null && secondsToExpiry > 0) {
      Instant now = Instant.now().plusSeconds(secondsToExpiry);
      return expires.isBefore(now);
    }
    return false;
  }

  /**
   * Get the expires time from the {@link Subject}'s token.
   *
   * @param subject subject whose token needs to be checked
   * @return {@code Date} or null if subject doesn't have an expire time.
   */
  public Date getExpires(Subject subject) {
    return ((null != subject)
            && (null != subject.getPrincipals())
            && !(subject.getPrincipals().byType(SecurityAssertion.class)).isEmpty())
        ? subject
            .getPrincipals()
            .byType(SecurityAssertion.class)
            .stream()
            .sorted(SubjectUtils.getAssertionComparator())
            .map(SecurityAssertion::getNotOnOrAfter)
            .findFirst()
            .orElse(null)
        : null;
  }

  /**
   * Gets a reference to the {@link SecurityManager}.
   *
   * @return reference to the {@link SecurityManager} or {@code null} if unable to get the {@link
   *     SecurityManager}
   */
  @Nullable
  public SecurityManager getSecurityManager() {
    BundleContext context = getBundleContext();
    if (context != null) {
      ServiceReference securityManagerRef = context.getServiceReference(SecurityManager.class);
      if (securityManagerRef != null) {
        return (SecurityManager) context.getService(securityManagerRef);
      }
    }
    LOGGER.warn(
        "Unable to get Security Manager. Authentication and Authorization mechanisms will not work correctly. A restart of the system may be necessary.");
    return null;
  }

  public <T> T runAsAdmin(PrivilegedAction<T> action) {
    return javax.security.auth.Subject.doAs(JAVA_ADMIN_SUBJECT, action);
  }

  public <T> T runAsAdminWithException(PrivilegedExceptionAction<T> action)
      throws PrivilegedActionException {
    return javax.security.auth.Subject.doAs(JAVA_ADMIN_SUBJECT, action);
  }

  private static javax.security.auth.Subject getAdminJavaSubject() {
    Set<Principal> principals = new HashSet<>();
    String localRoles =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty(KARAF_LOCAL_ROLE, ""));
    for (String role : localRoles.split(",")) {
      principals.add(new RolePrincipal(role));
    }
    return new javax.security.auth.Subject(true, principals, new HashSet(), new HashSet());
  }

  @VisibleForTesting
  @Nullable
  org.apache.shiro.subject.Subject getShiroSubject() {
    try {
      return org.apache.shiro.SecurityUtils.getSubject();
    } catch (IllegalStateException | UnavailableSecurityManagerException e) { // ignore
    }
    return null;
  }

  @VisibleForTesting
  BundleContext getBundleContext() {
    Bundle bundle = FrameworkUtil.getBundle(Security.class);
    if (bundle != null) {
      return bundle.getBundleContext();
    }
    return null;
  }

  @VisibleForTesting
  void auditFailedCodeExecutionForSystemSubject(ExecutionException e) {
    SecurityLogger.auditWarn("Failed to execute code as System subject", e.getCause());
  }

  @VisibleForTesting
  void auditInsufficientPermissions() {
    SecurityLogger.audit(INSUFFICIENT_PERMISSIONS_ERROR);
  }

  @VisibleForTesting
  void auditSystemSubjectElevation() {
    SecurityLogger.auditWarn("Elevating current user permissions to use System subject");
  }

  @VisibleForTesting
  void auditSystemSubjectAccess() {
    SecurityLogger.audit("Attempting to get System Subject");
  }

  private STSAuthenticationTokenFactory createBasicTokenFactory() {
    STSAuthenticationTokenFactory tokenFactory = new STSAuthenticationTokenFactory();
    tokenFactory.init();
    return tokenFactory;
  }

  private String getCertificateAlias() {
    return System.getProperty("org.codice.ddf.system.hostname");
  }

  public KeyStore getSystemKeyStore() {
    KeyStore keyStore;

    try {
      keyStore = KeyStore.getInstance(System.getProperty("javax.net.ssl.keyStoreType"));

    } catch (KeyStoreException e) {
      LOGGER.warn(
          "Unable to create keystore instance of type {}",
          System.getProperty("javax.net.ssl.keyStoreType"),
          e);
      return null;
    }

    Path keyStoreFile = new File(System.getProperty("javax.net.ssl.keyStore")).toPath();
    Path ddfHomePath = Paths.get(System.getProperty("ddf.home"));

    if (!keyStoreFile.isAbsolute()) {
      keyStoreFile = Paths.get(ddfHomePath.toString(), keyStoreFile.toString());
    }

    String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");

    if (!Files.isReadable(keyStoreFile)) {
      LOGGER.warn("Unable to read system key/trust store files: [ {} ] ", keyStoreFile);
      return null;
    }

    try (InputStream kfis = Files.newInputStream(keyStoreFile)) {
      keyStore.load(kfis, keyStorePassword.toCharArray());
    } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
      LOGGER.warn("Unable to load system key file.", e);
    }

    return keyStore;
  }
}
