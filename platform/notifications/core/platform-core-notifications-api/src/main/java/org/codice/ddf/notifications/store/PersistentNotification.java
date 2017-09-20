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
package org.codice.ddf.notifications.store;

import java.util.HashMap;
import java.util.UUID;

/**
 * Serializable Notification that is persisted to disk by @FileSystemPersistenceProvider and
 * includes the unique ID generated as the cache key for the notification.
 */
public class PersistentNotification extends HashMap<String, String> {
  public static final String NOTIFICATION_KEY_UUID = "id";

  public static final String NOTIFICATION_KEY_APPLICATION = "application";

  public static final String NOTIFICATION_KEY_MESSAGE = "message";

  public static final String NOTIFICATION_KEY_TIMESTAMP = "timestamp";

  public static final String NOTIFICATION_KEY_TITLE = "title";

  public static final String NOTIFICATION_KEY_USER_ID = "user";

  private static final long serialVersionUID = -2531844838114289516L;

  /**
   * Constructs a Notification with the specified application name, title message, and timestamp.
   *
   * @param application The name of the application that triggered the generation of this {@code
   *     Notification}
   * @param title The title of this {@code Notification}
   * @param message The message associated with this {@code Notification}
   * @param timestamp A <code>long</code> representing the number of milliseconds between January 1,
   *     1970, 00:00:00 GMT and the point at which the event triggering this {@code Notification}
   *     was generated.
   */
  public PersistentNotification(String application, String title, String message, Long timestamp) {
    this(application, title, message, String.valueOf(timestamp), null);
  }

  /**
   * Constructs a Notification with the specified application name, title message, and timestamp.
   *
   * @param application The name of the application that triggered the generation of this {@code
   *     Notification}
   * @param title The title of this {@code Notification}
   * @param message The message associated with this {@code Notification}
   * @param timestamp A {@code String} representing the number of milliseconds between January 1,
   *     1970, 00:00:00 GMT and the point at which the event triggering this {@code Notification}
   *     was generated.
   */
  public PersistentNotification(
      String application, String title, String message, String timestamp) {
    this(application, title, message, timestamp, null);
  }

  /**
   * Constructs a Notification with the specified application name, title message, timestamp, and
   * user ID.
   *
   * @param application The name of the application that triggered the generation of this {@code
   *     Notification}
   * @param title The title of this {@code Notification}
   * @param message The message associated with this {@code Notification}
   * @param timestamp A <code>long</code> representing the number of milliseconds between January 1,
   *     1970, 00:00:00 GMT and the point at which the event triggering this {@code Notification}
   *     was generated.
   * @param userId The id of the user to which this {@code Notification} should be sent.
   */
  public PersistentNotification(
      String application, String title, String message, Long timestamp, String userId) {
    this(application, title, message, String.valueOf(timestamp), userId);
  }

  /**
   * Constructs a Notification with the specified application name, title message, timestamp, and
   * user ID.
   *
   * @param application The name of the application that triggered the generation of this {@code
   *     Notification}
   * @param title The title of this {@code Notification}
   * @param message The message associated with this {@code Notification}
   * @param timestamp A {@code String} representing the number of milliseconds between January 1,
   *     1970, 00:00:00 GMT and the point at which the event triggering this {@code Notification}
   *     was generated.
   * @param userId The id of the user to which this {@code Notification} should be sent.
   */
  public PersistentNotification(
      String application, String title, String message, String timestamp, String userId) {
    setId(UUID.randomUUID().toString().replaceAll("-", ""));
    setApplication(application);
    setTitle(title);
    setMessage(message);
    setTimestamp(timestamp);

    if (null != userId && !userId.isEmpty()) {
      setUserId(userId);
    }
  }

  /**
   * Returns the unique ID of the {@code Notification}
   *
   * @return The id of the {@code Notification}
   */
  public String getId() {
    return this.get(NOTIFICATION_KEY_UUID);
  }

