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
package ddf.mime.custom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.mime.MimeTypeResolver;

/**
 * DDF custom mime type resolution packaged as a {@link MimeTypeResolver} OSGi service that can map
 * a list of custom file extensions to their corresponding custom mime types, and vice versa.
 * Currently used to add image/nitf mime type support.
 * 
 * @since 2.1.0
 * 
 */
public class CustomMimeTypeResolver implements MimeTypeResolver {
    private static XLogger logger = new XLogger(
            LoggerFactory.getLogger(CustomMimeTypeResolver.class));

    private String name;

    private int priority;

    private String[] customMimeTypes;

    private HashMap<String, String> customFileExtensionsToMimeTypesMap;

    private HashMap<String, List<String>> customMimeTypesToFileExtensionsMap;

    public CustomMimeTypeResolver() {
        this.customFileExtensionsToMimeTypesMap = new HashMap<String, String>();
        this.customMimeTypesToFileExtensionsMap = new HashMap<String, List<String>>();
    }

    public void init() {
        logger.trace("INSIDE: init");
    }

    public void destroy() {
        logger.trace("INSIDE: destroy");
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        logger.debug("Setting priority = {}", priority);
        this.priority = priority;
    }

    public String[] getCustomMimeTypes() {
        return customMimeTypes.clone();
    }

    public void setCustomMimeTypes(String[] customMimeTypes) {
        logger.trace("ENTERING: setCustomMimeTypes");

        this.customMimeTypes = customMimeTypes.clone();
        this.customFileExtensionsToMimeTypesMap = new HashMap<String, String>();
        this.customMimeTypesToFileExtensionsMap = new HashMap<String, List<String>>();

        for (String mimeTypeMapping : this.customMimeTypes) {
            logger.trace(mimeTypeMapping);

            // mimeTypeMapping is of the form <file extension>=<mime type>
            // Examples:
            // nitf=image/nitf

            String fileExtension = StringUtils.substringBefore(mimeTypeMapping, "=");
            String mimeType = StringUtils.substringAfter(mimeTypeMapping, "=");

            customFileExtensionsToMimeTypesMap.put(fileExtension, mimeType);
            List<String> fileExtensions = (List<String>) customMimeTypesToFileExtensionsMap
                    .get(mimeType);
            if (fileExtensions == null) {
                logger.debug("Creating fileExtensions array for mime type: {}", mimeType);
                fileExtensions = new ArrayList<String>();
            }
            logger.debug("Adding file extension: {} for mime type: {}", fileExtensions, mimeType);
            fileExtensions.add(fileExtension);
            customMimeTypesToFileExtensionsMap.put(mimeType, fileExtensions);
        }

        logger.debug("customFileExtensionsToMimeTypesMap = {} ", customFileExtensionsToMimeTypesMap);
        logger.debug("customMimeTypesToFileExtensionsMap = {}", customMimeTypesToFileExtensionsMap);

        logger.trace("EXITING: setCustomMimeTypes");
    }

    public HashMap<String, String> getCustomFileExtensionsToMimeTypesMap() {
        return customFileExtensionsToMimeTypesMap;
    }

    public void setCustomFileExtensionsToMimeTypesMap(
            HashMap<String, String> customFileExtensionsToMimeTypesMap) {
        this.customFileExtensionsToMimeTypesMap = customFileExtensionsToMimeTypesMap;
    }

    public HashMap<String, List<String>> getCustomMimeTypesToFileExtensionsMap() {
        return customMimeTypesToFileExtensionsMap;
    }

    public void setCustomMimeTypesToFileExtensionsMap(
            HashMap<String, List<String>> customMimeTypesToFileExtensionsMap) {
        this.customMimeTypesToFileExtensionsMap = customMimeTypesToFileExtensionsMap;
    }

    @Override
    public String getFileExtensionForMimeType(String mimeType) // throws MimeTypeException
    {
        logger.trace("ENTERING: getFileExtensionForMimeType");
        logger.debug("contentType = {}", mimeType);

        String fileExtension = null;
        if (mimeType != null && !mimeType.isEmpty()) {
            List<String> fileExtensions = customMimeTypesToFileExtensionsMap.get(mimeType);
            if (fileExtensions != null && fileExtensions.size() > 0) {
                logger.debug("{} file extensions found for mime type = {} ",
                        fileExtensions.size(), mimeType);

                fileExtension = fileExtensions.get(0);

                // Prepend "." to file extension if it is not already there.
                // This conforms to how Apache Tika returns a file extension for any given mime type
                // and allows client to just append the file extension to the file name it is
                // creating.
                if (fileExtension != null && !fileExtension.startsWith(".")) {
                    fileExtension = "." + fileExtension;
                }
            }
        }

        logger.debug("fileExtension = {}", fileExtension);

        logger.trace("EXITING: getFileExtensionForMimeType");

        return fileExtension;
    }

    @Override
    public String getMimeTypeForFileExtension(String fileExtension) // throws MimeTypeException
    {
        logger.trace("ENTERING: getMimeTypeForFileExtension");
        logger.debug("fileExtension = {}", fileExtension);

        String mimeType = null;
        if (fileExtension != null && !fileExtension.isEmpty()) {
            mimeType = customFileExtensionsToMimeTypesMap.get(fileExtension);
        }

        logger.debug("mimeType = {}", mimeType);

        logger.trace("EXITING: getMimeTypeForFileExtension");

        return mimeType;
    }

    public static <K, V> HashMap<V, K> reverse(Map<K, V> map) {
        HashMap<V, K> rev = new HashMap<V, K>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            rev.put(entry.getValue(), entry.getKey());
        }
        return rev;
    }

}
