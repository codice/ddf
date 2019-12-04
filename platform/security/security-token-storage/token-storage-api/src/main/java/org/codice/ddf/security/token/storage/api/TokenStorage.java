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
package org.codice.ddf.security.token.storage.api;

import java.util.Map;

/** Stores a user's access token, refresh token, and the list of sources authorized to query. */
public interface TokenStorage {

  String ACCESS_TOKEN = "access_token";
  String REFRESH_TOKEN = "refresh_token";
  String DISCOVERY_URL = "discovery_url";
  String EXPIRES_AT = "expires_at";
  String CLIENT_ID = "client_id";
  String SECRET = "client_secret";
  String SOURCE_ID = "source_id";
  String USER_ID = "user_id";
  String STATE = "state";

  /**
   * @return a map containing state UUIDs with their corresponding user ID, source ID, discovery
   *     URL, client ID, and secret information.
   */
  Map<String, Map<String, Object>> getStateMap();

  /**
   * Stores a user's data. If it's a new user, creates a new entry. Otherwise, updates the existing
   * user's data
   *
   * @param userId the user's email address or username if an email address is not available
   * @param sourceId the ID of the source the tokens are going to be used against
   * @param accessToken the user's access token
   * @param refreshToken the user's refresh token
   * @param discoveryUrl the metadata url of the Oauth provider protecting the source
   * @return an HTTP status code
   */
  int create(
      String userId, String sourceId, String accessToken, String refreshToken, String discoveryUrl);

  /**
   * Reads given user's information
   *
   * @param userId the user's email address or username if an email address is not available
   * @return a {@link TokenInformation} filled with the user's tokens
   */
  TokenInformation read(String userId);

  /**
   * Reads given user's tokens for the specified source
   *
   * @param userId the user's email address or username if an email address is not available
   * @param sourceId the source id the tokens correspond to
   * @return a {@link TokenInformation.TokenEntry} filled with the user's tokens
   */
  TokenInformation.TokenEntry read(String userId, String sourceId);

  /**
   * Checks if tokens are available
   *
   * @param userId the user's email address or username if an email address is not available
   * @param sourceId the source id the tokens correspond to
   * @return true if the tokens for the given user and source are available and false if they are
   *     not
   */
  boolean isAvailable(String userId, String sourceId);

  /**
   * Removes an existing user's tokens for the specified source
   *
   * @param userId the user's email address or username if an email address is not available
   * @param sourceId the ID for the source the tokens are going to be used against
   * @return an HTTP status code
   */
  int delete(String userId, String sourceId);
}
