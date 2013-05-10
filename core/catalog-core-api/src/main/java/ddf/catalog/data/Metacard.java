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
import ddf.catalog.source.Source;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;

/**
 * A {@link Metacard} is a container for Metadata.
 * 
 * It is modeled after the OGC SimpleFeature and does two additional things:
 * <ol>
 * <li/>Represents a resource.
 * <li/>Contains or can be represented as XML metadata.
 * </ol>
 * This interface does not extend the OGC SimpleFeature interface due to added
 * complexity that was deemed to conflict with the goal of simplicity for this
 * API. <br/>
 * The {@link Metacard} can include key/value-style {@link Attribute}s, and
 * includes several standard properties: id, title, document, location, source
 * name, dates(created, modified, effective, expiration), and thumbnail.
 * 
 * @author ddf.isgs@lmco.com
 * 
 * @see BasicTypes
 * @see BasicTypes#BASIC_METACARD
 */
public interface Metacard extends Serializable {

	// Names of Queryable Properties
	/**
     * Attribute name for querying all text of all textual properties of a
     * {@link Metacard}.
     * <em>Used in Queries only</em>
	 */
	public static final String ANY_TEXT = "anyText";

	/**
     * Attribute name for querying all geometries of all geometric properties of
	 * a {@link Metacard}
     * <em>Used in Queries only</em>
	 */
	public static final String ANY_GEO = "anyGeo";

	/**
     * Attribute name for querying all dates of all temporal properties of a
	 * {@link Metacard}.
     * <em>Used in Queries only</em>
	 */
	public static final String ANY_DATE = "anyDate";
	
    /**
     * Attribute name for the ID of the source where the {@link Metacard} is cataloged.
     * @deprecated
     */
    public static final String SOURCE_ID = "source-id";

	/**
     * Attribute name for querying the metadata content type of a
     * {@link Metacard}.
	 * 
	 * @see MetacardType
	 */
    public static final String CONTENT_TYPE = "metadata-content-type";
    
    /**
     * Attribute name for querying the version of the metadata content type of
     * a {@link Metacard}.
     */
    public static final String CONTENT_TYPE_VERSION = "metadata-content-type-version";

    /**
     * Attribute name for querying the target namespace of the metadata content 
     * type of a {@link Metacard}.
     */
    public static final String TARGET_NAMESPACE = "metadata-target-namespace";

	// Stored Metacard properties
	/**
     * {@link Attribute} name for accessing the ID of the {@link Metacard}. <br/>
     * Every {@link Source} is required to return this attribute. 
	 */
	public static final String ID = "id";

	/**
     * {@link Attribute} name for accessing the title of the {@link Metacard}. <br/>
	 */
	public static final String TITLE = "title";

	/**
     * {@link Attribute} name for accessing the XML metadata for this
	 * {@link Metacard}. <br/>
	 */
	public static final String METADATA = "metadata";

	/**
     * {@link Attribute} name for accessing the location for this
	 * {@link Metacard}. <br/>
	 */
    public static final String GEOGRAPHY = "location";

	/**
     * {@link Attribute} name for accessing the date/time this {@link Metacard}
	 * was created. <br/>
	 */
	public static final String CREATED = "created";

	/**
     * {@link Attribute} name for accessing the date/time this {@link Metacard}
	 * was last modified. <br/>
	 */
	public static final String MODIFIED = "modified";

	/**
     * {@link Attribute} name for accessing the date/time the {@link Metacard}
     * is no longer valid and could be removed. <br/>
	 */
	public static final String EXPIRATION = "expiration";

	/**
     * {@link Attribute} name for accessing the date/time the {@link Metacard}
	 * was last known to be up-to-date. <br/>
	 */
	public static final String EFFECTIVE = "effective";

	/**
     * {@link Attribute} name for accessing the {@link URI} reference to the
	 * product this {@link Metacard} represents. <br/>
	 */
    public static final String RESOURCE_URI = "resource-uri";

	/**
     * {@link Attribute} name for accessing the size (in bytes) of the product
     * this {@link Metacard} represents. <br/>
     */
    public static final String RESOURCE_SIZE = "resource-size";

	/**
	 * {@link Attribute} name for accessing the thumbnail image of the product
	 * this {@link Metacard} represents. The thumbnail must be of MIME Type
	 * <code>image/jpeg</code> and 128 kilobytes or less. <br/>
	 */
	public static final String THUMBNAIL = "thumbnail";

    /**
     * {@link Attribute} name for accessing the security relevant markings on
     * the product that this {@link Metacard} represents.
     */
    public static final String SECURITY = "security";


    /**
     * Returns {@link Attribute} for given attribute name.
     * 
     * @param name	name of attribute
     * @return	{@link Attribute} for given name, or <code>null</code> if not available
     */
    public Attribute getAttribute(String name);
    
	/**
	 * Sets {@link Attribute} with new attribute.
	 * 
	 * @param attribute	new {@link Attribute} to set
	 */
	void setAttribute(Attribute attribute);

    /**
	 * Return the {@link MetacardType} of this {@link Metacard}.
	 * 
	 * @return {@link MetacardType} - the type of this {@link Metacard},
	 *         MetacardType is required and must not be <code>null</code>
	 */
    public MetacardType getMetacardType();
    
    

