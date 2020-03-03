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

/**
 * Stores a user's or client's access token, refresh token, and the list of sources authorized to
 * query.
 */
public interface TokenStorage {

  String ACCESS_TOKEN = "access_token";
  String REFRESH_TOKEN = "refresh_token";
  String DISCOVERY_URL = "discovery_url";
  String EXPIRES_AT = "expires_at";
  String CLIENT_ID = "client_id";
  String SECRET = "client_secret";
  String SOURCE_ID = "source_id";
  String STATE = "state";

  /**
   * @return a map containing state UUIDs with their corresponding ID, source ID, discovery URL,
   *     client ID, and secret information.
   */
  Map<String, Map<String, Object>> getStateMap();

  /**
   * Stores a user's or client's access token. If it's a new user or client, creates a new entry.
   * Otherwise, updates the existing data.
   *
   * @param id the ID used to store the tokens
   * @param sourceId the ID of the source the tokens are going to be used against
   * @param accessToken the access token
   * @param refreshToken the refresh token
   * @param discoveryUrl the metadata url of the OAuth provider protecting the source
   * @return an HTTP status code
   */
  int create(
      String id, String sourceId, String accessToken, String refreshToken, String discoveryUrl);

  /**
   * Reads tokens associated with the given ID
   *
   * @param id the ID used to retrieve tokens
   * @return a {@link TokenInformation} filled with tokens
   */
  TokenInformation read(String id);

  /**
   * Reads tokens associated with the given ID for the specified source
   *
   * @param id the ID used to retrieve tokens
   * @param sourceId the source ID the tokens correspond to
   * @return a {@link TokenInformation.TokenEntry} filled with tokens
   */
  TokenInformation.TokenEntry read(String id, String sourceId);

  /**
   * Checks if tokens are available
   *
   * @param id the ID used to check if tokens are available
   * @param sourceId the source ID the tokens correspond to
   * @return true if the tokens for the given ID and source are available and false if they are not
   */
  boolean isAvailable(String id, String sourceId);

  /**
   * Removes tokens associated with the given ID
   *
   * @param id the ID associated with the tokens
   * @return an HTTP status code
   */
  int delete(String id);

  /**
   * Removes tokens associated with the given ID for the specified source
   *
   * @param id the ID associated with the tokens
   * @param sourceId the ID for the source the tokens are going to be used against
   * @return an HTTP status code
   */
  int delete(String id, String sourceId);
}
