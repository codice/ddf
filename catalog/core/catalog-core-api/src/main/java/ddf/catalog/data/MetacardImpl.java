/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.data;

import ddf.catalog.data.AttributeType.AttributeFormat;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the {@link Metacard}'s required attributes. <br/>
 * 
 * <p>
 * <b>Serialization Note</b><br/>
 * 
 * <p>
 * This class is {@link Serializable} and care should be taken with
 * compatibility if changes are made.
 * </p>
 * 
 * For backward and forward compatibility,
 * {@link ObjectOutputStream#defaultWriteObject()} is invoked when this object
 * is serialized because it provides "enhanced flexibility" (Joshua Block
 * <u>Effective Java</u>, Second Edition <i>Item 75</i>). Invoking
 * {@link ObjectOutputStream#defaultWriteObject()} allows the flexibility to add
 * nontransient instance fields in later versions if necessary. If earlier
 * versions do not have the newly added fields, those fields will be ignored and
 * the deserialization will still take place. In addition,
 * {@link ObjectInputStream#defaultReadObject()} is necessary to facilitate any
 * of the written fields. </p>
 * 
 * <p>
 * For what constitutes a compatible change in serialization, see <a href=
 * "http://docs.oracle.com/javase/6/docs/platform/serialization/spec/version.html#6678"
 * >Sun's Guidelines</a>.
 * </p>
 * 
 * @author ddf.isgs@lmco.com
 */
public class MetacardImpl implements Metacard {

	private static final long serialVersionUID = 1L;

	private static XLogger logger = new XLogger(LoggerFactory.getLogger(MetacardImpl.class));

	/**
	 * key/value map of {@link Attribute}s.
	 */
	private transient Map<String, Attribute> map = null;

	private transient Metacard wrappedMetacard;
	
	private transient MetacardType type;
	
	private String sourceId;

	/**
	 * Creates a {@link Metacard} with a type of
	 * {@link BasicTypes#BASIC_METACARD} and empty {@link Attribute}s.
	 */
	public MetacardImpl() {
		/*
		 * If any defensive logic is added to this constructor, then that logic
		 * should be reflected in the deserialization (readObject()) of this
		 * object so that the integrity of a serialized object is maintained.
		 * For instance, if a null check is added in the constructor, the same
		 * check should be added in the readObject() method.
		 */
		this(BasicTypes.BASIC_METACARD);
	}

	/**
	 * Creates a {@link Metacard} with the provided {@link MetacardType} and
	 * empty {@link Attribute}s.
	 * 
	 * @param type
	 *            the {@link MetacardType}
	 */
	public MetacardImpl(MetacardType type) {
		/*
		 * If any defensive logic is added to this constructor, then that logic
		 * should be reflected in the deserialization (readObject()) of this
		 * object so that the integrity of a serialized object is maintained.
		 * For instance, if a null check is added in the constructor, the same
		 * check should be added in the readObject() method.
		 */
		map = new HashMap<String, Attribute>();
		if(type != null) {
			this.type = type;
		} else {
			throw new IllegalArgumentException(MetacardType.class.getName() + " instance should not be null.") ;
		}
	}

	/**
	 * Creates a {@link Metacard} with the provided {@link Metacard}.
	 * 
	 * @param metacard
	 *            the {@link Metacard} to create this new {@code Metacard} from
	 */
	public MetacardImpl(Metacard metacard) {
		/*
		 * If any defensive logic is added to this constructor, then that logic
		 * should be reflected in the deserialization (readObject()) of this
		 * object so that the integrity of a serialized object is maintained.
		 * For instance, if a null check is added in the constructor, the same
		 * check should be added in the readObject() method.
		 */
		this.wrappedMetacard = metacard;
		if(metacard.getMetacardType() != null) {
			this.type = metacard.getMetacardType();
		} else {
			throw new IllegalArgumentException(MetacardType.class.getName() + " instance should not be null.") ;
		}

	}

	@Override
	public Date getCreatedDate() {
		return requestDate(Metacard.CREATED);
	}

	/**
	 * Sets the date/time the {@link Metacard} was created. <br/>
	 * Convenience method for <code>
	 * {@link #setAttribute setAttribute}(new {@link AttributeImpl}({@link Metacard#CREATED}, created))
	 * </code>
	 * 
	 * @param created
	 *            {@link Date} when this {@link Metacard} was created.
	 * 
	 * @see Metacard#CREATED
	 */
	public void setCreatedDate(Date created) {
		setAttribute(Metacard.CREATED, created);
	}

	@Override
	public Date getModifiedDate() {
		return requestDate(Metacard.MODIFIED);
	}

	/**
	 * Sets the date/time the {@link Metacard} was last modifed. <br/>
	 * Convenience method for <code>
	 * {@link #setAttribute setAttribute}(new {@link AttributeImpl}({@link Metacard#MODIFIED}, modified))
	 * </code>
	 * 
	 * @param modified
	 *            {@link Date} when this {@link Metacard} was last modified.
	 * 
	 * @see Metacard#MODIFIED
	 */
	public void setModifiedDate(Date modified) {
		setAttribute(Metacard.MODIFIED, modified);
	}

	@Override
	public Date getExpirationDate() {
		return requestDate(Metacard.EXPIRATION);
	}

	/**
	 * Sets the date/time this {@link Metacard} is no longer valid and could be
	 * removed. <br/>
	 * Convenience method for <code>
	 * {@link #setAttribute setAttribute}(new {@link AttributeImpl}({@link Metacard#EXPIRATION}, expiration))
	 * </code>
	 * 
	 * @param expiration
	 *            {@link Date} when the {@link Metacard} expires and should be
	 *            removed from any stores.
	 * 
	 * @see Metacard#EXPIRATION
	 */
	public void setExpirationDate(Date expiration) {
		setAttribute(Metacard.EXPIRATION, expiration);
	}

	@Override
	public Date getEffectiveDate() {
		return requestDate(Metacard.EFFECTIVE);
	}

	/**
	 * Sets the date/time the {@link Metacard} was last known to be valid. <br/>
	 * Convenience method for <code>
	 * {@link #setAttribute setAttribute}(new {@link AttributeImpl}({@link Metacard#EFFECTIVE}, effective))
	 * </code>
	 * 
	 * @param effective
	 *            {@link Date} when the information represented by the
	 *            {@link Metacard} was last known to be valid.
	 * 
	 * @see Metacard#EFFECTIVE
	 */
	public void setEffectiveDate(Date effective) {
		setAttribute(Metacard.EFFECTIVE, effective);
	}

	@Override
	public String getId() {
		return requestString(Metacard.ID);
	}

	/**
	 * Sets the ID of the {@link Metacard}. <br/>
	 * Convenience method for <code>
	 * {@link #setAttribute setAttribute}(new {@link AttributeImpl}({@link Metacard#ID}, id))
	 * </code>
	 * 
	 * @param id
	 *            unique identifier of the Metacard.
	 * 
	 * @see Metacard#ID
	 */
	public void setId(String id) {
		setAttribute(Metacard.ID, id);
	}

	@Override
	public String getLocation() {
		return requestString(Metacard.GEOGRAPHY);
	}

	/**
	 * Sets the WKT representation of the geometry. <br/>
	 * Convenience method for <code>
	 * {@link #setAttribute setAttribute}(new {@link AttributeImpl}({@link Metacard#GEOGRAPHY}, wkt))
	 * </code>
	 * 
	 * @param wkt
	 *            WKT-defined geospatial {@link String}, returns null if not
	 *            applicable
	 * 
	 * @see Metacard#GEOGRAPHY
	 */
	public void setLocation(String wkt) {
		setAttribute(Metacard.GEOGRAPHY, wkt);
	}

	@Override
	public String getSourceId() {
        return wrappedMetacard != null ? wrappedMetacard.getSourceId() : sourceId;
	}

	public void setSourceId(String sourceId) {
	    if (wrappedMetacard != null) {
	        wrappedMetacard.setSourceId(sourceId);
	    } else {
	        this.sourceId = sourceId;
	    }
	}

	@Override
	public byte[] getThumbnail() {
		return requestBytes(Metacard.THUMBNAIL);
	}

	/**
	 * Sets the thumbnail associated with this {@link Metacard}. <br/>
	 * Convenience method for <code>
	 * {@link #setAttribute setAttribute}(new {@link AttributeImpl}({@link Metacard#THUMBNAIL}, bytes))
	 * </code>
	 * 
	 * @param bytes
	 *            thumbnail for the {@link Metacard}.
	 * 
	 * @see Metacard#THUMBNAIL
	 */
	public void setThumbnail(byte[] bytes) {
		setAttribute(Metacard.THUMBNAIL, bytes);
	}

	@Override
	public String getTitle() {
		return requestString(Metacard.TITLE);
	}

	/**
	 * Sets the title of this {@link Metacard}. <br/>
	 * Convenience method for <code>
	 * {@link #setAttribute setAttribute}(new {@link AttributeImpl}({@link Metacard#TITLE}, id))
	 * </code>
	 * 
	 * @param title
	 *            Title of the {@link Metacard}
	 * 
	 * @see Metacard#TITLE
	 */
	public void setTitle(String title) {
		setAttribute(Metacard.TITLE, title);
	}

	@Override
	public String getMetadata() {
		return requestString(Metacard.METADATA);
	}

	/**
	 * Sets the metadata associated with this {@link Metacard}. <br/>
	 * Convenience method for <code>
	 * {@link #setAttribute setAttribute}(new {@link AttributeImpl}({@link Metacard#METADATA}, metadata))
	 * </code>
	 * 
	 * @param metadata
	 *            Associated metadata of the {@link Metacard}
	 * 
	 * @see Metacard#METADATA
	 */
	public void setMetadata(String metadata) {
		setAttribute(Metacard.METADATA, metadata);
	}

	@Override
	public MetacardType getMetacardType() {
		return type;
	}

	/**
	 * Sets the {@link MetacardType} of the {@link Metacard}.
	 * 
	 * @param type
	 *            {@link MetacardType} of the {@link Metacard}
	 * 
	 * @see MetacardType
	 */
    public void setType(MetacardType type) {
        this.type = type;
    }

	@Override
	public URI getContentTypeNamespace() {
		URI uri = null;
		String uriString = requestString(Metacard.TARGET_NAMESPACE);
		if (uriString != null && !uriString.isEmpty()) {
			uri = URI.create(uriString);
		}
		return uri;
	}

	/**
	 * Some types of metadata use different content types. If utilized, sets the
	 * {@link URI} of the content type. <br/>
	 * Convenience method for <code>
	 * {@link #setAttribute setAttribute}(new {@link AttributeImpl}({@link Metacard#TARGET_NAMESPACE}, targetNamespace))
	 * </code>
	 * 
	 * @param targetNamespace
	 *            {@link URI} of the sub-type, null if unused
	 * 
	 * @see Metacard#TARGET_NAMESPACE
	 */
	public void setTargetNamespace(URI targetNamespace) {
		setAttribute(Metacard.TARGET_NAMESPACE, targetNamespace.toASCIIString());
	}

	@Override
	public String getContentTypeName() {
		return requestString(Metacard.CONTENT_TYPE);
	}

	/**
	 * Sets the name of the content type of the {@link Metacard}. <br/>
	 * Convenience method for <code>
	 * {@link #setAttribute setAttribute}(new {@link AttributeImpl}({@link Metacard#CONTENT_TYPE}, contentType))
	 * </code>
	 * 
	 * @param contentType
	 *            name of content type of the {@link Metacard}
	 * 
	 * @see Metacard#CONTENT_TYPE
	 */
	public void setContentTypeName(String contentType) {
		setAttribute(Metacard.CONTENT_TYPE, contentType);
	}

	@Override
	public String getContentTypeVersion() {
		return requestString(Metacard.CONTENT_TYPE_VERSION);
	}

	/**
	 * Sets the version of the content type of the {@link Metacard}. <br/>
	 * Convenience method for <code>
	 * {@link #setAttribute setAttribute}(new {@link AttributeImpl}({@link Metacard#CONTENT_TYPE_VERSION}, contentTypeVersion))
	 * </code>
	 * 
	 * @param contentTypeVersion
	 *            version of content type of the {@link Metacard}
	 * 
	 * @see Metacard#CONTENT_TYPE_VERSION
	 */
	public void setContentTypeVersion(String contentTypeVersion) {
		setAttribute(Metacard.CONTENT_TYPE_VERSION, contentTypeVersion);
	}

	@Override
	public URI getResourceURI() {
		URI uri = null;
		String data = requestString(Metacard.RESOURCE_URI);
		if (data != null) {
			try {
				uri = new URI(data);
			} catch (URISyntaxException e) {
				logger.warn("failed parsing URI string, returning null");
			}
		}
		return uri;
	}

	/**
	 * Sets the value of this {@link Metacard}s Resource URI and in the form of
	 * a {@link URI} Object. <br/>
	 * Convenience method for <code>
	 * {@link #setAttribute setAttribute}(new {@link AttributeImpl}({@link Metacard#RESOURCE_URI}, uri))
	 * </code>
	 * 
	 * @param uri
	 *            a {@link URI} representation of the {@link Metacard}'s
	 *            {@link Metacard#RESOURCE_URI Resource URI} which itself is
	 *            stored as a {@link AttributeFormat#STRING String
	 *            AttributeFormat}
	 * 
	 * @see Metacard#RESOURCE_URI
	 */
	public void setResourceURI(URI uri) {
		if (uri == null) {
			return;
		}
		setAttribute(RESOURCE_URI, uri.toString());
	}

	@Override
	public String getResourceSize() {
		return requestString(Metacard.RESOURCE_SIZE);
	}

	/**
	 * Sets the size of the resource which may or may not contain a unit. <br/>
	 * Convenience method for <code>
	 * {@link #setAttribute setAttribute}(new {@link AttributeImpl}({@link Metacard#RESOURCE_SIZE}, dadSize))
	 * </code>
	 * 
	 * @param dadSize
	 *            {@link String} representation of the size
	 * 
	 * @see Metacard#RESOURCE_SIZE
	 */
	public void setResourceSize(String dadSize) {
		setAttribute(RESOURCE_SIZE, dadSize);
	}

    /**
     * Returns the security relevant markings on the {@link ddf.catalog.data.Metacard}.
     *
     * @return security markings
     */
    public Map<String, List<String>> getSecurity()
    {
        return requestData(Metacard.SECURITY, HashMap.class);
    }

    /**
     * Sets the security markings on this {@link Metacard}. <br />
     * Convenience method for <code>
     * {@link #setAttribute setAttribute}(new {@link AttributeImpl}({@link Metacard#SECURITY}, security))
     * </code>
     *
     * @param security
     */
    public void setSecurity(HashMap<String, List<String>> security)
    {
        setAttribute(Metacard.SECURITY, security);
    }


    /**
	 * The brains of the operation -- does the interaction with the map or the
	 * wrapped metacard.
	 * 
	 * @param <T>
	 *            the type of the Attribute value expected
	 * @param attributeName
	 *            the name of the {@link Attribute} to retrieve
	 * @param returnType
	 *            the class that the value of the {@link AttributeType} is
	 *            expected to be bound to
	 * @return the value of the requested {@link Attribute} name
	 */
	protected <T> T requestData(String attributeName, Class<T> returnType) {

		Attribute attribute = getAttribute(attributeName);

		if (attribute == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Attribute " + attributeName + " was not found, returning null");
			}
			return null;
		}

		Serializable data = attribute.getValue();

		if (returnType.isAssignableFrom(data.getClass())) {
			return returnType.cast(data);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug(data.getClass().toString() + " can not be assigned to " + returnType.toString());
			}
		}

		return null;
	}

	/**
	 * Get {@link Date} data from the map or wrapped metacard. <br/>
	 * Convenience method for <code>
	 * {@link #requestData requestData}(key, Date.class))
	 * </code>
	 * 
	 * @param key
	 *            the name of the {@link Attribute} to retrieve
	 * @return the value of the requested {@link Attribute} name
	 * 
	 * @see MetacardImpl#requestData(String, Class)
	 */
	protected Date requestDate(String key) {
		return requestData(key, Date.class);
	}

	/**
	 * Get {@link Double} data from the map or wrapped metacard. <br/>
	 * Convenience method for <code>
	 * {@link #requestData requestData}(key, Double.class))
	 * </code>
	 * 
	 * @param key
	 *            the name of the {@link Attribute} to retrieve
	 * @return the value of the requested {@link Attribute} name
	 * 
	 * @see MetacardImpl#requestData(String, Class)
	 */
	protected Double requestDouble(String key) {
		return requestData(key, Double.class);
	}

	/**
	 * Get {@link BinaryContent} data from the map or wrapped metacard. <br/>
	 * Convenience method for <code>
	 * {@link #requestData requestData}(key, BinaryContent.class))
	 * </code>
	 * 
	 * @param key
	 *            the name of the {@link Attribute} to retrieve
	 * @return the value of the requested {@link Attribute} name
	 * 
	 * @see MetacardImpl#requestData(String, Class)
	 */
	protected InputStream requestInputStream(String key) {
		BinaryContent data = requestData(key, BinaryContent.class);
		if (data != null)
			return data.getInputStream();
		return null;
	}

	/**
	 * Get {@link Integer} data from the map or wrapped metacard. <br/>
	 * Convenience method for <code>
	 * {@link #requestData requestData}(key, Integer.class))
	 * </code>
	 * 
	 * @param key
	 *            the name of the {@link Attribute} to retrieve
	 * @return the value of the requested {@link Attribute} name
	 * 
	 * @see MetacardImpl#requestData(String, Class)
	 */
	protected Integer requestInteger(String key) {
		return requestData(key, Integer.class);
	}

	/**
	 * Get {@link Long} data from the map or wrapped metacard. <br/>
	 * Convenience method for <code>
	 * {@link #requestData requestData}(key, Long.class))
	 * </code>
	 * 
	 * @param key
	 *            the name of the {@link Attribute} to retrieve
	 * @return the value of the requested {@link Attribute} name
	 * 
	 * @see MetacardImpl#requestData(String, Class)
	 */
	protected Long requestLong(String key) {
		return requestData(key, Long.class);
	}

	/**
	 * Get {@link String} data from the map or wrapped metacard. <br/>
	 * Convenience method for <code>
	 * {@link #requestData requestData}(key, String.class))
	 * </code>
	 * 
	 * @param key
	 *            the name of the {@link Attribute} to retrieve
	 * @return the value of the requested {@link Attribute} name
	 * 
	 * @see MetacardImpl#requestData(String, Class)
	 */
	protected String requestString(String key) {
		return requestData(key, String.class);
	}

	/**
	 * Get {@link byte[]} data from the map or wrapped metacard. <br/>
	 * Convenience method for <code>
	 * {@link #requestData requestData}(key, byte[].class))
	 * </code>
	 * 
	 * @param key
	 *            the name of the {@link Attribute} to retrieve
	 * @return the value of the requested {@link Attribute} name
	 * 
	 * @see MetacardImpl#requestData(String, Class)
	 */
	protected byte[] requestBytes(String key) {
		return requestData(key, byte[].class);
	}

	@Override
	public Attribute getAttribute(String name) {
		return (wrappedMetacard != null) ? wrappedMetacard.getAttribute(name) : map.get(name);
	}

	/**
	 * Set an attribute via a name/value pair.
	 * 
	 * @param name
	 *            the name of the {@link Attribute}
	 * @param value
	 *            the value of the {@link Attribute}
	 */
	public void setAttribute(String name, Serializable value) {
		setAttribute(new AttributeImpl(name, value));
	}

	@Override
	public void setAttribute(Attribute attribute) {

		if (attribute == null) {
			return;
		}

		if (wrappedMetacard != null) {
			wrappedMetacard.setAttribute(attribute);
		} else {
			String name = attribute.getName();
			Serializable value = attribute.getValue();
			if (name != null) {
				if (value != null) {
					map.put(name, attribute);
				} else {
					map.remove(name);
				}
			}
		}
	}

	/**
	 * Serializes this {@link MetacardImpl} instance.
	 * 
	 * @serialData First, all non-transient fields are written out by the
	 *             default Java serialization implementation (
	 *             {@link ObjectOutputStream#defaultWriteObject()}) . Next, the
	 *             {@link MetacardType} is written out as a
	 *             {@link MetacardTypeImpl}. Then the <i>number</i> of
	 *             {@code Attribute} objects is written as an {@code int}. After
	 *             the number of objects, each {@code Attribute} object is
	 *             written out.
	 * 
	 *             <p>
	 *             The MetacardType object is written out as a
	 *             {@link MetacardTypeImpl} object because
	 *             {@link MetacardTypeImpl} is a class that is part of the DDF
	 *             API and is guaranteed to be on the classpath when this object
	 *             is deserialized. Secondly, the {@link MetacardTypeImpl} has a
	 *             trusted serialization implementation where the object's
	 *             logical representation is serialized.
	 *             </p>
	 * 
	 * @param stream
	 *            the {@link ObjectOutputStream} that contains the object to
	 *            be serialized
	 * @throws IOException
	 */
	private void writeObject(ObjectOutputStream stream) throws IOException {

		/*
		 * defaultWriteObject() is invoked for greater flexibility and
		 * compatibility. See the *Serialization Note* in MetacardImpl's class
		 * Javadoc.
		 */
		stream.defaultWriteObject();

		/*
		 * Cannot allow unknown implementations of MetacardType to be
		 * serialized. Must convert them to our implementation to guarantee it
		 * is serializing the logical representation and not the physical
		 * representation. 
		 */
		if(type instanceof MetacardTypeImpl) {
			stream.writeObject(type);
		}
		else {
			MetacardTypeImpl mt = new MetacardTypeImpl(type.getName(), type.getAttributeDescriptors());
			stream.writeObject(mt);
		}
		
		if (map != null) {
			stream.writeInt(map.size());

			for (Attribute attribute : this.map.values()) {
				stream.writeObject(attribute);
			}
		} else {
			if (wrappedMetacard != null && wrappedMetacard.getMetacardType() != null) {

				MetacardType metacardType = wrappedMetacard.getMetacardType();

				List<Attribute> attributes = new ArrayList<Attribute>();

				if (metacardType.getAttributeDescriptors() == null) {
					// no descriptors, means no attributes can be defined.
					// no attributes defined, means no attributes written to
					// disk
					stream.writeInt(0);
				} else {

					for (AttributeDescriptor ad : metacardType.getAttributeDescriptors()) {

						Attribute attribute = wrappedMetacard.getAttribute(ad.getName());

						if (attribute != null) {
							attributes.add(attribute);
						}
					}

					// Must loop again because the size of the attributes list
					// is not known until list has been fully populated.
					stream.writeInt(attributes.size());

					for (Attribute attribute : attributes) {
						stream.writeObject(attribute);
					}
				}
			}
		}

	}

	/**
	 * Deserializes this {@link MetacardImpl}'s instance.
	 * @param stream
	 *            the {@link ObjectInputStream} that contains the bytes of the
	 *            object
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {

		/*
		 * defaultReadObject() is invoked for greater flexibility and
		 * compatibility. See the *Serialization Note* in MetacardImpl's class
		 * Javadoc.
		 */
		stream.defaultReadObject();
		
		map = new HashMap<String, Attribute>();
		
		wrappedMetacard = null;
		
		type = (MetacardType) stream.readObject();
		
		if(type == null) {
			throw new InvalidObjectException(MetacardType.class.getName() + " instance cannot be null.");
		}
		
		int numElements = stream.readInt();

		for (int i = 0; i < numElements; i++) {
		
			Attribute attribute = (Attribute) stream.readObject();

			if (attribute != null) {

				AttributeDescriptor attributeDescriptor = getMetacardType().getAttributeDescriptor(attribute.getName());
				
				if (attributeDescriptor != null && attribute.getValue() != null) {
					attributeDescriptor.getType().getAttributeFormat();
					attributeDescriptor.getType().getClass();
				}

			}

			setAttribute(attribute);
		}

	}

}
