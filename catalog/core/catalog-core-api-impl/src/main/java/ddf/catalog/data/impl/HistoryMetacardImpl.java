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
package ddf.catalog.data.impl;

import static com.google.api.client.repackaged.com.google.common.base.Strings.isNullOrEmpty;

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

import com.google.api.client.repackaged.com.google.common.base.Strings;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.security.SubjectUtils;

public class HistoryMetacardImpl extends MetacardImpl {

    public enum Action {
        CREATED("Created"),
        UPDATED("Updated"),
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
    }

    private static final String HISTORY_PREFIX = "metacard.history";

    private static Function<String, String> prefix = s -> String.format("%s.%s", HISTORY_PREFIX, s);

    /**
     * {@link ddf.catalog.data.Attribute} value for {@link ddf.catalog.data.Metacard#TAGS} when
     * a metacard is a History Metacard.
     */
    public static final String HISTORY_TAG = "history";

    /**
     * {@link ddf.catalog.data.Attribute} name for state of the current {@link Metacard}.
     * Can be one of <code>Active</code>, <code>Inactive</code>, or <code>Deleted</code>.
     * Default value is <code>Inactive</code>.
     *
     * @since DDF-2.9.0
     */
    public static final String STATE = prefix.apply("state");

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
    public static final String VERSIONED = prefix.apply("versioned");

    /**
     * {@link ddf.catalog.data.Attribute} name for metacard ID on a history item of this {@link Metacard} revision.
     *
     * @since DDF-2.9.0
     */
    public static final String ID_HISTORY = prefix.apply("history-id");

    public static final String TAGS_HISTORY = prefix.apply("history-tags");

    private static MetacardType versionHistoryMetacard;

    public static final String METACARD_TYPE_NAME = "ddf.history.metacard";

    static {
        HashSet<AttributeDescriptor> historyDescriptors =
                new HashSet<>(BasicTypes.BASIC_METACARD.getAttributeDescriptors());
        historyDescriptors.add(new AttributeDescriptorImpl(STATE,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        historyDescriptors.add(new AttributeDescriptorImpl(EDITED_BY,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        historyDescriptors.add(new AttributeDescriptorImpl(VERSIONED,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.DATE_TYPE));
        historyDescriptors.add(new AttributeDescriptorImpl(ID_HISTORY,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        historyDescriptors.add(new AttributeDescriptorImpl(TAGS_HISTORY,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                true /* multivalued */,
                BasicTypes.STRING_TYPE));

        versionHistoryMetacard = new MetacardTypeImpl(METACARD_TYPE_NAME, historyDescriptors);
    }

    /**
     * Will convert the given {@link Metacard} to a {@link HistoryMetacardImpl} by cloning
     * it and adding the current subject, time, and a random UUID. Cannot take a
     * {@link HistoryMetacardImpl} as the sourceMetacard.
     *
     * @param sourceMetacard Metacard to clone and create a history item from
     * @param action         Which action was done to modify the metacard
     * @throws IllegalArgumentException
     */
    public HistoryMetacardImpl(Metacard sourceMetacard, Action action, Subject subject) {
        super(sourceMetacard, versionHistoryMetacard);
        if (sourceMetacard instanceof HistoryMetacardImpl) {
            throw new IllegalArgumentException(
                    "Cannot create a history item from a history metacard.");
        }

        this.setState(action);
        this.setIdHistory(sourceMetacard.getId());
        this.setTagsHistory(sourceMetacard.getTags());

        String editedBy = SubjectUtils.getEmailAddress(subject);
        if (isNullOrEmpty(editedBy)) {
            editedBy = SubjectUtils.getName(subject);
        }
        this.setEditedBy(editedBy);

        this.setVersioned(Date.from(Instant.now()));
        this.setId(UUID.randomUUID()
                .toString()
                .replace("-", ""));
        this.setTags(Collections.singleton(HISTORY_TAG));
    }

    /**
     * Returns a {@link BasicTypes#BASIC_METACARD} version of the given
     * {@link HistoryMetacardImpl}
     *
     * @return The converted metacard
     * @throws IllegalStateException
     */
    public Metacard toBasicMetacard() {
        String id = this.getIdHistory();
        if (isNullOrEmpty(id)) {
            throw new IllegalStateException(
                    "Cannot convert history metacard without the original metacard id");
        }

        MetacardImpl result = new MetacardImpl(this, BasicTypes.BASIC_METACARD);
        result.setId(id);
        result.setTags(this.getTagsHistory());
        return result;
    }

    public String getIdHistory() {
        return requestString(ID_HISTORY);
    }

    public void setIdHistory(String idHistory) {
        setAttribute(ID_HISTORY, idHistory);
    }

    public Date getVersioned() {
        return requestDate(VERSIONED);
    }

    public void setVersioned(Date date) {
        setAttribute(VERSIONED, date);
    }

    public String getEditedBy() {
        return requestString(EDITED_BY);
    }

    public void setEditedBy(String editedBy) {
        setAttribute(EDITED_BY, editedBy);
    }

    public Action getState() {
        return Action.fromKey(requestString(STATE));
    }

    public void setState(Action state) {
        setAttribute(STATE, state.getKey());
    }

    public Set<String> getTagsHistory() {
        Attribute attribute = getAttribute(TAGS_HISTORY);
        if (attribute == null || attribute.getValue() == null) {
            return new HashSet<>();
        } else {
            return new HashSet<>(attribute.getValues()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList()));
        }
    }

    public void setTagsHistory(Set<String> tags) {
        setAttribute(TAGS_HISTORY, new ArrayList<>(tags));
    }

    public static MetacardType getVersionHistoryMetacardType() {
        return versionHistoryMetacard;
    }
}
