/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.data.dynamic.impl;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.LazyDynaBean;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.dynamic.api.DynamicMetacard;
import ddf.catalog.data.impl.AttributeImpl;

/**
 * Implements the {@link Metacard}'s required attributes using dynamic beans. This allows for the
 * creation and modification of beans on the fly.<br/>
 * <p/>
 * <p>
 * <b>Serialization Note</b><br/>
 * <p/>
 * <p>
 * This class is {@link Serializable} and care should be taken with compatibility if changes are
 * made.
 * </p>
 * <p/>
 * For backward and forward compatibility, {@link java.io.ObjectOutputStream#defaultWriteObject()} is
 * invoked when this object is serialized because it provides "enhanced flexibility" (Joshua Block
 * <u>Effective Java</u>, Second Edition <i>Item 75</i>). Invoking
 * {@link java.io.ObjectOutputStream#defaultWriteObject()} allows the flexibility to add nontransient
 * instance fields in later versions if necessary. If earlier versions do not have the newly added
 * fields, those fields will be ignored and the deserialization will still take place. In addition,
 * {@link java.io.ObjectInputStream#defaultReadObject()} is necessary to facilitate any of the written
 * fields. </p>
 * <p/>
 * <p>
 * For what constitutes a compatible change in serialization, see <a href=
 * "http://docs.oracle.com/javase/6/docs/platform/serialization/spec/version.html#6678" >Sun's
 * Guidelines</a>.
 * </p>
 */
public class DynamicMetacardImpl implements DynamicMetacard {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicMetacardImpl.class);

    private LazyDynaBean attributesBean;
    private ArrayList<String> metacardTypes = new ArrayList<>();

    public DynamicMetacardImpl() {

    }

    /**
     * Creates a {@link Metacard} from the provided {@link DynaClass} with empty
     * {@link Attribute}s.
     */
