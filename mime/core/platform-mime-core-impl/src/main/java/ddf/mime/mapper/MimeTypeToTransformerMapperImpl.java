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
package ddf.mime.mapper;

import ddf.mime.MimeTypeToTransformerMapper;
import org.apache.commons.lang.StringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link MimeTypeToTransformerMapper} Implementation that finds mimeType matches among transformer
 * services
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class MimeTypeToTransformerMapperImpl implements MimeTypeToTransformerMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MimeTypeToTransformerMapperImpl.class);

    public MimeTypeToTransformerMapperImpl() {

    }

    protected BundleContext getContext() {
        Bundle cxfBundle = FrameworkUtil.getBundle(MimeTypeToTransformerMapperImpl.class);
        if (cxfBundle != null) {
            return cxfBundle.getBundleContext();
        }
        return null;
    }

    @Override
    public <T> List<T> findMatches(Class<T> clazz, MimeType userMimeType) {
        BundleContext bundleContext = getContext();
        ServiceReference[] refs = null;
        List<T> list = new ArrayList<T>();

        if (bundleContext == null) {
            LOGGER.debug("Cannot find matches, bundle context is null.");
            return list;
        }
        if (clazz == null) {
            LOGGER.warn("Cannot find matches, service argument is null.");
            throw new IllegalArgumentException("Invalid argument supplied, null service argument");
        }

        /*
         * Extract the services using the bundle context.
         */
        try {
            refs = bundleContext.getServiceReferences(clazz.getName(), null);
        } catch (InvalidSyntaxException e) {
            LOGGER.warn("Invalid filter syntax ", e);
            throw new IllegalArgumentException("Invalid syntax supplied: "
                    + userMimeType.toString());
        }
        
        // If no InputTransformers found, return empty list
        if (refs == null) {
            LOGGER.debug("No {} services found - return empty list", clazz.getName());
            return list;
        }
        
        /*
         * Sort the list of service references based in it's Comparable interface.
         */
        Arrays.sort(refs, Collections.reverseOrder());

        /*
         * If the mime type is null return the whole list of service references
         */
        if (userMimeType == null) {
            if (refs.length > 0) {
                for (ServiceReference ref : refs) {
                    Object service = (bundleContext.getService(ref));
                    T typedService = clazz.cast(service);
                    list.add(typedService);
                }
            }
            return list;
        }

        String userIdValue = userMimeType.getParameter(MimeTypeToTransformerMapper.ID_KEY);
        List<T> strictlyMatching = new ArrayList<T>();

        for (ServiceReference ref : refs) {

            List<String> mimeTypesServicePropertyList = getServiceMimeTypesList(ref);

            String serviceId = getServiceId(ref);

            for (String mimeTypeRawEntry : mimeTypesServicePropertyList) {

                MimeType mimeTypeEntry = constructMimeType(mimeTypeRawEntry);

                if (mimeTypeEntry != null
                        && StringUtils.equals(mimeTypeEntry.getBaseType(),
                                userMimeType.getBaseType())
                        && (userIdValue == null || StringUtils.equals(userIdValue, serviceId))) {

                    try {
                        T service = clazz.cast(bundleContext.getService(ref));
                        strictlyMatching.add(service);
                        break; // found exact mimetype, no need to continue within
                        // the same service

                    } catch (ClassCastException cce) {
                        LOGGER.debug("Caught illegal cast to transformer type. ", cce);
                    }
                }
            }
        }

        return strictlyMatching;
    }

    private MimeType constructMimeType(String mimeTypeRawEntry) {

        try {
            return new MimeType(mimeTypeRawEntry);
        } catch (MimeTypeParseException e) {
            LOGGER.debug("MIME type parse exception constructing MIME type", e);
        }

        return null;
    }

    private List<String> getServiceMimeTypesList(ServiceReference ref) {

        Object mimeTypeServiceProperty = ref.getProperty(MIME_TYPE_KEY);

        if (mimeTypeServiceProperty != null) {

            if (mimeTypeServiceProperty instanceof String) {
                /*
                 * We cannot enforce how the property is given to us whether it is a list or a
                 * single property. This case catches the single mime-type property.
                 */
                return Arrays.asList(mimeTypeServiceProperty.toString());
            }

            return (List<String>) mimeTypeServiceProperty;
        }

        /*
         * An empty list is returned, if the call to getProperty has returned with a null value.
         */
        return new ArrayList<String>();
    }

    private String getServiceId(ServiceReference ref) {
        Object idServiceProperty = ref.getProperty(ID_KEY);

        if (idServiceProperty != null) {

            return idServiceProperty.toString();
        }

        return null;
    }

}
