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

import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.SecurityAttributes;

/**
 * Default implementation of the {@link MetacardType}, used by {@link BasicTypes} to create the
 * {@link BasicTypes#BASIC_METACARD}.
 * <p>
 * This class is {@link java.io.Serializable} and care should be taken with compatibility if changes are
 * made.
 * </p>
 * <p>
 * For what constitutes a compatible change in serialization, see <a href=
 * "http://docs.oracle.com/javase/6/docs/platform/serialization/spec/version.html#6678" >Sun's
 * Guidelines</a>.
 * </p>
 */
public class MetacardTypeImpl implements MetacardType {

    private static final long serialVersionUID = 1L;

    //@formatter:off
    private static final Collector<AttributeDescriptor, ?, Map<String, AttributeDescriptor>>
            ATTRIBUTE_COLLECTOR = Collectors.toMap(
                    AttributeDescriptor::getName,
                    Function.identity(),
                    (oldVal, newVal) -> oldVal);
    //@formatter:on

    /**
     * Set of {@link AttributeDescriptor}s
     */
    protected transient Map<String, AttributeDescriptor> descriptors = Collections.emptyMap();

    /**
     * The name of this {@code MetacardTypeImpl}
     *
     * @serial
     */
    protected String name;

    /**
     * Creates a {@code MetacardTypeImpl} with the provided {@code name} and
     * {@link AttributeDescriptor}s.
     *
     * @param name        the name of this {@code MetacardTypeImpl}
     * @param descriptors the set of descriptors for this {@code MetacardTypeImpl}
     */
    public MetacardTypeImpl(String name, Set<AttributeDescriptor> descriptors) {
        /*
         * If any defensive logic is added to this constructor, then that logic should be reflected
         * in the deserialization (readObject()) of this object so that the integrity of a
         * serialized object is maintained. For instance, if a null check is added in the
         * constructor, the same check should be added in the readObject() method.
         */
        this.name = name;
        this.descriptors = Optional.ofNullable(descriptors)
                .orElseGet(Collections::emptySet)
                .stream()
                .collect(ATTRIBUTE_COLLECTOR);
    }

    /**
     * Creates a {@code MetacardTypeImpl} with the provided name, {@link MetacardType}, and set of
     * additional {@linkplain AttributeDescriptor AttributeDescriptors}.
     * <p>
     * {@code additionalDescriptors} and the descriptors in {@code metacardType} will be combined to
     * form the set of descriptors for this {@code MetacardTypeImpl}.
     * <p>
     * Essentially, this is a convenience constructor for creating a new {@code MetacardTypeImpl}
     * that extends an existing {@link MetacardType}.
     *
     * @param name                  the name of this {@code MetacardTypeImpl}
     * @param metacardType          the base {@link MetacardType}, cannot be null
     * @param additionalDescriptors the descriptors to add to this {@code MetacardTypeImpl} in
     *                              addition to the descriptors in {@code metacardType}, cannot be
     *                              null or empty
     * @throws IllegalArgumentException if {@code metacardType} or {@code additionalDescriptors} is
     *                                  null, or if {@code additionalDescriptors} is empty
     */
    public MetacardTypeImpl(String name, MetacardType metacardType,
            Set<AttributeDescriptor> additionalDescriptors) {
        notNull(metacardType, "The metacard type cannot be null.");
        notEmpty(additionalDescriptors,
                "The set of additional descriptors cannot be null or empty");

        this.name = name;
        this.descriptors = Stream.of(metacardType.getAttributeDescriptors(), additionalDescriptors)
                .flatMap(Set::stream)
                .collect(ATTRIBUTE_COLLECTOR);
    }

    /**
     * Creates a {@code MetacardTypeImpl} with the provided {@code name} and
     * {@link MetacardType}s.
     *
     * @param name          the name of this {@code MetacardTypeImpl}
     * @param metacardTypes the set of {@link MetacardType}s to compose this {@code MetacardTypeImpl}
     */
    public MetacardTypeImpl(String name, List<MetacardType> metacardTypes) {
        notEmpty(metacardTypes, "The set of Metacard Types cannot be null or empty");

        this.name = name;

        this.descriptors = Stream.concat(Stream.of(new CoreAttributes(), new SecurityAttributes()),
                metacardTypes.stream())
                .filter(Objects::nonNull)
                .map(MetacardType::getAttributeDescriptors)
                .flatMap(Set::stream)
                .collect(ATTRIBUTE_COLLECTOR);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<AttributeDescriptor> getAttributeDescriptors() {
        return Collections.unmodifiableSet(getDescriptors());
    }

    @Override
    public AttributeDescriptor getAttributeDescriptor(String attributeName) {
        if (attributeName == null) {
            return null;
        }
        return descriptors.get(attributeName);
    }

    /**
     * Serializes this {@link MetacardTypeImpl} instance.
     *
     * @param stream the {@link ObjectOutputStream} that contains the object to be serialized
     * @throws IOException
     * @serialData First, the name is written as a {@code String} by the default Java serialization
     * implementation, then the number of {@link AttributeDescriptor} objects is written
     * as an ( {@code int}), followed by all of the {@code AttributeDescriptor} objects
     * in no guaranteed sequence or order.
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {

        /*
         * defaultWriteObject() is invoked for greater flexibility and compatibility. See the
         * *Serialization Note* in MetacardImpl's class Javadoc.
         */
        stream.defaultWriteObject();

        stream.writeInt(getDescriptors().size());

        for (AttributeDescriptor descriptor : getDescriptors()) {
            stream.writeObject(descriptor);
        }

    }

    /**
     * Deserializes this {@link MetacardTypeImpl} instance.
     *
     * @param stream the {@link ObjectInputStream} that contains the bytes of the object
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {

        /*
         * defaultReadObject() is invoked for greater flexibility and compatibility. See the
         * *Serialization Note* in MetacardImpl's class Javadoc.
         */
        stream.defaultReadObject();

        int numElements = stream.readInt();

        this.descriptors = IntStream.range(0, numElements)
                .boxed()
                .map(objectGetter(stream))
                .collect(ATTRIBUTE_COLLECTOR);
    }

    private Function<Integer, AttributeDescriptor> objectGetter(ObjectInputStream stream) {
        return (i) -> {
            try {
                return (AttributeDescriptor) stream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        };
    }

    /*
     * Only to be used by the getDescriptors method.
     */
    private Set<AttributeDescriptor> cachedDescriptors;

    private Set<AttributeDescriptor> getDescriptors() {
        if (cachedDescriptors == null) {
            cachedDescriptors = new HashSet<>(descriptors.values());
        }
        return cachedDescriptors;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(19, 71).append(name)
                .append(getDescriptors())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof MetacardType)) {
            return false;
        }

        MetacardType other = (MetacardType) obj;
        return new EqualsBuilder().append(name, other.getName())
                .append(getDescriptors(), other.getAttributeDescriptors())
                .isEquals();
    }
}