/*
    public DynamicMetacardImpl(DynaClass dynaClass) {
        attributesBean = new LazyDynaBean(dynaClass);
    }
*/

    /**
     * Creates a {@link Metacard} from the provided {@link LazyDynaBean} or another DynamicMetacardImpl
     * with empty {@link Attribute}s.
     */
    public DynamicMetacardImpl(LazyDynaBean dynaBean) {
        attributesBean = dynaBean;
        metacardTypes.add(getName());
    }

    /**
     * Returns the property of this DynamicMetacardImpl as a {@link Attribute}.
     *
     * @param s name of the attribute whose value(s) are to be retrieved
     * @return Attribute object containing the name and attributes for the specified property
     */
    @Override
    public Attribute getAttribute(String s) {
        List<Serializable> list = null;
        Serializable obj = (Serializable) attributesBean.get(s);
        if (obj != null) {
            list = new ArrayList<Serializable>();
            if (obj instanceof Collection) {
                list.addAll((List) obj);
            } else if (obj instanceof Map) {
                list.addAll(((Map) obj).values());
            } else if (obj instanceof byte[]) {
                byte[] temp = (byte[]) obj;
                if (temp.length > 0) {
                    list.add(obj);
                }
            } else {
                list.add(obj);
            }
        }

        Attribute attribute = null;
        if (CollectionUtils.isNotEmpty(list)) {
            attribute = new AttributeImpl(s, list);
        }

        return attribute;
    }

    /**
     * Sets a metacard property specified by the name and value(s) in the given {@link Attribute}.
     * If a property of the given name does not exist, it is created dynamically.
     *
     * @param attribute Attribute to be set on the current metacard
     */
    @Override
    public void setAttribute(Attribute attribute) {
        if (attribute != null) {
            String name = attribute.getName();
            List<Serializable> values = attribute.getValues();
            Serializable value = attribute.getValue();
            if ((values != null) && (values.size() > 1)) {
                attributesBean.set(name, values);
            } else {
                if (value instanceof URI) {
                    value = ((URI) value).toString();
                }
                attributesBean.set(name, value);
            }
        }
    }

    /**
     * Returns an instance of a {@link MetacardType} the describes this {@link Metacard} instance.
     *
     * @return a {@link MetacardType} that describes this {@link Metacard} instance
     */
    @Override
    public MetacardType getMetacardType() {
        return this;
    }

    /**
     * Returns the id of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute}({@link Metacard#ID})
     * </code>
     *
     * @return the id for this {@link Metacard}
     */
    @Override
    public String getId() {
        return getPropertyAsString(Metacard.ID);
    }

    /**
     * Returns the metadata field of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute}({@link Metacard#METADATA})
     * </code>
     *
     * @return the metadata field this {@link Metacard}
     */
    @Override
    public String getMetadata() {
        return getPropertyAsString(Metacard.METADATA);
    }

    /**
     * Returns the created date of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute}({@link Metacard#CREATED})
     * </code>
     *
     * @return the created date for this {@link Metacard}
     */
    @Override
    public Date getCreatedDate() {
        return getPropertyAsDate(Metacard.CREATED);
    }

    /**
     * Returns the modified of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute}({@link Metacard#MODIFIED})
     * </code>
     *
     * @return the id for this {@link Metacard}
     */
    @Override
    public Date getModifiedDate() {
        return getPropertyAsDate(Metacard.MODIFIED);
    }

    /**
     * Returns the expiration date of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute}({@link Metacard#EXPIRATION})
     * </code>
     *
     * @return the expiration for this {@link Metacard}
     */
    @Override
    public Date getExpirationDate() {
        return getPropertyAsDate(Metacard.EXPIRATION);
    }

    /**
     * Returns the effective date of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute}({@link Metacard#EFFECTIVE})
     * </code>
     *
     * @return the effective date for this {@link Metacard}
     */
    @Override
    public Date getEffectiveDate() {
        return getPropertyAsDate(Metacard.EFFECTIVE);
    }

    /**
     * Returns the location of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute}({@link Metacard#GEOGRAPHY})
     * </code>
     *
     * @return the location for this {@link Metacard}
     */
    @Override
    public String getLocation() {
        return getPropertyAsString(Metacard.GEOGRAPHY);
    }

    /**
     * Returns the source id of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute}({@link Metacard#SOURCE_ID})
     * </code>
     *
     * @return the source id for this {@link Metacard}
     */
    @Override
    public String getSourceId() {
        return getPropertyAsString(Metacard.SOURCE_ID);
    }

    /**
     * Sets the source id of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #setAttribute}({@link Metacard#SOURCE_ID}, value)
     * </code>
     *
     * @param s the source id for this {@link Metacard}
     */
    @Override
    public void setSourceId(String s) {
        setAttribute(Metacard.SOURCE_ID, s);
    }

    /**
     * Returns the title of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute}({@link Metacard#TITLE})
     * </code>
     *
     * @return the title for this {@link Metacard}
     */
    @Override
    public String getTitle() {
        return getPropertyAsString(Metacard.TITLE);
    }

    /**
     * Returns the resource URI of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute}({@link Metacard#RESOURCE_URI})
     * </code>
     *
     * @return the resource URI for this {@link Metacard}
     */
    @Override
    public URI getResourceURI() {
        return getPropertyAsURI(Metacard.RESOURCE_URI);
    }

    /**
     * Returns the resource size for this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute}({@link Metacard#RESOURCE_URI})
     * </code>
     *
     * @return the resource size for this {@link Metacard}
     */
    @Override
    public String getResourceSize() {
        return getPropertyAsString(Metacard.RESOURCE_SIZE);
    }

    /**
     * Returns the thumbnail of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute}({@link Metacard#THUMBNAIL})
     * </code>
     *
     * @return the thumbnail for this {@link Metacard}
     */
    @Override
    public byte[] getThumbnail() {
        return getPropertyAsBinary(Metacard.THUMBNAIL);
    }

    /**
     * Returns the content type of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute}({@link Metacard#CONTENT_TYPE})
     * </code>
     *
     * @return the content type for this {@link Metacard}
     */
    @Override
    public String getContentTypeName() {
        return getPropertyAsString(Metacard.CONTENT_TYPE);
    }

    /**
     * Returns the content type version of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute}({@link Metacard#CONTENT_TYPE_VERSION})
     * </code>
     *
     * @return the content type version for this {@link Metacard}
     */
    @Override
    public String getContentTypeVersion() {
        return getPropertyAsString(Metacard.CONTENT_TYPE_VERSION);
    }

    /**
     * Returns the content type namespace of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute}({@link Metacard#TARGET_NAMESPACE})
     * </code>
     *
     * @return the content type namespace for this {@link Metacard}
     */
    @Override
    public URI getContentTypeNamespace() {
        return getPropertyAsURI(Metacard.TARGET_NAMESPACE);
    }

    /**
     * Returns the value for the given attribute name as a string
     *
     * @param attributeName
     * @return string value of the given attribute, or null
     */
    private String getPropertyAsString(String attributeName) {
        String s = null;
        Object o = attributesBean.get(attributeName);
        if (o instanceof String) {
            s = (String) o;
        } else {
            logUnexpectedDataType(attributeName, o);
        }
        return s;
    }

    /**
     * Returns the value for the given attribute name as a Date
     *
     * @param attributeName
     * @return Date value of the given attribute or null if the value is not a Date
     */
    private Date getPropertyAsDate(String attributeName) {
        Date date = null;
        Object o = attributesBean.get(attributeName);
        if (o instanceof Date) {
            date = (Date) o;
        } else {
            logUnexpectedDataType(attributeName, o);
        }
        return date;
    }

    /**
     * Returns the value for the given attribute name as a byte[]
     *
     * @param attributeName
     * @return byte[] value of the given attribute or null if the value is not a byte[]
     */
    private byte[] getPropertyAsBinary(String attributeName) {
        byte[] binary = null;
        Object o = attributesBean.get(attributeName);
        if (o instanceof byte[]) {
            binary = (byte[]) o;
            if (binary.length == 0) {
                binary = null;
            }
        } else {
            logUnexpectedDataType(attributeName, o);
        }
        return binary;
    }

    /**
     * Returns the value for the given attribute name as a URI. URIs can be stored as Strings so
     * try to convert a String to a URI if possible.
     *
     * @param attributeName
     * @return URI value of the given attribute or null if it cannot be converted into a URI
     */
    private URI getPropertyAsURI(String attributeName) {
        URI uri = null;
        Object o = attributesBean.get(attributeName);
        if (o instanceof URI) {
            uri = (URI) o;
        } else if (o instanceof String) {
            try {
                uri = new URI((String) o);
            } catch (URISyntaxException e) {
                LOGGER.warn("Error converting String to URI for {}: {}",
                        attributeName,
                        e.getMessage());
            }
        } else {
            logUnexpectedDataType(attributeName, o);
        }
        return uri;
    }

    private void logUnexpectedDataType(String attributeName, Object o) {
        LOGGER.warn("Unexpected data type retrieving {}: {}",
                attributeName,
                o == null ? "null" : o.getClass().getName());
    }

    /**
     * Returns the name representing the equivalent {@link MetacardType} name.
     *
     * @return string value of the given attribute
     */
    @Override
    public String getName() {
        return attributesBean.getDynaClass()
                .getName();
    }

    /**
     * Returns a set of {@link AttributeDescriptor}s describing all the attributes
     * of this Dynamic Metacard.
     *
     * @return the set of {@link AttributeDescriptor}s for this metacard
     */
    @Override
    public Set<AttributeDescriptor> getAttributeDescriptors() {
        DynaProperty[] properties = attributesBean.getDynaClass()
                .getDynaProperties();
        Set<AttributeDescriptor> descriptors = new HashSet<>();
        for (DynaProperty property : properties) {
            descriptors.add(new MetacardAttributeDescriptor(property));
        }
        return descriptors;
    }

    /**
     * Returns the {@link AttributeDescriptor} for the specified attribute.
     *
     * @param s name of the attribute whose descriptor is to be returned
     * @return the {@link AttributeDescriptor} for the named attribute
     */
    @Override
    public AttributeDescriptor getAttributeDescriptor(String s) {
        AttributeDescriptor descriptor = null;
        if (s != null) {
            DynaProperty dynaProperty = attributesBean.getDynaClass().getDynaProperty(s);
            if (dynaProperty != null) {
                descriptor = new MetacardAttributeDescriptor(dynaProperty);
            }
        }
        return descriptor;
    }

    /**
     * Adds another value to the given attribute. If the attribute is not a multi-valued
     * attribute, it replaces the current value with the provided value.
     *
     * @param name  name of the attribute to set
     * @param value the value to be set
     */
    @Override
    public void addAttribute(String name, Object value) {
        if (name == null) {
            LOGGER.warn("Call to add attribute with null name - no action taken.");
            return;
        }

        DynaProperty dynaProperty = attributesBean.getDynaClass().getDynaProperty(name);
        if (value instanceof URI) {
            value = ((URI) value).toString();
        }

        try {
            if (dynaProperty == null) {
                attributesBean.set(name, 0, value);
            } else {
                if (dynaProperty.isIndexed()) {
                    if (value instanceof Collection) {
                        // add all attributes individually
                        for (Object o : (Collection) value) {
                            attributesBean.set(name, attributesBean.size(name), o);
                        }
                    } else {
                        attributesBean.set(name, attributesBean.size(name), value);
                    }
                } else {
                    LOGGER.debug(
                            "Can't add another value to a simple attribute {} - replacing value.",
                            name);
                    attributesBean.set(name, value);
                }
            }
        } catch (ConversionException e) {
            if (value != null && dynaProperty != null) {
                LOGGER.warn(
                        "Unable to to convert provided value of class {} to attribute {} value of class {} - no action taken.",
                        value.getClass().getSimpleName(), name, dynaProperty.getContentType().getSimpleName());
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Attribute {} has different than expected cardinality - no action taken",
                    name);
        } catch (IndexOutOfBoundsException e) {
            LOGGER.warn("Trying to set attribute {} at index {} which is out of range.",
                    name,
                    attributesBean.size(name));
        } catch (NullPointerException e) {
            LOGGER.warn(
                    "Trying to set primitive type for attribute {} to a null value - no action taken",
                    name);
        }
    }

    /**
     * Sets the value of the given attribute. If the attribute is a multi-valued
     * attribute, it adds the given value to the current collection. If the value
     * is multi-valued, it replaces the values of the named attribute.
     *
     * @param name  name of the attribute to set
     * @param value the value to be set
     */
    @Override
    public void setAttribute(String name, Object value) {
        if (name == null) {
            LOGGER.warn("Call to set attribute with null name - no action taken.");
            return;
        }

        DynaProperty dynaProperty = attributesBean.getDynaClass()
                .getDynaProperty(name);
        if (value == null) {
            attributesBean.set(name, value);
            return;
        }

        if (value instanceof URI) {
            value = ((URI) value).toString();
        }

        try {
            if (dynaProperty == null) {
                attributesBean.set(name, value);
            } else {
                if ((dynaProperty.getType() == Byte[].class) || !dynaProperty.isIndexed()) {
                    attributesBean.set(name, value);
                } else {
                    if (value instanceof Collection<?>) {
                        LOGGER.debug("Replacing current value of attribute {} with provided collection", name);
                        attributesBean.set(name, value);
                    } else {
                        LOGGER.debug("Adding provided value to attribute {}", name);
                        attributesBean.set(name, attributesBean.size(name), value);
                    }
                }
            }
        } catch (ConversionException e) {
            if (value != null && dynaProperty != null) {
                String classname = dynaProperty.getContentType() == null ?
                        dynaProperty.getType().getSimpleName() :
                        dynaProperty.getContentType().getSimpleName();
                LOGGER.warn(
                        "Unable to to convert provided value of class {} to attribute {} value of class {} - no action taken.",
                        value.getClass().getSimpleName(), name, classname);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Attribute {} has different than expected cardinality - no action taken",
                    name);
        } catch (IndexOutOfBoundsException e) {
            LOGGER.warn("Trying to set attribute {} at index {} which is out of range.",
                    name,
                    attributesBean.size(name));
        } catch (NullPointerException e) {
            LOGGER.warn(
                    "Trying to set primitive type for attribute {} to a null value - no action taken",
                    name);
        }
    }

/*
    public void addMetacardType(String name) {
        if (!metacardTypes.contains(name)) {
            metacardTypes.add(name);
        }
    }
*/

    @Override
    public List<String> getMetacardTypes() {
        return Collections.unmodifiableList(metacardTypes);
    }

    @Override
    public boolean isType(String name) {
        return metacardTypes.contains(name);
    }
}
