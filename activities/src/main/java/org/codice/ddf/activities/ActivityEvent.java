/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.activities;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityEvent extends HashMap<String, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityEvent.class);

    private static final long serialVersionUID = -3965553379790729847L;

    public static final String ID_KEY = "id";
    
    public static final String SESSION_ID_KEY = "session";

    public static final String STATUS_KEY = "status";

    public static final String TITLE_KEY = "title";

    public static final String MESSAGE_KEY = "message";

    public static final String TIMESTAMP_KEY = "timestamp";

    public static final String OPERATIONS_KEY = "operations";

    public static final String PROGRESS_KEY = "progress";

    public static final String USER_ID_KEY = "user";

    public static final String CATEGORY_KEY = "category";

    public static final String BYTES_READ_KEY = "bytes";

    public static final String DOWNLOAD_ID_KEY = "download_id";

    public static final String EVENT_TOPIC = "ddf/activities";

    public static final String EVENT_TOPIC_DOWNLOAD_CANCEL = "ddf/download/cancel";

    /**
     * 
     * Status enumerations for an activity. Describes the various states an
     * activity can be in. <br/>
     * <br/>
     * <b>STARTED</b> - Initial status, the activity is in the process of
     * starting up and is not RUNNING yet. <br/>
     * <b>RUNNING</b> - Main state used to describe that the activity is
     * currently active and performing the activity. <br/>
     * <b>FINISHED</b> - The activity has finished its processing and will not
     * send any more statuses. <br/>
     * <b>STOPPED</b> - The activity has stopped running before finishing and
     * cannot be resumed. <br/>
     * <b>PAUSED</b> - The activity has been paused before finishing and can be
     * resumed. <br/>
     * <b>FAILED</b> - The activity suffered a failure before finishing and
     * cannot be resumed.
     * 
     */
    public enum ActivityStatus {
        STARTED, RUNNING, COMPLETE, STOPPED, PAUSED, FAILED
    }

    public ActivityEvent(String id, String sessionId, Date timestamp, String category, String title, String message,
            int progress, Map<String, String> operations, String user, ActivityStatus type, Long bytes) {
        setActivityId(id);
        setSessionId(sessionId);
        setTimestamp(timestamp);
        setCategory(category);
        setTitle(title);
        setMessage(message);
        setProgress(progress);
        setOperations(operations);
        setUserId(user);
        setBytesRead(bytes);
        this.put(STATUS_KEY, type.toString());
    }

    /**
     * Sets the operations that this activity can perform.
     * 
     * @param operations
     *            Map of operations where the key is the operation name and the
     *            value is a url to the operation
     */
    public void setOperations(Map<String, String> operations) {
        this.put(OPERATIONS_KEY, operations);
    }

    /**
     * Sets the progress of the activity.
     * 
     * @param progress
     *            Integer value representation of the progress, ranging from 0 to 100
     */
    public void setProgress(int progress) {
        this.put(PROGRESS_KEY, progress);
    }

    /**
     * Returns the progress of the activity.
     * 
     * @return progress
     *            Integer value representation of the progress, ranging from 0 to 100
     */
    public int getProgress() {
        return (Integer) this.get(PROGRESS_KEY);
    }

    /**
     * Return the type of {@code ActivityEvent} that is going on.
     * 
     * @return Type of the {@code ActivityEvent}, must follow the enumerated
     *         ActivityType values.
     */
    public String getActivityType() {
        return this.get(STATUS_KEY).toString();
    }

    /**
     * Set the id of the activity being performed.
     * 
     * @param id
     *            This id should be unique and used across the entire lifecycle
     *            of the {@code ActivityEvent}.
     */
    public void setActivityId(String id) {
        this.put(ID_KEY, id);
    }

    /**
     * Retrieves the ID of the current {@code ActivityEvent}.
     * 
     * @return ID of the {@code ActivityEvent}.
     */
    public String getActivityId() {
        return this.get(ID_KEY).toString();
    }

    /**
     * Set the session id of the activity being performed.
     * 
     * @param sessionId
     *            This session id should be unique and used across the entire lifecycle
     *            of the {@code ActivityEvent}.
     */
    public void setSessionId(String sessionId) {
        this.put(SESSION_ID_KEY, sessionId);
    }

    /**
     * Retrieves the session ID of the current {@code ActivityEvent}.
     * 
     * @return session ID of the {@code ActivityEvent}.
     */
    public String getSessionId() {
        return this.get(SESSION_ID_KEY).toString();
    }

    /**
     * Returns the message associated with the {@code ActivityEvent}
     * 
     * @return The message associated with the {@code ActivityEvent}
     */
    public String getMessage() {
        return this.get(MESSAGE_KEY).toString();
    }

    /**
     * Overwrites the message associated with the {@code ActivityEvent}
     * 
     * @param message
     *            The new message associated with the {@code ActivityEvent}
     */
    public void setMessage(String message) {
        this.put(MESSAGE_KEY, message);
    }

    /**
     * Returns the title associated with the {@code ActivityEvent}
     * 
     * @return The title associated with the {@code ActivityEvent}
     */
    public String getTitle() {
        return this.get(TITLE_KEY).toString();
    }

    /**
     * Overwrites the title associated with the {@code ActivityEvent}
     * 
     * @param title
     *            The new title associated with the {@code ActivityEvent}
     */
    public void setTitle(String title) {
        this.put(TITLE_KEY, title);
    }

    /**
     * Returns the category associated with the {@code ActivityEvent}
     * 
     * @return The category associated with the {@code ActivityEvent}
     */
    public String getCategory() {
        return this.get(CATEGORY_KEY).toString();
    }

    /**
     * Overwrites the category associated with the {@code ActivityEvent}
     * 
     * @param category
     *            A human-readable category that describes the event that is
     *            occuring. Example: 'Product Retrieval'.
     */
    public void setCategory(String category) {
        this.put(CATEGORY_KEY, category);
    }

    /**
     * Returns a {@code String} depicting the time at which the event that
     * triggered this {@code ActivityEvent} occurred.
     *  
     * @return A {@code String} representing the number of milliseconds
     *         between January 1, 1970, 00:00:00 GMT and the point at 
     *         which the event that triggered this {@code ActivityEvent}  
     *         occurred.
     */
    public String getTimestampString() {
        return this.get(TIMESTAMP_KEY).toString();
    }
    
    /**
     * Returns a <code>long</code> depicting the time at which the event that
     * triggered this {@code ActivityEvent} occurred.
     *  
     * @return A <code>long</code> representing the number of milliseconds
     *         between January 1, 1970, 00:00:00 GMT and the point at 
     *         which the event that triggered this {@code ActivityEvent} 
     *         occurred.
     */
    public Long getTimestampLong() {
        return getTimestamp().getTime();
    }
    
    /**
     * Returns a <code>Date</code> depicting the time at which the event that triggered this
     * {@code ActivityEvent} occurred.
     * 
     * @return A <code>Date</code> the point at which the event that triggered this
     *         {@code ActivityEvent} occurred.
     */
    public Date getTimestamp() {
        return ISODateTimeFormat.dateTime().parseDateTime(getTimestampString()).toDate();
    }
    
    /**
     * Overwrites the timestamp that depicts the time at which the event that
     * triggered the {@code ActivityEvent} occurred.
     *  
     * @param timestampString A {@code String} representing the number of 
     *                        milliseconds between January 1, 1970, 00:00:00 GMT
     *                        and the point at which the event that triggered
     *                        this {@code ActivityEvent} occurred.
     */
    private void setTimestamp(String timestampString) {
        this.put(TIMESTAMP_KEY, timestampString);
    }
    
    /**
     * Overwrites the timestamp that depicts the time at which the event that triggered the
     * {@code ActivityEvent} occurred.
     * 
     * @param timestamp
     *            A <code>long</code> representing the point at which the event that triggered this
     *            {@code ActivityEvent} occurred.
     */
    public void setTimestamp(Date timestamp) {
        setTimestamp(ISODateTimeFormat.dateTime().print(timestamp.getTime()));
    }
    
    /**
     * Overwrites the timestamp that depicts the time at which the event that
     * triggered the {@code ActivityEvent} occurred.
     *  
     * @param timestamp A <code>long</code> representing the number of 
     *                  milliseconds between January 1, 1970, 00:00:00 GMT and
     *                  the point at which the event that triggered this 
     *                  {@code ActivityEvent} occurred.
     */
    public void setTimestamp(Long timestamp) {
        this.setTimestamp(new Date(timestamp));
    }

    /**
     * Returns the id of the user to whom this {@code ActivityEvent} is addressed.
     * 
     * @return The id of the user to whom this {@code ActivityEvent} is addressed.
     */
    public String getUserId() {
        return this.get(USER_ID_KEY).toString();
    }

    /**
     * Overwrites the id of the user to whom the {@code ActivityEvent} is addressed.
     * 
     * @param userId
     *            The new userId to whom the {@code ActivityEvent} should be
     *            addressed.
     */
    public void setUserId(String userId) {
        this.put(USER_ID_KEY, userId);
    }

    /**
     * Returns the bytes read associated with the {@code ActivityEvent}.
     *
     * @return The bytes read associated with the {@code ActivityEvent}.
     */
    public Long getBytesRead() {
        try {
            Long bytes = new Long(this.get(BYTES_READ_KEY).toString());
            return bytes;
        } catch (NumberFormatException nfe) {
            LOGGER.debug("Received invalid number of bytes. ", nfe.getMessage());
            return 0L;
        }
    }

    /**
     * Overwrites the bytes read associated with the {@code ActivityEvent}.
     *
     * @param bytesRead
     *            The new bytes read associated with the {@code ActivityEvent}.
     */
    public void setBytesRead(Long bytesRead) {
        this.put(BYTES_READ_KEY, bytesRead);
    }
}
