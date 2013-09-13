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

import java.util.Set;

/**
 * Default implementation of the QualifiedMetacardType.
 * 
 * <p>
 * <b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 * 
 * @author Ian Barnett
 * @author ddf.isgs@lmco.com
 * 
 */
public class QualifiedMetacardTypeImpl extends MetacardTypeImpl implements QualifiedMetacardType {

    private static final long serialVersionUID = -5596051498437529825L;

    private String namespace;

    public QualifiedMetacardTypeImpl(String namespace, String name,
            Set<AttributeDescriptor> descriptors) {
        super(name, descriptors);

        this.namespace = namespace;

        if (namespace == null || namespace.isEmpty()) {
            this.namespace = QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE;
        }
    }

    public QualifiedMetacardTypeImpl(MetacardType mt) {
        this(null, mt.getName(), mt.getAttributeDescriptors());
    }

    public QualifiedMetacardTypeImpl(String namespace, MetacardType mt) {
        this(namespace, mt.getName(), mt.getAttributeDescriptors());
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        if (!(obj instanceof MetacardType)) {
            return false;
        }

        if (obj instanceof QualifiedMetacardType) {
            QualifiedMetacardTypeImpl other = (QualifiedMetacardTypeImpl) obj;

            if (namespace == null) {
                if (other.namespace != null) {
                    return false;
                }
            } else if (!namespace.equals(other.namespace)) {
                return false;
            }

        }

        return true;
    }

}
