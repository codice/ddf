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
package ddf.catalog.ftp;

import ddf.catalog.ftp.user.FtpUser;
import ddf.security.Subject;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.STSAuthenticationTokenFactory;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link UserManager} that is registered to an FTP server and performs user authorization. */
public class UserManagerImpl implements UserManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserManagerImpl.class);

  private static final int MAX_IDLE_TIME_SECONDS = 300;

  private SecurityManager securityManager;

  private ContextPolicyManager contextPolicyManager;

  private String karafLocalRoles;

  private String uploadDirectory;

  private String admin;

  private Map<String, User> users;

  public UserManagerImpl(
      SecurityManager securityManager, ContextPolicyManager contextPolicyManager) {
    notNull(securityManager, "securityManager");

    this.securityManager = securityManager;
    this.contextPolicyManager = contextPolicyManager;
    users = new HashMap<>();
  }

  public void save(User user) {
    String username = user.getName();
    if (!doesExist(username)) {
      users.put(username, user);
    }
  }

  public void delete(String usrName) throws FtpException {
    users.remove(usrName);
  }

  public String[] getAllUserNames() {
    return usersToStringArray(users.keySet().toArray());
  }

  public User getUserByName(String userName) {
    return users.get(userName);
  }

  public boolean doesExist(String name) {
    return users.containsKey(name);
  }

  /**
   * @param authentication The {@link Authentication} that proves the users identity. {@link
   *     org.apache.ftpserver.usermanager.AnonymousAuthentication} is not permitted
   * @return {@link User} upon successful authorization
   * @throws AuthenticationFailedException upon unsuccessful authorization
   */
  public User authenticate(Authentication authentication) throws AuthenticationFailedException {
    BaseAuthenticationToken authenticationToken;
    String username;
    User user;

    if (authentication instanceof UsernamePasswordAuthentication) {
      username = ((UsernamePasswordAuthentication) authentication).getUsername();
      authenticationToken =
          new STSAuthenticationTokenFactory()
              .fromUsernamePassword(
                  username,
                  ((UsernamePasswordAuthentication) authentication).getPassword(),
                  ((UsernamePasswordAuthentication) authentication)
                      .getUserMetadata()
                      .getInetAddress()
                      .getHostAddress());
      authenticationToken.setAllowGuest(contextPolicyManager.getGuestAccess());

      try {
        Subject subject = securityManager.getSubject(authenticationToken);

        if (subject != null) {
          if (!doesExist(username)) {
            user = createUser(username, subject);
          } else {
            user = getUserByName(username);
            updateUserSubject(user, subject);
          }
          return user;
        }
      } catch (SecurityServiceException e) {
        LOGGER.info("Failure to retrieve subject.", e);
        throw new AuthenticationFailedException("Failure to retrieve subject.");
      }
    }

    throw new AuthenticationFailedException("Authentication failed");
  }

  private User updateUserSubject(User user, Subject subject) {
    ((FtpUser) user).setSubject(subject);
    return user;
  }

  @Override
  public String getAdminName() {
    return admin;
  }

  @Override
  public boolean isAdmin(String username) {
    return checkAdmin(username);
  }

  /**
   * @param userName name of the user being authenticated
   * @param subject {@link Subject} of the user
   * @return {@link FtpUser}
   */
  protected FtpUser createUser(String userName, Subject subject) {
    FtpUser user = new FtpUser();
    user.setName(userName);
    user.setEnabled(true);
    user.setHomeDirectory(uploadDirectory);

    List<Authority> authorities = new ArrayList<>();

    authorities.add(new WritePermission());
    authorities.add(new ConcurrentLoginPermission(0, 0));
    authorities.add(new TransferRatePermission(0, 0));

    user.setAuthorities(authorities);
    user.setMaxIdleTime(MAX_IDLE_TIME_SECONDS);

    user.setSubject(subject);

    setAdmin(user);

    save(user);

    return user;
  }

  public void setUploadDirectory(String directoryPath) {
    uploadDirectory = directoryPath;
  }

  private String[] usersToStringArray(Object[] users) {
    String[] strArray = new String[users.length];
    for (int i = 0; i < users.length; i++) {
      strArray[i] = (String) users[i];
    }
    return strArray;
  }

  public void setKarafLocalRoles(String roles) {
    this.karafLocalRoles = roles;
  }

  private boolean checkAdmin(String username) {
    return this.admin.equals(username);
  }

  /**
   * The {@code UserManager} interface expects only one admin. The first admin to login receives
   * admin status for this {@code UserManager}
   *
   * @param user {@link User} eligible for admin
   */
  private void setAdmin(User user) {
    if (StringUtils.isEmpty(this.admin)) {
      Subject subject = ((FtpUser) user).getSubject();
      for (String role : karafLocalRoles.split(",")) {
        if (role.equalsIgnoreCase("admin") && subject.hasRole(role)) {
          this.admin = user.getName();
        }
      }
    }
  }

  private void notNull(Object object, String name) {
    if (object == null) {
      String msg = name + " cannot be null";
      LOGGER.debug(msg);
      throw new IllegalArgumentException(msg);
    }
  }
}