  /**
   * Overwrites the unique ID of the {@code Notification}
   *
   * @param id The new unique ID of the {@code Notification}
   */
  public void setId(String id) {
    this.put(NOTIFICATION_KEY_UUID, id);
  }

  /**
   * Returns the name of the application that triggered the generation of the {@code Notification}
   *
   * @return The name of the application that triggered the generation of the {@code Notification}
   */
  public String getApplication() {
    return this.get(NOTIFICATION_KEY_APPLICATION);
  }

  /**
   * Overwrites the name of the application that triggered the generation of the {@code
   * Notification}
   *
   * @param application The new name of the application that triggered the generation of the {@code
   *     Notification}
   */
  public void setApplication(String application) {
    this.put(NOTIFICATION_KEY_APPLICATION, application);
  }

  /**
   * Returns the name of the title of the {@code Notification}
   *
   * @return The title of the {@code Notification}
   */
  public String getTitle() {
    return this.get(NOTIFICATION_KEY_TITLE);
  }

  /**
   * Overwrites the title of the {@code Notification}
   *
   * @param title The new title of the {@code Notification}
   */
  public void setTitle(String title) {
    this.put(NOTIFICATION_KEY_TITLE, title);
  }

  /**
   * Returns the name of the message associated with the {@code Notification}
   *
   * @return The message associated with the {@code Notification}
   */
  public String getMessage() {
    return this.get(NOTIFICATION_KEY_MESSAGE);
  }

  /**
   * Overwrites the message associated with the {@code Notification}
   *
   * @param message The new message associated with the {@code Notification}
   */
  public void setMessage(String message) {
    this.put(NOTIFICATION_KEY_MESSAGE, message);
  }

  /**
   * Returns a {@code String} depicting the time at which the event that triggered this {@code
   * Notification} occurred.
   *
   * @return A {@code String} representing the number of milliseconds between January 1, 1970,
   *     00:00:00 GMT and the point at which the event that triggered this {@code Notification}
   *     occurred.
   */
  public String getTimestampString() {
    return this.get(NOTIFICATION_KEY_TIMESTAMP);
  }

  /**
   * Returns a <code>long</code> depicting the time at which the event that triggered this {@code
   * Notification} occurred.
   *
   * @return A <code>long</code> representing the number of milliseconds between January 1, 1970,
   *     00:00:00 GMT and the point at which the event that triggered this {@code Notification}
   *     occurred.
   */
  public Long getTimestampLong() {
    return Long.valueOf(this.get(NOTIFICATION_KEY_TIMESTAMP));
  }

  /**
   * Overwrites the timestamp that depicts the time at which the event that triggered the {@code
   * Notification} occurred.
   *
   * @param timestampString A {@code String} representing the number of milliseconds between January
   *     1, 1970, 00:00:00 GMT and the point at which the event that triggered this {@code
   *     Notification} occurred.
   */
  public void setTimestamp(String timestampString) {
    this.put(NOTIFICATION_KEY_TIMESTAMP, timestampString);
  }

  /**
   * Overwrites the timestamp that depicts the time at which the event that triggered the {@code
   * Notification} occurred.
   *
   * @param timestamp A <code>long</code> representing the number of milliseconds between January 1,
   *     1970, 00:00:00 GMT and the point at which the event that triggered this {@code
   *     Notification} occurred.
   */
  public void setTimestamp(Long timestamp) {
    this.put(NOTIFICATION_KEY_TIMESTAMP, String.valueOf(timestamp));
  }

  /**
   * Returns the id of the user to whom this {@code Notification} is addressed.
   *
   * @return The id of the user to whom this {@code Notification} is addressed.
   */
  public String getUserId() {
    return this.get(NOTIFICATION_KEY_USER_ID);
  }

  /**
   * Overwrites the id of the user to whom the {@code Notification} is addressed.
   *
   * @param userId The new userId to whom the {@code Notification} should be addressed.
   */
  public void setUserId(String userId) {
    this.put(NOTIFICATION_KEY_USER_ID, userId);
  }
}
