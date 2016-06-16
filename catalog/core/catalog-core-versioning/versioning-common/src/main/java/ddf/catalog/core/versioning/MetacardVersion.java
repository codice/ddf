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
package ddf.catalog.core.versioning;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.shiro.subject.Subject;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.security.SubjectUtils;

public class MetacardVersion extends MetacardImpl {

    public enum Action {
        CREATED("Created"),
        CREATED_CONTENT("Created-Content"),
        UPDATED("Updated"),
        UPDATED_CONTENT("Updated-Content"),
        DELETED("Deleted");

        private static Map<String, Action> keyMap = new HashMap<>();

        static {
            for (Action action : Action.values()) {
                keyMap.put(action.getKey(), action);
            }
        }

        private String key;

        Action(String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }

        public static Action fromKey(String key) {
            return keyMap.get(key);
        }

        public static Action ofMetacard(Metacard metacard) {
            if (isNotVersion(metacard)) {
                throw new IllegalArgumentException(
                        "Cannot get action of a non version metacard [" + metacard.getId() + "]");
            }
            Serializable svalue = metacard.getAttribute(ACTION).getValue();
            if (!(svalue instanceof String)) {
                throw new IllegalArgumentException("The action attribute must be a string");
            }
            String value = (String) svalue;
            return keyMap.get(value);
        }
    }

    public static final String ALREADY_VERSIONED = "already-versioned";

    public static final String HISTORY_METACARDS_PROPERTY = "history-metacards";

    private static final String VERSION_PREFIX = "metacard.version";

    private static Function<String, String> prefix = s -> String.format("%s.%s", VERSION_PREFIX, s);

    /**
     * {@link ddf.catalog.data.Attribute} value for {@link ddf.catalog.data.Metacard#TAGS} when
     * a metacard is a History Metacard.
     */
    public static final String VERSION_TAG = "revision";

    /**
     * {@link ddf.catalog.data.Attribute} name for action of the current {@link MetacardVersion}.
     * Can be one of <code>Created</code>, <code>Updated</code>, or <code>Deleted</code>.
     *
     * @since DDF-2.9.0
     */
    public static final String ACTION = prefix.apply("action");

    /**
     * {@link ddf.catalog.data.Attribute} name for the editor of this {@link Metacard} revision.
     *
     * @since DDF-2.9.0
     */
    public static final String EDITED_BY = prefix.apply("edited-by");

    /**
     * {@link ddf.catalog.data.Attribute} name for version date of this {@link Metacard} revision.
     *
     * @since DDF-2.9.0
     */
    public static final String VERSIONED_ON = prefix.apply("versioned-on");

    /**
     * {@link ddf.catalog.data.Attribute} name for metacard ID on a history item of this {@link Metacard} revision.
     *
     * @since DDF-2.9.0
     */
    public static final String VERSION_OF_ID = prefix.apply("id");

    public static final String VERSION_TAGS = prefix.apply("tags");

    private static MetacardType metacardVersion;

