/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.system.alerts;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SystemNotice is the property map class used by DDF collectors when sending system notifications to
 * the Decanter topic 'decanter/collect/*'. This class defines all the common keys used in system
 * notification events and provides convenient helpers for accessing them.
 */
public class SystemNotice {
    //topics
    public static final String SYSTEM_NOTICE_BASE_TOPIC = "decanter/collect/";

    public static final String SYSTEM_NOTICE_EVENT_TYPE = "system-notice";

    //common decanter fields

    /**
     * Key for a type identifier. Included to be consistent with other decanter events. Will always be system-notice
     */
    public static final String EVENT_TYPE_KEY = "type";

    /**
     * Key for the host name on which this notice originated
     */
    public static final String SYSTEM_NOTICE_HOST_NAME_KEY = "hostName";

    /**
     * Key for the host address on which this notice originated
     */
    public static final String SYSTEM_NOTICE_HOST_ADDRESS_KEY = "hostAddress";

    //custom system notice fields

    /**
     * Key for the notice id. This is an auto generated unique id
     */
    public static final String SYSTEM_NOTICE_ID_KEY = "id";

    /**
     * Key for the date/time of this notice. This is auto populated on creation
     */
    public static final String SYSTEM_NOTICE_TIME_KEY = "noticeTime";

    /**
     * Key for the source of this notice. Usually this is a class name and possibly followed by an additional discriminator (ie method name)
     */
    public static final String SYSTEM_NOTICE_SOURCE_KEY = "source";

    /**
     * Key for the priority of the notice. Acceptable values can be found in NoticePriority
     */
    public static final String SYSTEM_NOTICE_PRIORITY_KEY = "priority";

    /**
     * Key for the title of the notice. Defaults to an empty string
     */
    public static final String SYSTEM_NOTICE_TITLE_KEY = "title";

    /**
     * Key for the Set<String> of details of this notice
     */
    public static final String SYSTEM_NOTICE_DETAILS_KEY = "details";

    protected Map<String, Object> properties = new HashMap<>();

    public SystemNotice() {
        init();
    }

    public SystemNotice(String source, NoticePriority priority, String title, Set<String> details) {
        this();
        setSource(source);
        setPriority(priority);
        setTitle(title);
        setDetails(details == null ? new ArrayList<>() : details);

    }

    public SystemNotice(Map<String, Object> properties) {
        this();
        this.properties.putAll(properties);
    }

    protected void init() {
        properties.put(EVENT_TYPE_KEY, SYSTEM_NOTICE_EVENT_TYPE);
        setTime(new Date());
        setId(UUID.randomUUID()
                .toString());
        setSource("Unknown");
        setPriority(NoticePriority.NORMAL);
        setTitle("");
        setDetails(new HashSet<>());

        try {
            setHostAddress(InetAddress.getLocalHost()
                    .getHostAddress());
            setHostName(InetAddress.getLocalHost()
                    .getHostName());
        } catch (UnknownHostException e) {
            // Should never happen
            throw new IllegalStateException(
                    "Cannot create system notice because the host name could not be retrieved. Reason: "
                            + e.getMessage());
        }
    }

    /**
     * Gets the SystemNotice properties as a map for eventing.
     *
     * @return A copy of the internal properties map.
     */
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }

    public String getId() {
        return (String) properties.get(SYSTEM_NOTICE_ID_KEY);
    }

    public String getSource() {
        return (String) properties.get(SYSTEM_NOTICE_SOURCE_KEY);
    }

    public String getHostName() {
        return (String) properties.get(SYSTEM_NOTICE_HOST_NAME_KEY);
    }

    public String getHostAddress() {
        return (String) properties.get(SYSTEM_NOTICE_HOST_ADDRESS_KEY);
    }

    public NoticePriority getPriority() {
        Object value = properties.get(SYSTEM_NOTICE_PRIORITY_KEY);
        if (value instanceof String) {
            return NoticePriority.valueOf(Integer.parseInt((String) value));
        }
        return NoticePriority.valueOf((int) value);
    }

    public String getTitle() {
        return (String) properties.get(SYSTEM_NOTICE_TITLE_KEY);
    }

    public Set<String> getDetails() {
        Object value = properties.get(SYSTEM_NOTICE_DETAILS_KEY);
        if (value instanceof String) {
            return Collections.singleton((String) value);
        }
        return (Set<String>) value;
    }

    public Date getTime() {
        Object value = properties.get(SYSTEM_NOTICE_TIME_KEY);
        if (value instanceof Long) {
            return new Date((long) value);
        }
        return (Date) value;
    }

    private void setId(String id) {
        safePut(SYSTEM_NOTICE_ID_KEY, id);
    }

    private void setSource(String source) {
        safePut(SYSTEM_NOTICE_SOURCE_KEY, source);
    }

    private void setHostName(String host) {
        safePut(SYSTEM_NOTICE_HOST_NAME_KEY, host);
    }

    private void setHostAddress(String address) {
        safePut(SYSTEM_NOTICE_HOST_ADDRESS_KEY, address);
    }

    private void setPriority(NoticePriority priority) {
        safePut(SYSTEM_NOTICE_PRIORITY_KEY, priority.value());
    }

    private void setTitle(String title) {
        safePut(SYSTEM_NOTICE_TITLE_KEY, title);
    }

    private void setDetails(Collection<String> details) {
        safePut(SYSTEM_NOTICE_DETAILS_KEY, details);
    }

    private void setTime(Date time) {
        safePut(SYSTEM_NOTICE_TIME_KEY, time);
    }

    protected void safePut(String key, Object obj) {
        if (obj != null) {
            properties.put(key, obj);
            return;
        }
        throw new IllegalArgumentException("Null values are not allowed.");
    }
}
