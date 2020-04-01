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
package ddf.catalog.core.versioning.impl;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import ddf.catalog.core.versioning.MetacardVersion;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Experimental. Subject to change. <br>
 * Represents a version at a particular instant. Also included are the {@link Action} that was
 * performed, who it was edited by, and what time it was edited on.
 */
public class MetacardVersionImpl extends MetacardImpl implements MetacardVersion {
  private static final Logger LOGGER = LoggerFactory.getLogger(MetacardVersionImpl.class);

  private static final MetacardType METACARD_VERSION;

  private static final Set<AttributeDescriptor> VERSION_DESCRIPTORS =
      new HashSet<>(MetacardImpl.BASIC_METACARD.getAttributeDescriptors());

  private static final Cache<Integer, byte[]> METACARD_TYPE_CACHE =
      CacheBuilder.newBuilder().maximumSize(256).build();

  static {
    VERSION_DESCRIPTORS.add(
        new AttributeDescriptorImpl(ACTION, true, true, false, false, BasicTypes.STRING_TYPE));
    VERSION_DESCRIPTORS.add(
        new AttributeDescriptorImpl(EDITED_BY, true, true, false, false, BasicTypes.STRING_TYPE));
    VERSION_DESCRIPTORS.add(
        new AttributeDescriptorImpl(VERSIONED_ON, true, true, false, false, BasicTypes.DATE_TYPE));
    VERSION_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            VERSION_OF_ID, true, true, false, false, BasicTypes.STRING_TYPE));
    VERSION_DESCRIPTORS.add(
        new AttributeDescriptorImpl(VERSION_TAGS, true, true, false, true, BasicTypes.STRING_TYPE));
    VERSION_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            VERSION_TYPE, true, true, false, false, BasicTypes.STRING_TYPE));
    VERSION_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            VERSION_TYPE_BINARY, false, true, false, false, BasicTypes.BINARY_TYPE));
    VERSION_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            VERSIONED_RESOURCE_URI, true, true, true, false, BasicTypes.STRING_TYPE));
    METACARD_VERSION = new MetacardTypeImpl(PREFIX, VERSION_DESCRIPTORS);
  }

  /**
   * Will convert the given {@link Metacard} to a {@link MetacardVersionImpl} by cloning it and
   * adding the current subject, time, and a random UUID. Cannot take a {@link MetacardVersionImpl}
   * as the sourceMetacard.
   *
   * @param sourceMetacard Metacard to clone and create a history item from
   * @param action Which action was done to modify the metacard
   * @throws IllegalArgumentException
   */
  public MetacardVersionImpl(String id, Metacard sourceMetacard, Action action, Subject subject) {
    this(
        id,
        sourceMetacard,
        action,
        subject,
        Collections.singletonList(MetacardImpl.BASIC_METACARD));
  }

  /**
   * Will convert the given {@link Metacard} to a {@link MetacardVersionImpl} by cloning it and
   * adding the current subject, time, and a random UUID. Cannot take a {@link MetacardVersionImpl}
   * as the sourceMetacard. <br>
   * The {@link MetacardType}s list will affect whether the metacard type is stored by name or
   * entirely by serializing the type and storing it in the metacard.
   *
   * @param sourceMetacard Metacard to clone and create a history item from
   * @param action Which action was done to modify the metacard
   * @param types A list of currently defined types in the system
   * @throws IllegalArgumentException
   */
  public MetacardVersionImpl(
      String id,
      Metacard sourceMetacard,
      Action action,
      Subject subject,
      List<MetacardType> types) {
    super(
        sourceMetacard,
        new MetacardTypeImpl(
            PREFIX, METACARD_VERSION, sourceMetacard.getMetacardType().getAttributeDescriptors()));

    this.setAction(action);
    this.setVersionOfId(sourceMetacard.getId());
    this.setVersionTags(sourceMetacard.getTags());

    this.setVersionType(sourceMetacard.getMetacardType().getName());
    this.setVersionTypeBinary(getVersionType(sourceMetacard));

    String editedBy = SubjectUtils.getEmailAddress(subject);
    if (isNullOrEmpty(editedBy)) {
      editedBy = SubjectUtils.getName(subject);
    }
    this.setEditedBy(editedBy);

    this.setVersionedOn(Date.from(Instant.now()));
    this.setId(id);
    this.setVersionResourceUri(sourceMetacard.getResourceURI());
    this.setTags(Collections.singleton(VERSION_TAG));
  }

  // @formatter:off
  private byte[] getVersionType(Metacard sourceMetacard) {
    try {
      return METACARD_TYPE_CACHE.get(
          sourceMetacard.getMetacardType().hashCode(),
          () -> {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
              oos.writeObject(sourceMetacard.getMetacardType());
              return baos.toByteArray();
            }
          });
    } catch (ExecutionException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Could not serialize metacard type for metacard {}", sourceMetacard, e);
      }
      throw new RuntimeException("Could not serialize MetacardType", e);
    }
  }
  // @formatter:on

  /**
   * Reverts the {@link MetacardVersionImpl} to the original {@link Metacard}
   *
   * @return the original metacard this version represents
   * @deprecated The metacard types passed in are no longer used
   */
  @Deprecated
  public Metacard getMetacard(List<MetacardType> types) {
    return this.getMetacard();
  }

  /**
   * Reverts the {@link MetacardVersionImpl} to the original {@link Metacard}
   *
   * @return The original metacard this version represents
   */
  public Metacard getMetacard() {
    return toMetacard(this);
  }

  /**
   * Reverts the passed in {@link Metacard} to the original {@link Metacard} IF it is a version
   * metacard.
   *
   * @return The original metacard this version represents
   * @deprecated The metacard types passed in are no longer used
   */
  @Deprecated
  public static Metacard toMetacard(Metacard source, List<MetacardType> types) {
    return toMetacard(source);
  }

  /**
   * Reverts the passed in {@link Metacard} to the original {@link Metacard} IF it is a version
   * metacard.
   *
   * @param source The metacard to revert
   * @return The original metacard this version represents
   */
  public static Metacard toMetacard(Metacard source) {
    String id = (String) source.getAttribute(MetacardVersion.VERSION_OF_ID).getValue();
    if (isNullOrEmpty(id)) {
      throw new IllegalStateException(
          "Cannot convert history metacard without the original metacard id");
    }

    MetacardImpl result =
        new MetacardImpl(
            source, getMetacardTypeBinary(source).orElseThrow(cannotDeserializeException));
    result.setId(id);
    result.setTags(getVersionTags(source));
    if (source.getAttribute(VERSIONED_RESOURCE_URI) != null) {
      try {
        result.setResourceURI(
            new URI(String.valueOf(source.getAttribute(VERSIONED_RESOURCE_URI).getValue())));
      } catch (URISyntaxException e) {
        LOGGER.debug("Could not replace the versioned resource URI, It might not be valid", e);
      }
    }

    sanitizeVersionAttributes(result);
    return result;
  }

  private static void sanitizeVersionAttributes(/*Mutable*/ Metacard source) {
    Consumer<String> nullifySourceAttribute =
        (s) -> source.setAttribute(new AttributeImpl(s, (Serializable) null));
    Sets.difference(VERSION_DESCRIPTORS, MetacardImpl.BASIC_METACARD.getAttributeDescriptors())
        .stream()
        .map(AttributeDescriptor::getName)
        .forEach(nullifySourceAttribute);
  }

  private static Supplier<RuntimeException> cannotDeserializeException =
      () -> new RuntimeException("Could not Deserialize MetacardType");

  private static Optional<MetacardType> getMetacardTypeBinary(Metacard source) {
    byte[] typeBytes =
        Optional.of(source)
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
      LOGGER.debug("Error while processing metacard type", e);
      return Optional.empty();
    }
  }

  private static Set<String> getVersionTags(Metacard source) {
    Attribute attribute = source.getAttribute(VERSION_TAGS);
    if (attribute == null || attribute.getValue() == null) {
      return new HashSet<>();
    } else {
      return new HashSet<>(
          attribute.getValues().stream().map(String::valueOf).collect(Collectors.toList()));
    }
  }

  protected static MetacardVersion toVersion(Metacard metacard) {
    if (!(metacard instanceof MetacardVersion)) {
      throw new IllegalArgumentException("Metacard must be a instanceof MetacardVersion");
    }
    return (MetacardVersion) metacard;
  }

  public static boolean isNotVersion(@Nullable Metacard metacard) {
    return !isVersion(metacard);
  }

  public static boolean isVersion(@Nullable Metacard metacard) {

    return metacard instanceof MetacardVersion
        || getMetacardVersionType()
            .getName()
            .equals(
                Optional.ofNullable(metacard)
                    .map(Metacard::getMetacardType)
                    .map(MetacardType::getName)
                    .orElse(null));
  }

  public URI getVersionResourceUri() {
    return requestData(VERSIONED_RESOURCE_URI, URI.class);
  }

  public void setVersionResourceUri(String uri) {
    setAttribute(VERSIONED_RESOURCE_URI, uri);
  }

  public void setVersionResourceUri(URI uri) {
    setVersionResourceUri(uri == null ? "" : uri.toString());
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
      return new HashSet<>(
          attribute.getValues().stream().map(String::valueOf).collect(Collectors.toList()));
    }
  }

  public void setVersionTags(Set<String> tags) {
    setAttribute(VERSION_TAGS, new ArrayList<>(tags));
  }

  public static MetacardType getMetacardVersionType() {
    return METACARD_VERSION;
  }
}