	// Shortcuts

	/**
     * Sets the source ID of the source the metacard is located. <br/>
	 * 
     * @param sourceId	unique name of source location of metacard
     */
    public void setSourceId(String sourceId);
    
	/**
     * 
     * Returns the ID of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute getAttribute}({@link Metacard#ID})
     * </code>
     * 
     * @return unique identifier of the Metacard
     * 
     * @see Metacard#ID
	 */
	public String getId();

	/**
     * Returns the metadata associated with this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute getAttribute}({@link Metacard#METADATA})
     * </code>
     * 
     * @return XML metadata
     * 
     * @see Metacard#METADATA
	 */
	public String getMetadata();

	/**
     * Returns the date/time this {@link Metacard} was created. <br/>
     * Convenience method for <code>
     * {@link #getAttribute getAttribute}({@link Metacard#CREATED})
     * </code>
	 * 
	 * @return {@link Date} - when this {@link Metacard} was created.
	 * 
	 * @see Metacard#CREATED
	 */
	public Date getCreatedDate();

	/**
     * Returns the date/time this {@link Metacard} was last modifed. <br/>
     * Convenience method for <code>
     * {@link #getAttribute getAttribute}({@link Metacard#MODIFIED})
     * </code>
	 * 
	 * @return {@link Date} - when this {@link Metacard} was last modified.
	 * 
	 * @see Metacard#MODIFIED
	 */
	public Date getModifiedDate();

	/**
     * Returns the date/time this {@link Metacard} is no longer valid and could be removed. <br/>
     * Convenience method for <code>
     * {@link #getAttribute getAttribute}({@link Metacard#EXPIRATION})
     * </code>
	 * 
	 * @return {@link Date} - when this {@link Metacard} expires and should be
	 *         removed from any stores.
	 * 
	 * @see Metacard#EXPIRATION
	 */
	public Date getExpirationDate();

	/**
     * Returns the date/time this {@link Metacard} was last known to be valid. <br/>
     * Convenience method for <code>
     * {@link #getAttribute getAttribute}({@link Metacard#EFFECTIVE})
     * </code>
	 * 
	 * @return {@link Date} - when the information represented by this
	 *         {@link Metacard} was last known to be valid.
	 * 
	 * @see Metacard#EFFECTIVE
	 */
	public Date getEffectiveDate();

	/**
     * Returns the WKT representation of the geometry. <br/>
     * Convenience method for <code>
     * {@link #getAttribute getAttribute}({@link Metacard#GEOGRAPHY})
     * </code>
	 * 
     * @return {@link String} - WKT-defined geospatial object, returns null if
     *         not applicable
     *         
     * @see Metacard#GEOGRAPHY
	 */
	public String getLocation();

    /**
     * Returns the source ID of the source the metacard is located. <br/>
	 *
     */
    public String getSourceId();

	/**
     * Returns the title of this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute getAttribute}({@link Metacard#TITLE})
     * </code>
     * 
     * @return Title of the {@link Metacard}
     * 
     * @see Metacard#TITLE
	 */
	public String getTitle();

    /**
     * Get the value of this {@link Metacard}s Resource URI and in the form of a {@link URI} Object. <br/>
     * Convenience method for <code>
     * new URI({@link #getAttribute getAttribute}({@link Metacard#RESOURCE_URI}));
     * </code>
     * 
     * @return {@link URI} - a {@link URI} representation of the {@link Metacard}'s {@link Metacard#RESOURCE_URI Resource URI} which itself is stored as a {@link AttributeFormat#STRING String AttributeFormat}
     */
    public URI getResourceURI();

    /**
     * This is the size of the resource which may or may not contain a unit.
     * 
     * @return {@link String} - {@link String} representation of the size
     */
    public String getResourceSize();

	/**
     * Returns the thumbnail associated with this {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute getAttribute}({@link Metacard#THUMBNAIL})
     * </code>
     * 
     * @return thumbnail for the {@link Metacard}
     * 
     * @see Metacard#THUMBNAIL
	 */
    public byte[] getThumbnail();
    
	/**
     * Returns the name of the content type of the {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute getAttribute}({@link Metacard#CONTENT_TYPE})
     * </code>
     * 
     * @return name of content type of the {@link Metacard}
     * 
     * @see Metacard#CONTENT_TYPE
     */
    public String getContentTypeName();
    
	/**
     * Returns the version of the content type of the {@link Metacard}. <br/>
     * Convenience method for <code>
     * {@link #getAttribute getAttribute}({@link Metacard#CONTENT_TYPE_VERSION})
     * </code>
     * 
     * @return version of content type of the {@link Metacard}
     * 
     * @see Metacard#CONTENT_TYPE_VERSION
     */
    public String getContentTypeVersion();
    
    /**
     * Some types of metadata use different content types. If utilized, returns
     * the {@link URI} of the content type. <br/>
     * Convenience method for <code>
     * {@link #getAttribute getAttribute}({@link Metacard#TARGET_NAMESPACE})
     * </code>
     * 
     * @return {@link URI} - the sub-type, null if unused
     * 
     * @see Metacard#TARGET_NAMESPACE
     */
    public URI getContentTypeNamespace();
}