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

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Simple implementation of {@link AttributeDescriptor}
 * 
 * <p>
 * This class does not have a custom serialization implementation (like some other API classes such
 * as {@link AttributeImpl}) because its physical representation is the same as its logical
 * representation, which according to Joshua Bloch in <u>Effective Java</u>, Second Edition makes it
 * a good candidate for the default Java serialization implementation.
 * </p>
 * 
 * <p>
 * For what constitutes a compatible change in serialization, see <a href=
 * "http://docs.oracle.com/javase/6/docs/platform/serialization/spec/version.html#6678" >Sun's
 * Guidelines</a>.
 * </p>
 * 
 * @deprecated Use ddf.catalog.data.impl.AttributeDescriptorImpl
 * 
 */
@Deprecated
public class AttributeDescriptorImpl implements AttributeDescriptor {

    private static final int HASHCODE_OFFSET = 17;

    /**
     * This value was chosen because it is an odd prime. It can be also replaced by a shift and a
     * subtraction for better performance, which most VMs do these days. (For more information, read
     * <i>Item 9</i> in <u>Effective Java</u>, Second Edition)
     */
    private static final int HASHCODE_MULTIPLIER = 31;

    private static final long serialVersionUID = 1L;

    protected String name;

    protected boolean indexed;

    protected boolean stored;

    protected boolean tokenized;

    protected boolean multivalued;

    protected AttributeType<?> type;

    /**
     * Constructor
     * 
     * @param name
     *            - the name of this {@link AttributeDescriptor}
     * @param indexed
     *            - whether {@link Attribute} values using this {@link AttributeDescriptor} should
     *            be indexed
     * @param stored
     *            - whether {@link Attribute} values using this {@link AttributeDescriptor} should
     *            be stored
     * @param tokenized
     *            - whether {@link Attribute} values using this {@link AttributeDescriptor} should
     *            be tokenized
     * @param multivalued
     *            whether {@link Attribute} values using this {@link AttributeDescriptor} are
     *            multivalued
     * @param type
     *            - the type of this {@link AttributeDescriptor}
     * @see AttributeDescriptor#isIndexed()
     * @see AttributeDescriptor#isStored()
     * @see AttributeDescriptor#isTokenized()
     * @see AttributeDescriptor#isMultiValued()
     */
    public AttributeDescriptorImpl(String name, boolean indexed, boolean stored, boolean tokenized,
            boolean multivalued, AttributeType<?> type) {
        super();
        this.name = name;
        this.indexed = indexed;
        this.stored = stored;
        this.tokenized = tokenized;
        this.multivalued = multivalued;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AttributeType<?> getType() {
        return type;
    }

    @Override
    public boolean isIndexed() {
        return indexed;
    }

    @Override
    public boolean isStored() {
        return stored;
    }

    @Override
    public boolean isTokenized() {
        return tokenized;
    }

    @Override
    public boolean isMultiValued() {
        return multivalued;
    }

    @Override
    public int hashCode() {

        /*
         * Any major changes to this method, requires changes in the equals method.
         */

        int result = HASHCODE_OFFSET;

        result = HASHCODE_MULTIPLIER * result + (name != null ? name.hashCode() : 0);

        result = HASHCODE_MULTIPLIER * result + (indexed ? 1 : 0);

        result = HASHCODE_MULTIPLIER * result + (stored ? 1 : 0);

        result = HASHCODE_MULTIPLIER * result + (tokenized ? 1 : 0);

        result = HASHCODE_MULTIPLIER * result + (multivalued ? 1 : 0);

        result = HASHCODE_MULTIPLIER * result + type.getAttributeFormat().hashCode();

        result = HASHCODE_MULTIPLIER * result + type.getBinding().hashCode();

        return result;

    }

    @Override
    public boolean equals(Object obj) {

        /*
         * Any major changes to this method such as adding a field check, requires changes in the
         * hashCode method. According to the Java Object Specification, "If two objects are equal
         * according to the equals(Object) method, then calling the hashCode method on each of the
         * two objects must produce the same integer result."
         */

        if (!(obj instanceof AttributeDescriptor)) {
            return false;
        }

        AttributeDescriptor newObject = (AttributeDescriptor) obj;

        if (this.getName() == null) {
            if (newObject.getName() != null) {
                return false;
            }
        } else if (!this.getName().equals(newObject.getName())) {
            return false;
        }

        if (!this.getType().getAttributeFormat().equals(newObject.getType().getAttributeFormat())) {
            return false;
        }

        if (!this.getType().getBinding().equals(newObject.getType().getBinding())) {
            return false;
        }

        if (this.isIndexed() != newObject.isIndexed()) {
            return false;
        }

        if (this.isStored() != newObject.isStored()) {
            return false;
        }

        if (this.isTokenized() != newObject.isTokenized()) {
            return false;
        }

        if (this.isMultiValued() != newObject.isMultiValued()) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
