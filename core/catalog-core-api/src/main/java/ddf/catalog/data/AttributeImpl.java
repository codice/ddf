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
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * A simple implementation of {@link Attribute}.
 * 
 * <p>
 * This class is {@link Serializable} and care should be taken with compatibility if changes are
 * made.
 * </p>
 * 
 * <p>
 * For what constitutes a compatible change in serialization, see <a href=
 * "http://docs.oracle.com/javase/6/docs/platform/serialization/spec/version.html#6678" >Sun's
 * Guidelines</a>.
 * </p>
 * 
 * @author ddf.isgs@lmco.com
 * 
 */
public class AttributeImpl implements Attribute {

    private static final long serialVersionUID = 1L;

    /**
     * Nontransient field that holds the name of the {@link Attribute}.
     * 
     * @serial
     */
    protected String name;

    private transient List<Serializable> values;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Serializable getValue() {
        if (!values.isEmpty()) {
            return values.get(0);
        } else {
            return null;
        }
    }

    /**
     * Constructor
     * 
     * @param name
     *            - the name of this {@link Attribute}
     * @param value
     *            - the value of this {@link Attribute}
     */
    public AttributeImpl(String name, Serializable value) {
        /*
         * If any defensive logic is added to this constructor, then that logic should be reflected
         * in the deserialization (readObject()) of this object so that the integrity of a
         * serialized object is maintained. For instance, if a null check is added in the
         * constructor, the same check should be added in the readObject() method.
         */
        this.name = name;
        this.values = createPopulatedList(value);
    }

    private List<Serializable> createPopulatedList(Serializable value) {
        List<Serializable> list = new LinkedList<Serializable>();
        list.add(value);
        return list;
    }

    @Override
    public List<Serializable> getValues() {
        return values;
    }

    /**
     * Adds a value to this {@link Attribute}
     * 
     * @param value
     *            the value to add
     */
    public void addValue(Serializable value) {
        values.add(value);
    }

    /**
     * Clears all {@link Attribute} values
     */
    public void clearValues() {
        values.clear();
    }

    /**
     * Serializes this {@link AttributeImpl} instance.
     * 
     * @serialData First, all non-transient fields are written out by the default Java serialization
     *             implementation (ObjectInputStream.defaultWriteObject()). Then the number of
     *             "value" objects is written out as an ({@code int}). After the number of objects,
     *             each "value" object is written out (each as {@code Serializable}).
     * @param s
     *            - the {@link ObjectOutputStream} which contains the object to be serialized
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream s) throws IOException {

        /*
         * defaultWriteObject() is invoked for greater flexibility and compatibility. See the
         * *Serialization Note* in MetacardImpl's class Javadoc.
         */
        s.defaultWriteObject();

        s.writeInt(values.size());

        for (Serializable ser : values) {
            s.writeObject(ser);
        }
    }

    /**
     * Deserializes this {@link AttributeImpl}'s instance.
     * 
     * @param stream
     *            the {@link ObjectInputStream} that contains the bytes of the object
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {

        /*
         * defaultReadObject() is invoked for greater flexibility and compatibility. See the
         * *Serialization Note* in MetacardImpl's class Javadoc.
         */
        s.defaultReadObject();

        int numElements = s.readInt();

        validateNonEmpty(numElements);

        values = new LinkedList<Serializable>();
        for (int i = 0; i < numElements; i++) {
            values.add((Serializable) s.readObject());
        }

        validateUntampered(numElements);

    }

    private void validateUntampered(int numElements) throws InvalidObjectException {
        // Invariant: When the object was serialized, the integer written to
        // disk matched the number of value objects written to disk.
        if (values.size() != numElements) {
            throw new InvalidObjectException(
                    "Corrupt object: written number of values does not match actual number of values.");
        }
    }

    private void validateNonEmpty(int numElements) throws InvalidObjectException {
        // Invariant: This implementation does not allow an Attribute object
        // with no values
        if (numElements == 0) {
            throw new InvalidObjectException("Cannot construct " + this.getClass().getName()
                    + " object without any values.");
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + " {name=" + this.name + ", values=" + this.values + "}";
    }
}
