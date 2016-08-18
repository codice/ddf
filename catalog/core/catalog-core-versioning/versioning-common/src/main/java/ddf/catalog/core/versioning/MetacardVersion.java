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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.security.SubjectUtils;

/**
 * Represents a version at a particular instant. Also included are the {@link Action} that was
 * performed, who it was edited by, and what time it was edited on.
 */
public class MetacardVersion extends MetacardImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardVersion.class);

    private static final Set<AttributeDescriptor> VERSION_DESCRIPTORS =
            new HashSet<>(BasicTypes.BASIC_METACARD.getAttributeDescriptors());

    private static final String VERSION_PREFIX = "metacard.version";

    private static final Function<String, String> PREFIX = s -> String.format("%s.%s",
            VERSION_PREFIX,
            s);

    private static final MetacardType METACARD_VERSION;

    ////////////////////////////////////////////////////////////////////////////////////
    // OPERATION PROPERTIES
    ////////////////////////////////////////////////////////////////////////////////////

    public static final String SKIP_VERSIONING = "skip-versioning";

    public static final String HISTORY_METACARDS_PROPERTY = "history-metacards";

    ////////////////////////////////////////////////////////////////////////////////////
    // ATTRIBUTE VALUES
    ////////////////////////////////////////////////////////////////////////////////////

    /**
     * {@link ddf.catalog.data.Attribute} value for {@link ddf.catalog.data.Metacard#TAGS} when
     * a metacard is a History Metacard.
     */
    public static final String VERSION_TAG = "revision";

    ////////////////////////////////////////////////////////////////////////////////////
    // ATTRIBUTE NAMES
    ////////////////////////////////////////////////////////////////////////////////////

    /**
     * {@link ddf.catalog.data.Attribute} name for action of the current {@link MetacardVersion}.
     * Can be one of <code>Created</code>, <code>Updated</code>, or <code>Deleted</code>.
     *
     * @since DDF-2.9.0
     */
    public static final String ACTION = PREFIX.apply("action");

    /**
     * {@link ddf.catalog.data.Attribute} name for the editor of this {@link Metacard} revision.
     *
     * @since DDF-2.9.0
     */
    public static final String EDITED_BY = PREFIX.apply("edited-by");

    /**
     * {@link ddf.catalog.data.Attribute} name for version date of this {@link Metacard} revision.
     *
     * @since DDF-2.9.0
     */
    public static final String VERSIONED_ON = PREFIX.apply("versioned-on");

    /**
     * {@link ddf.catalog.data.Attribute} name for metacard ID on a history item of this {@link Metacard} revision.
     *
     * @since DDF-2.9.0
     */
    public static final String VERSION_OF_ID = PREFIX.apply("id");

    /**
     * {@link ddf.catalog.data.Attribute} name for original tags of this {@link Metacard} revision.
     *
     * @since DDF-2.9.0
     */
    public static final String VERSION_TAGS = PREFIX.apply("tags");

    /**
     * {@link ddf.catalog.data.Attribute} name for original metacard type of this {@link Metacard} revision.
     *
     * @since DDF-2.9.0
     */
    public static final String VERSION_TYPE = PREFIX.apply("type");

    /**
     * {@link ddf.catalog.data.Attribute} name for original serialized metacard type  of this {@link Metacard} revision.
     *
     * @since DDF-2.9.0
     */
    public static final String VERSION_TYPE_BINARY = PREFIX.apply("type-binary");

    static {
        VERSION_DESCRIPTORS.add(new AttributeDescriptorImpl(ACTION,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        VERSION_DESCRIPTORS.add(new AttributeDescriptorImpl(EDITED_BY,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        VERSION_DESCRIPTORS.add(new AttributeDescriptorImpl(VERSIONED_ON,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.DATE_TYPE));
        VERSION_DESCRIPTORS.add(new AttributeDescriptorImpl(VERSION_OF_ID,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        VERSION_DESCRIPTORS.add(new AttributeDescriptorImpl(VERSION_TAGS,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                true /* multivalued */,
                BasicTypes.STRING_TYPE));
        VERSION_DESCRIPTORS.add(new AttributeDescriptorImpl(VERSION_TYPE,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        VERSION_DESCRIPTORS.add(new AttributeDescriptorImpl(VERSION_TYPE_BINARY,
                false /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.BINARY_TYPE));
        METACARD_VERSION = new MetacardTypeImpl(VERSION_PREFIX, VERSION_DESCRIPTORS);
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // ENUMERATIONS
    ////////////////////////////////////////////////////////////////////////////////////
    public enum Action {
        // @formatter:off
        CREATED("Created"),
        CREATED_CONTENT("Created-Content"),
        UPDATED("Updated"),
        UPDATED_CONTENT("Updated-Content"),
        DELETED("Deleted");
        // @formatter:on

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
            Serializable svalue = Optional.ofNullable(metacard.getAttribute(ACTION))
                    .map(Attribute::getValue)
                    .orElse(null);
            if (!(svalue instanceof String)) {
                throw new IllegalArgumentException("The action attribute must be a string");
            }
            String value = (String) svalue;
            return keyMap.get(value);
        }

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
        this(sourceMetacard, action, subject, Collections.singletonList(BasicTypes.BASIC_METACARD));
    }

    public MetacardVersion(Metacard sourceMetacard, Action action, Subject subject,
            List<MetacardType> types) {
        super(sourceMetacard,
                new MetacardTypeImpl(VERSION_PREFIX,
                        METACARD_VERSION,
                        sourceMetacard.getMetacardType()
                                .getAttributeDescriptors()));

        if (sourceMetacard instanceof MetacardVersion) {
            throw new IllegalArgumentException(
                    "Cannot create a history item from a history metacard.");
        }

        this.setAction(action);
        this.setVersionOfId(sourceMetacard.getId());
        this.setVersionTags(sourceMetacard.getTags());

        Optional<MetacardType> type = types.stream()
                .filter(mt -> sourceMetacard.getMetacardType()
                        .getName()
                        .equals(mt.getName()))
                .findFirst();

        this.setVersionType(sourceMetacard.getMetacardType()
                .getName());
        if (!type.isPresent()) {
            this.setVersionTypeBinary(getVersionType(sourceMetacard));
        }

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

    private byte[] getVersionType(Metacard sourceMetacard) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(sourceMetacard.getMetacardType());
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Could not serialize MetacardType", e);
        }
    }

    /**
     * Reverts the {@link MetacardVersion} to the original {@link Metacard}
     *
     * @return The converted metacard
     * @throws IllegalStateException
     */
    public Metacard getMetacard(List<MetacardType> types) {
        return toMetacard(this, types);
    }

    public static Metacard toMetacard(Metacard source, List<MetacardType> types) {
        String id = (String) source.getAttribute(MetacardVersion.VERSION_OF_ID)
                .getValue();
        if (isNullOrEmpty(id)) {
            throw new IllegalStateException(
                    "Cannot convert history metacard without the original metacard id");
        }

        String typeString = (String) source.getAttribute(VERSION_TYPE)
                .getValue();
        Optional<MetacardType> typeFromExisting = types.stream()
                .filter(mt -> mt.getName()
                        .equals(typeString))
                .findFirst();

        MetacardImpl result = new MetacardImpl(source,
                typeFromExisting.orElseGet(() -> getMetacardTypeBinary(source).orElseThrow(
                        cannotDeserializeException)));
        result.setId(id);
        result.setTags(getVersionTags(source));

        sanitizeVersionAttributes(result);
        return result;
    }

    private static void sanitizeVersionAttributes(/*Mutable*/ Metacard source) {
        Consumer<String> nullifySourceAttribute = (s) -> source.setAttribute(new AttributeImpl(s,
                (Serializable) null));
        Sets.difference(VERSION_DESCRIPTORS, BasicTypes.BASIC_METACARD.getAttributeDescriptors())
                .stream()
                .map(AttributeDescriptor::getName)
                .forEach(nullifySourceAttribute);
    }

    private static Supplier<RuntimeException> cannotDeserializeException =
            () -> new RuntimeException("Could not Deserialize MetacardType");

    private static Optional<MetacardType> getMetacardTypeBinary(Metacard source) {
        byte[] typeBytes = Optional.of(source)
                .map(m -> m.getAttribute(MetacardVersion.VERSION_TYPE_BINARY))
                .map(Attribute::getValue)
                .filter(byte[].class::isInstance)
                .map(byte[].class::cast)
                .orElseThrow(cannotDeserializeException);

        try (ByteArrayInputStream bais = new ByteArrayInputStream(typeBytes);
                ObjectInputStream ois = new ObjectInputStream(bais)) {
            return Optional.ofNullable(ois.readObject())
                    .filter(MetacardType.class::isInstance)
                    .map(MetacardType.class::cast);
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error("Error while processing metacard type", e);
            return Optional.empty();
        }
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

    public byte[] getVersionType() {
        return requestBytes(VERSION_TYPE);
    }

    public void setVersionType(String versionType) {
        setAttribute(VERSION_TYPE, versionType);
    }

    public String getVersionTypeBinary() {
        return requestString(VERSION_TYPE_BINARY);
    }

    public void setVersionTypeBinary(byte[] versionType) {
        setAttribute(VERSION_TYPE_BINARY, versionType);
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
        return METACARD_VERSION;
    }
}