    static {
        HashSet<AttributeDescriptor> versionDescriptors =
                new HashSet<>(BasicTypes.BASIC_METACARD.getAttributeDescriptors());
        versionDescriptors.add(new AttributeDescriptorImpl(ACTION,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        versionDescriptors.add(new AttributeDescriptorImpl(EDITED_BY,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        versionDescriptors.add(new AttributeDescriptorImpl(VERSIONED_ON,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.DATE_TYPE));
        versionDescriptors.add(new AttributeDescriptorImpl(VERSION_OF_ID,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        versionDescriptors.add(new AttributeDescriptorImpl(VERSION_TAGS,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                true /* multivalued */,
                BasicTypes.STRING_TYPE));

        metacardVersion = new MetacardTypeImpl(VERSION_PREFIX, versionDescriptors);
    }

    /**
     * Will convert the given {@link Metacard} to a {@link MetacardVersion} by cloning
     * it and adding the current subject, time, and a random UUID. Cannot take a
     * {@link MetacardVersion} as the sourceMetacard.
     *
     * @param sourceMetacard Metacard to clone and create a history item from
     * @param action         Which action was done to modify the metacard
     * @throws IllegalArgumentException
     */
    public MetacardVersion(Metacard sourceMetacard, Action action, Subject subject) {
        super(sourceMetacard, metacardVersion);
        if (sourceMetacard instanceof MetacardVersion) {
            throw new IllegalArgumentException(
                    "Cannot create a history item from a history metacard.");
        }

        this.setAction(action);
        this.setVersionOfId(sourceMetacard.getId());
        this.setVersionTags(sourceMetacard.getTags());

        String editedBy = SubjectUtils.getEmailAddress(subject);
        if (isNullOrEmpty(editedBy)) {
            editedBy = SubjectUtils.getName(subject);
        }
        this.setEditedBy(editedBy);

        this.setVersionedOn(Date.from(Instant.now()));
        this.setId(UUID.randomUUID()
                .toString()
                .replace("-", ""));
        this.setTags(Collections.singleton(VERSION_TAG));
    }

    /**
     * Returns a {@link BasicTypes#BASIC_METACARD} version of the given
     * {@link MetacardVersion}
     *
     * @return The converted metacard
     * @throws IllegalStateException
     */
    public Metacard toBasicMetacard() {
        return toBasicMetacard(this);
    }

    public static Metacard toBasicMetacard(Metacard source) {
        String id = (String) source.getAttribute(MetacardVersion.VERSION_OF_ID)
                .getValue();
        if (isNullOrEmpty(id)) {
            throw new IllegalStateException(
                    "Cannot convert history metacard without the original metacard id");
        }

        MetacardImpl result = new MetacardImpl(source, BasicTypes.BASIC_METACARD);
        result.setId(id);
        result.setTags(getVersionTags(source));
        return result;
    }

    private static Set<String> getVersionTags(Metacard source) {
        Attribute attribute = source.getAttribute(VERSION_TAGS);
        if (attribute == null || attribute.getValue() == null) {
            return new HashSet<>();
        } else {
            return new HashSet<>(attribute.getValues()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList()));
        }
    }

    protected static MetacardVersion toVersion(Metacard metacard) {
        if (!(metacard instanceof MetacardVersion)) {
            throw new IllegalArgumentException("Metacard must be a instanceof MetacardVersion");
        }
        return (MetacardVersion) metacard;
    }

    public static boolean isNotVersion(Metacard metacard) {
        return !isVersion(metacard);
    }

    public static boolean isVersion(Metacard metacard) {
        return metacard instanceof MetacardVersion || getMetacardVersionType().getName()
                .equals(metacard.getMetacardType()
                        .getName());
    }

    public String getVersionOfId() {
        return requestString(VERSION_OF_ID);
    }

    public void setVersionOfId(String idHistory) {
        setAttribute(VERSION_OF_ID, idHistory);
    }

    public Date getVersionedOn() {
        return requestDate(VERSIONED_ON);
    }

    public void setVersionedOn(Date date) {
        setAttribute(VERSIONED_ON, date);
    }

    public String getEditedBy() {
        return requestString(EDITED_BY);
    }

    public void setEditedBy(String editedBy) {
        setAttribute(EDITED_BY, editedBy);
    }

    public Action getAction() {
        return Action.fromKey(requestString(ACTION));
    }

    public void setAction(Action action) {
        setAttribute(ACTION, action.getKey());
    }

    public Set<String> getVersionTags() {
        Attribute attribute = getAttribute(VERSION_TAGS);
        if (attribute == null || attribute.getValue() == null) {
            return new HashSet<>();
        } else {
            return new HashSet<>(attribute.getValues()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList()));
        }
    }

    public void setVersionTags(Set<String> tags) {
        setAttribute(VERSION_TAGS, new ArrayList<>(tags));
    }

    public static MetacardType getMetacardVersionType() {
        return metacardVersion;
    }
}

