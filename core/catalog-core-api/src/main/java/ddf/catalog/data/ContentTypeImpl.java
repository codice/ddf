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

import java.net.URI;

/**
 * This class is a common implementation of the {@link ContentType} interface.
 * 
 * It is used to return the list of {@link ContentType}s currently stored in a {@link Source}.
 * 
 * @author ddf.isgs@lmco.com
 *
 */
public class ContentTypeImpl implements ContentType {

	private static final int HASHCODE_OFFSET = 17;

	/**
	 * This value was chosen because it is an odd prime. It can be also replaced
	 * by a shift and a subtraction for better performance, which most VMs do
	 * these days. (For more information, read <i>Item 9</i> in <u>Effective
	 * Java</u>, Second Edition)
	 */
	private static final int HASHCODE_MULTIPLIER = 31;

	/**
     * Name of the content type.
     */
    protected String name;
    
    /**
     * Version of the content type.
     */
    protected String version;
    
    /**
     * Namespace of the content type.
     */
    protected URI namespace;
    
    
    public ContentTypeImpl(){}
    
    /**
     * @param name the name of the {@link ContentType}
     * @param version the version of the {@link ContentType}
     */
    public ContentTypeImpl( String name, String version ){
        this( name, version, null );
    }
    
    /**
     * @param name the name of the {@link ContentType}
     * @param version the version of the {@link ContentType}
     * @param targetNamespace the namespace of the {@link ContentType}
     */
    public ContentTypeImpl( String name, String version, URI targetNamespace ){
        this.name = name;
        this.version = version;
        this.namespace = targetNamespace;
    }
    
    @Override
    public String getName(){
        return name;
    }
    
    /**
     * Sets the name of the content type.
     * 
     * @param name the name of the content type
     */
    public void setName( String name ){
        this.name = name;
    }
    
    @Override
    public String getVersion(){
        return version;
    }
    
    /**
     * Sets the version of the content type.
     * 
     * @param version the version of the content type
     */
    public void setVersion( String version ){
        this.version = version;
    }
    
    /**
     * Sets the namespace of the content type.
     * 
     * @param namespace the namespace of the content type
     */
    public void setNamespace( URI namespace ){
        this.namespace = namespace;
    }

	@Override
	public URI getNamespace() {
		  return namespace;
	}
	
	@Override
	public int hashCode() {

		/*
		 * Any major changes to this method, requires changes in the equals
		 * method.
		 */

		int result = HASHCODE_OFFSET;

		result = HASHCODE_MULTIPLIER * result + ((name != null) ? name.hashCode() : 0);

		result = HASHCODE_MULTIPLIER * result + ((version != null) ? version.hashCode() : 0);

		result = HASHCODE_MULTIPLIER * result + ((namespace != null) ? namespace.hashCode() : 0);

		return result;

	}

	@Override
	public boolean equals(Object obj) {

		/*
		 * Any major changes to this method such as adding a field check,
		 * requires changes in the hashCode method. According to the Java Object
		 * Specification, "If two objects are equal according to the
		 * equals(Object) method, then calling the hashCode method on each of
		 * the two objects must produce the same integer result."
		 */

		if (!(obj instanceof ContentType)) {
			return false;
		}

		ContentType newObject = (ContentType) obj;

		if (this.getName() == null) {
			if (newObject.getName() != null) {
				return false;
			}
		} else if(!this.getName().equals(newObject.getName())) {
			return false;
		}
		
		if(this.getVersion() == null) {
			if (newObject.getVersion() != null) {
				return false;
			}
		} else if (!this.getVersion().equals(newObject.getVersion())) {
			return false;
		}
		
		if(this.getNamespace() == null) {
			if (newObject.getNamespace() != null) {
				return false; 
			}
		} else if (!this.getNamespace().equals(newObject.getNamespace())) {
			return false;
		}

		return true;
	}
}
