/*
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
package ddf.catalog.transformer.xml.adapter;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.transformer.xml.binding.Base64BinaryElement;
import ddf.catalog.transformer.xml.binding.BooleanElement;
import ddf.catalog.transformer.xml.binding.DateTimeElement;
import ddf.catalog.transformer.xml.binding.DoubleElement;
import ddf.catalog.transformer.xml.binding.FloatElement;
import ddf.catalog.transformer.xml.binding.GeometryElement;
import ddf.catalog.transformer.xml.binding.IntElement;
import ddf.catalog.transformer.xml.binding.LongElement;
import ddf.catalog.transformer.xml.binding.ObjectElement;
import ddf.catalog.transformer.xml.binding.ShortElement;
import ddf.catalog.transformer.xml.binding.StringElement;
import ddf.catalog.transformer.xml.binding.StringxmlElement;
import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.jvnet.jaxb2_commons.lang.StringUtils;

/** @see <a href="http://stackoverflow.com/a/11967459">http://stackoverflow.com/a/11967459</a> */
@XmlRootElement(name = "metacard", namespace = "urn:catalog:metacard")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"metacardType", "sourceId", "attributes"})
public class AdaptedMetacard implements Metacard {

  private static final String METACARD_URI = "urn:catalog:metacard";

  /** */
  private static final long serialVersionUID = 7675587921316158761L;

  private Metacard delegate = null;

  private MetacardType metacardType;

  private String sourceId = null;

  private Attribute idAttribute;

  // Suppressing Warnings and using HashSet rather than Set here because
  // HashSet implements Serializable (Set does not).
  @SuppressWarnings("all")
  private HashSet<Attribute> attributes = new HashSet<>();

  public AdaptedMetacard(Metacard metacard) {
    if (metacard == null) {
      this.metacardType = MetacardImpl.BASIC_METACARD;
    } else {
      this.sourceId = metacard.getSourceId();
      this.metacardType =
          metacard.getMetacardType() != null
              ? metacard.getMetacardType()
              : MetacardImpl.BASIC_METACARD;
      for (AttributeDescriptor descriptor : metacardType.getAttributeDescriptors()) {
        if (descriptor != null) {
          this.setAttribute(metacard.getAttribute(descriptor.getName()));
        }
      }
    }
    delegate = new MetacardImpl(this);
  }

