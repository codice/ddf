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

package ddf.catalog.data.metacardtype;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

import ddf.catalog.data.MetacardTypeRegistry;
import ddf.catalog.data.QualifiedMetacardType;

public final class MetacardTypeRegistryImpl implements MetacardTypeRegistry {

    private BundleContext context;
    private List<QualifiedMetacardType> registeredMetacardTypes;
    private static Logger logger = Logger.getLogger(MetacardTypeRegistryImpl.class);

    private MetacardTypeRegistryImpl(BundleContext context, List<QualifiedMetacardType> registeredMetacardTypes) {
	this.context = context;
	this.registeredMetacardTypes = registeredMetacardTypes;
    }

    public static MetacardTypeRegistry getInstance(BundleContext context,
	    List<QualifiedMetacardType> registeredMetacardTypes) {
	return new MetacardTypeRegistryImpl(context, registeredMetacardTypes);
    }

    @Override
    public void register(QualifiedMetacardType qualifiedMetacardType) throws IllegalArgumentException{
	register(qualifiedMetacardType, null);
    }

    @Override
    public void register(QualifiedMetacardType qualifiedMetacardType, String sourceId)
	    throws IllegalArgumentException {
	if (qualifiedMetacardType == null || qualifiedMetacardType.getName() == null
		|| qualifiedMetacardType.getName().isEmpty()) {
	    throw new IllegalArgumentException("Neither the QualifiedMetacardType, nor its name can be null or empty.");
	}

	String mtName = qualifiedMetacardType.getName();
	String mtNamespace = qualifiedMetacardType.getNamespace();

	if (mtNamespace == null || mtNamespace.isEmpty()) {
	    mtNamespace = QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE;
	}

	Dictionary<String, String> properties = new Hashtable<String, String>();
	properties.put(METACARD_TYPE_NAMESPACE_KEY, mtNamespace);
	properties.put(METACARD_TYPE_NAME_KEY, mtName);

	if (sourceId != null && !sourceId.isEmpty()) {
	    properties.put(METACARD_TYPE_SOURCE_ID_KEY, sourceId);
	}

	context.registerService(QualifiedMetacardType.class.getName(), qualifiedMetacardType, properties);
    }

    @Override
    public QualifiedMetacardType lookup(String namespace, String metacardTypeName)
	    throws IllegalArgumentException {

	validateInput(namespace, metacardTypeName);

	for (QualifiedMetacardType qmt : registeredMetacardTypes) {
	    String currName = qmt.getName();
	    String currNamespace = qmt.getNamespace();
	    if (metacardTypeName.equals(currName) && namespace.equals(currNamespace)) {
		return qmt;
	    }
	}
	logger.debug("No registered MetacardType with namespace: " + namespace + " and name: " + metacardTypeName);
	return null;
    }

    @Override
    public QualifiedMetacardType lookup(String metacardTypeName) throws IllegalArgumentException {
	return lookup(QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE, metacardTypeName);
    }

//    @Override
//    public void unregister(String namespace, String metacardTypeName) throws IllegalArgumentException, MetacardTypeUnregistrationException {
//	validateInput(namespace, metacardTypeName);
//	try {
//	    context.getServiceReferences(QualifiedMetacardType.class.getName(), generateServiceFilter(namespace, metacardTypeName));
//	} catch (InvalidSyntaxException e) {
//	    String message = "Unable to unregister specified MetacardType.";
//	    logger.debug(message, e);
//	    throw new MetacardTypeUnregistrationException(message);
//	}
//	
//    }


//    @Override
//    public void unregister(String metacardTypeName) throws IllegalArgumentException {
//	// TODO Auto-generated method stub
//	
//    }

    private void validateInput(String namespace, String metacardTypeName) {
        if (metacardTypeName == null || metacardTypeName.isEmpty()) {
            String message = "MetacardTypeName parameter cannot be null or empty.";
            logger.debug(message);
            throw new IllegalArgumentException(message);
        }
        
        if(namespace == null){
            String message = "Namespace parameter cannot be null.";
            logger.debug(message);
            throw new IllegalArgumentException(message);
        }
    }
    
        private String generateServiceFilter(String namespace, String metacardTypeName) {
    	return "(&(" + METACARD_TYPE_NAME_KEY + "=" + metacardTypeName + ")(" + METACARD_TYPE_NAMESPACE_KEY + "=" + namespace + "))";
        }
}
