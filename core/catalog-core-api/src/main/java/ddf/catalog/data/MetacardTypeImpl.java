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
package ddf.catalog.data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Default implementation of the {@link MetacardType}, used by
 * {@link BasicTypes} to create the {@link BasicTypes#BASIC_METACARD}.
 * 
 * <p>
 * This class is {@link Serializable} and care should be taken with
 * compatibility if changes are made.
 * </p>
 * 
 * <p>
 * For what constitutes a compatible change in serialization, see <a href=
 * "http://docs.oracle.com/javase/6/docs/platform/serialization/spec/version.html#6678"
 * >Sun's Guidelines</a>.
 * </p>
 * 
 * @deprecated As of release 2.3.0, replaced by
 *             ddf.catalog.data.impl.MetacardTypeImpl
 * 
 */
@Deprecated
public class MetacardTypeImpl implements MetacardType {

    private static final long serialVersionUID = 1L;

    /**
     * Set of {@link AttributeDescriptor}s
     */
    protected transient Set<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();

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
     * @param name
     *            the name of this {@code MetacardTypeImpl}
     * @param descriptors
     *            the set of descriptors for this {@code MetacardTypeImpl}
     */
    public MetacardTypeImpl(String name, Set<AttributeDescriptor> descriptors) {
        /*
         * If any defensive logic is added to this constructor, then that logic should be reflected
         * in the deserialization (readObject()) of this object so that the integrity of a
         * serialized object is maintained. For instance, if a null check is added in the
         * constructor, the same check should be added in the readObject() method.
         */
        this.name = name;
        if (descriptors != null) {
            this.descriptors.addAll(descriptors);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<AttributeDescriptor> getAttributeDescriptors() {
        return Collections.unmodifiableSet(descriptors);
    }

    @Override
    public AttributeDescriptor getAttributeDescriptor(String attributeName) {
        if (attributeName == null) {
            return null;
        }
        // TODO could this be faster?
        for (AttributeDescriptor descriptor : descriptors) {
            if (attributeName.equals(descriptor.getName())) {
                return descriptor;
            }
        }
        return null;
    }

    /**
     * Serializes this {@link MetacardTypeImpl} instance.
     * 
     * @serialData First, the name is written as a {@code String} by the default Java serialization
     *             implementation, then the number of {@link AttributeDescriptor} objects is written
     *             as an ( {@code int}), followed by all of the {@code AttributeDescriptor} objects
     *             in no guaranteed sequence or order.
     * @param stream
     *            the {@link ObjectOutputStream} that contains the object to be serialized
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {

        /*
         * defaultWriteObject() is invoked for greater flexibility and compatibility. See the
         * *Serialization Note* in MetacardImpl's class Javadoc.
         */
        stream.defaultWriteObject();

        stream.writeInt(descriptors.size());

        for (AttributeDescriptor descriptor : descriptors) {
            stream.writeObject(descriptor);
        }

    }

    /**
     * Deserializes this {@link MetacardTypeImpl} instance.
     * 
     * @param stream
     *            the {@link ObjectInputStream} that contains the bytes of the object
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

        descriptors = new HashSet<AttributeDescriptor>();

        for (int i = 0; i < numElements; i++) {
            descriptors.add((AttributeDescriptor) stream.readObject());
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result += (descriptors == null) ? 0 : descriptors.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof MetacardType)) {
            return false;
        }

        MetacardTypeImpl other = (MetacardTypeImpl) obj;

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        if (descriptors == null) {
            if (other.descriptors != null) {
                return false;
            }
        } else if (!descriptors.equals(other.descriptors)) {
            return false;
        }

        return true;
    }

}