  public AdaptedMetacard() {
    this(null);
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.data.MetacardImpl#setId(java.lang.String)
   */
  @Override
  @XmlAttribute(name = "id", namespace = "http://www.opengis.net/gml")
  @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
  @XmlID
  @XmlSchemaType(name = "ID")
  public String getId() {
    return delegate.getId();
  }

  /** @param id the id to set */
  protected void setId(String id) {
    setAttribute(new AttributeImpl(Core.ID, id));
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.data.MetacardImpl#setSourceId(java.lang.String)
   */
  @Override
  @XmlElement(name = "source", namespace = METACARD_URI)
  public String getSourceId() {
    return this.sourceId;
  }

  /** @param sourceId the sourceId to set */
  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.data.MetacardImpl#setType(ddf.catalog.data.MetacardType)
   */
  @Override
  @XmlElement(name = "type", namespace = METACARD_URI, required = true)
  public MetacardType getMetacardType() {
    return this.metacardType;
  }

  /** @param metacardType the metacardType to set */
  protected void setMetacardType(MetacardType metacardType) {
    this.metacardType = metacardType;
  }

  @XmlElements({
    @XmlElement(name = "boolean", namespace = METACARD_URI, type = BooleanElement.class),
    @XmlElement(name = "base64Binary", namespace = METACARD_URI, type = Base64BinaryElement.class),
    @XmlElement(name = "dateTime", namespace = METACARD_URI, type = DateTimeElement.class),
    @XmlElement(name = "double", namespace = METACARD_URI, type = DoubleElement.class),
    @XmlElement(name = "float", namespace = METACARD_URI, type = FloatElement.class),
    @XmlElement(name = "geometry", namespace = METACARD_URI, type = GeometryElement.class),
    @XmlElement(name = "int", namespace = METACARD_URI, type = IntElement.class),
    @XmlElement(name = "long", namespace = METACARD_URI, type = LongElement.class),
    @XmlElement(name = "object", namespace = METACARD_URI, type = ObjectElement.class),
    @XmlElement(name = "short", namespace = METACARD_URI, type = ShortElement.class),
    @XmlElement(name = "string", namespace = METACARD_URI, type = StringElement.class),
    @XmlElement(name = "stringxml", namespace = METACARD_URI, type = StringxmlElement.class)
  })
  protected Set<Attribute> getAttributes() {
    return attributes;
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.data.MetacardImpl#getAttribute(java.lang.String)
   */
  @Override
  public final Attribute getAttribute(String name) {
    if (StringUtils.isEmpty(name)) {
      return null;
    }
    if (Core.ID.equals(name)) {
      return this.idAttribute;
    }
    return attributes
        .stream()
        .filter(Objects::nonNull)
        .filter(attr -> name.equals(attr.getName()))
        .findFirst()
        .orElse(null);
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.data.MetacardImpl#setAttribute(ddf.catalog.data.Attribute)
   */
  @Override
  public final void setAttribute(@Nullable Attribute attribute) {
    if (attribute != null) {
      if (Core.ID.equals(attribute.getName())) {
        this.idAttribute = attribute;
      } else {
        Attribute currentAttribute = getAttribute(attribute.getName());
        if (currentAttribute != null) {
          attributes.remove(currentAttribute);
        }
        attributes.add(attribute);
      }
    }
  }

  /**
   * @return
   * @see ddf.catalog.data.Metacard#getContentTypeName()
   */
  public String getContentTypeName() {
    return delegate.getContentTypeName();
  }

  /**
   * @return
   * @see ddf.catalog.data.Metacard#getContentTypeNamespace()
   */
  public URI getContentTypeNamespace() {
    return delegate.getContentTypeNamespace();
  }

  /**
   * @return
   * @see ddf.catalog.data.Metacard#getContentTypeVersion()
   */
  public String getContentTypeVersion() {
    return delegate.getContentTypeVersion();
  }

  /**
   * @return
   * @see ddf.catalog.data.Metacard#getCreatedDate()
   */
  public Date getCreatedDate() {
    return (Date) delegate.getAttribute(Core.CREATED).getValue();
  }

  /**
   * @return
   * @see ddf.catalog.data.Metacard#getEffectiveDate()
   */
  public Date getEffectiveDate() {
    return delegate.getEffectiveDate();
  }

  /**
   * @return
   * @see ddf.catalog.data.Metacard#getExpirationDate()
   */
  public Date getExpirationDate() {
    return delegate.getExpirationDate();
  }

  /**
   * @return
   * @see ddf.catalog.data.Metacard#getLocation()
   */
  public String getLocation() {
    return delegate.getLocation();
  }

  /**
   * @return
   * @see ddf.catalog.data.Metacard#getMetadata()
   */
  public String getMetadata() {
    return delegate.getMetadata();
  }

  /**
   * @return
   * @see ddf.catalog.data.Metacard#getModifiedDate()
   */
  public Date getModifiedDate() {
    return (Date) delegate.getAttribute(Core.MODIFIED).getValue();
  }

  /**
   * @return
   * @see ddf.catalog.data.Metacard#getResourceSize()
   */
  public String getResourceSize() {
    return delegate.getResourceSize();
  }

  /**
   * @return
   * @see ddf.catalog.data.Metacard#getResourceURI()
   */
  public URI getResourceURI() {
    return delegate.getResourceURI();
  }

  /**
   * @return
   * @see ddf.catalog.data.Metacard#getThumbnail()
   */
  public byte[] getThumbnail() {
    return delegate.getThumbnail();
  }

  /**
   * @return
   * @see ddf.catalog.data.Metacard#getTitle()
   */
  public String getTitle() {
    return delegate.getTitle();
  }
}
